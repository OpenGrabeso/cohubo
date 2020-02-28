package com.github.opengrabeso.cohabo
package frontend.views

import common.css._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import org.scalajs.dom.{Element, Node}
import org.scalajs.dom.Node
import io.udash.css.CssView._
import io.udash.properties.ModelPropertyCreator
import scalatags.JsDom.all._

object TableFactory {
  val s = SelectPageStyles

  case class TableAttrib[ItemType](name: String, value: (ItemType, ModelProperty[ItemType], NestedInterceptor) => Seq[Node], shortName: Option[String] = None)

  def headerFactory[ItemType](attribs: Seq[TableAttrib[ItemType]]): NestedInterceptor => Modifier = _ => tr {
    attribs.flatMap { a =>
      a.shortName.map { shortName =>
        val wide = td(s.wideMedia, b(a.name)).render
        if (shortName.isEmpty) {
          Seq(wide)
        } else {
          val narrow = td(s.narrowMedia, b(a.shortName)).render
          Seq(wide, narrow)
        }
      }.getOrElse(Seq(th(
        if (a.name != "") b(a.name).render
        else Seq[Modifier](
          attr("data-field"):="state",
          attr("data-checkbox"):="true"
        )
      ).render))
    }
  }.render

  def rowFactory[ItemType: ModelPropertyCreator](attribs: Seq[TableAttrib[ItemType]], compact: Boolean): (CastableProperty[ItemType], NestedInterceptor) => Element = (el,_) => tr(
    if (compact) Seq[Modifier](s.tableCompact) else Seq.empty[Modifier],
    produceWithNested(el) { (ha, nested) =>
      attribs.flatMap { a =>
        // existing but empty shortName means the column should be hidden on narrow view
        val aValue = a.value(ha, el.asModel, nested)
        if (a.shortName.contains("")) {
          td(s.wideMedia, aValue).render
        } else {
          td(aValue).render
        }
      }
    }
  ).render
}
