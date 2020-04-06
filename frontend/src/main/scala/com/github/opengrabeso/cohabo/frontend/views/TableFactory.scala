package com.github.opengrabeso.cohabo
package frontend.views

import common.css._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import org.scalajs.dom.{Element, Event, Node}
import io.udash.css.CssView._
import io.udash.properties.ModelPropertyCreator
import io.udash.wrappers.jquery.{JQuery, jQ}
import scalatags.JsDom.all._

object TableFactory {
  val s = SelectPageStyles

  case class TableAttrib[ItemType](
    name: String, value: (ItemType, ModelProperty[ItemType], NestedInterceptor) => Modifier,
    style: Option[String] = None,
    modifier: Option[ItemType => Modifier] = None,
    shortName: Option[String] = None
  )

  trait TableRowHandler[ItemType, SelType] {
    def id(item: ItemType): SelType
    def indent(item: ItemType): Int
    def rowModifier(itemModel: ModelProperty[ItemType]): Modifier
    def tdModifier: Modifier
  }

  def headerFactory[ItemType](attribs: Seq[TableAttrib[ItemType]]): NestedInterceptor => Modifier = _ => tr {
    attribs.flatMap { a =>
      val st = a.style.map(style := _)
      a.shortName.map { shortName =>
        val wide = th(s.th, s.wideMedia, div(a.name), st).render
        if (shortName.isEmpty) {
          Seq(wide)
        } else {
          val narrow = th(s.th, st, s.narrowMedia, div(a.shortName)).render
          Seq(wide, narrow)
        }
      }.getOrElse(Seq(th(s.th, st, div(a.name)).render))
    }
  }.render

  def rowFactory[ItemType: ModelPropertyCreator, SelType](
    selDisabled: ReadableProperty[Boolean],
    sel: Property[Option[SelType]],
    attribs: Seq[TableAttrib[ItemType]]
  )(implicit rowHandler: TableRowHandler[ItemType, SelType]): (CastableProperty[ItemType], NestedInterceptor) => Element = { (el,_) =>
    val timing = false
    val start = System.currentTimeMillis()
    val elData = el.get
    val level = rowHandler.indent(elData)
    val row = tr(
      rowHandler.rowModifier(el.asModel),
      produceWithNested(el) { (ha, nested) =>
        attribs.flatMap { a =>
          // existing but empty shortName means the column should be hidden on narrow view
          val tdItem = td(rowHandler.tdModifier, a.modifier.map(_ (ha)), a.value(ha, el.asModel, nested))
          if (a.shortName.contains("")) {
            tdItem(s.wideMedia).render
          } else {
            tdItem.render
          }
        }
      },

      if (level > 0) Seq[Modifier](style := "display: none") else Seq.empty[Modifier],
      attr("data-depth") := level,

      onclick :+= { e: Event =>
        if (!selDisabled.get) {
          val td = e.target.asInstanceOf[Element]
          // e.target may be a td inside of tr, we need to find a tr parent in such case
          val tr = jQ(td).closest("tr")
          val wasSelected = tr.hasClass("selected")
          if (!wasSelected) {
            val selId = rowHandler.id(elData)
            tr.addClass("selected")
            sel.set(Some(selId))

            // once the selection is changed from us, unselect us
            def listenCallback(newId: Option[SelType]): Unit = {
              if (newId.contains(selId)) sel.listenOnce(listenCallback) // listen again
              else {
                tr.removeClass("selected")
              }
            }
            sel.listenOnce(listenCallback)
          }
        }

        false
      }
    ).render
    jQ(row).find(".fold-control").on("click", { (control, event) =>
      // from https://stackoverflow.com/a/49364929/16673
      //println(tr.attr("data-depth"))
      val tr = jQ(control).closest("tr")

      def getDepth(d: Option[Any]) = d.map(_.asInstanceOf[Int]).getOrElse(0)
      // find all children (following items with greater level)
      def findChildren(tr: JQuery) = {
        def getDepth(d: Option[Any]) = d.map(_.asInstanceOf[Int]).getOrElse(0)
        val depth = getDepth(tr.data("depth"))
        tr.nextUntil(jQ("tr").filter((x: Element, _: Int, _: Element) => {
          getDepth(jQ(x).data("depth")) <= depth
        }))
      }

      val children = findChildren(tr)
      //println(children.length)
      val arrow = tr.find(".fold-control")
      if (children.is(":visible")) {
        arrow.html("\u02c3") // >
        tr.find(".fold-control").removeClass("fold-open")
        children.hide()
      } else {
        arrow.html("\u02c5") // v
        tr.find(".fold-control").addClass("fold-open")
        tr.removeClass("closed")
        children.show()

        val childrenClosed = children.filter((e: Element, _: Int, _: Element) => jQ(e).find(".fold-open").length == 0)

        childrenClosed.get.foreach { close =>
          val hide = findChildren(jQ(close))
          hide.hide()
        }
      }

    })

    if (timing) println(s"Row $elData took ${System.currentTimeMillis()-start}")
    row
  }
}
