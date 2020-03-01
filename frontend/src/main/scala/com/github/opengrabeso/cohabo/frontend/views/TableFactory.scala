package com.github.opengrabeso.cohabo
package frontend.views

import common.css._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import org.scalajs.dom.{Element, Event, Node}
import io.udash.css.CssView._
import io.udash.properties.ModelPropertyCreator
import io.udash.wrappers.jquery.jQ
import scalatags.JsDom.all._

object TableFactory {
  val s = SelectPageStyles

  case class TableAttrib[ItemType](
    name: String, value: (ItemType, ModelProperty[ItemType], NestedInterceptor) => Modifier,
    style: Option[String] = None,
    shortName: Option[String] = None
  )

  def headerFactory[ItemType](attribs: Seq[TableAttrib[ItemType]]): NestedInterceptor => Modifier = _ => tr {
    attribs.flatMap { a =>
      val st = a.style.map(style := _)
      a.shortName.map { shortName =>
        val wide = th(s.wideMedia, b(a.name), st).render
        if (shortName.isEmpty) {
          Seq(wide)
        } else {
          val narrow = th(st, s.narrowMedia, b(a.shortName)).render
          Seq(wide, narrow)
        }
      }.getOrElse(Seq(th(st, b(a.name)).render))
    }
  }.render

  def rowFactory[ItemType: ModelPropertyCreator, SelType](id: ItemType => SelType, sel: Property[Option[SelType]], attribs: Seq[TableAttrib[ItemType]]): (CastableProperty[ItemType], NestedInterceptor) => Element = { (el,_) =>
    tr(
      s.tr,
      produceWithNested(el) { (ha, nested) =>
        attribs.flatMap { a =>
          // existing but empty shortName means the column should be hidden on narrow view
          if (a.shortName.contains("")) {
            td(s.td, s.wideMedia, a.value(ha, el.asModel, nested)).render
          } else {
            td(s.td, a.value(ha, el.asModel, nested)).render
          }
        }
      },
      onclick :+= { e: Event =>
        val td = e.target.asInstanceOf[Element]
        // e.target may be a td inside of tr, we need to find a tr parent in such case

        val tr = jQ(td).closest("tr")
        val wasSelected = tr.hasClass("selected")
        if (!wasSelected) {
          val selId = id(el.get)
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

        false
      }
    ).render
  }
}
