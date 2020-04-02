package com.github.opengrabeso.cohabo
package frontend.views

object ShortIds {
  def compute(input: Seq[Seq[String]]): Seq[String] = {

    case class Initials(data: IndexedSeq[(String, Int)]) {
      def advance: Initials = {
        // always advance the smaller one if possible, reverse ti prefer advancing the last one
        val minIndex = data.zipWithIndex.reverse.minBy {
          case ((name, pos), _) =>
            if (pos >= name.length) Int.MaxValue // nowhere to advance to
            else pos
        }._2
        val current = data(minIndex)
        Initials(data.patch(minIndex, Seq(current.copy(_2 = current._2 + 1)), 1))
      }
      def result: String = data.map { case (str, pos) =>
        str(pos).toUpper
      }.mkString
    }

    object Initials {
      def from(in: Seq[String]): Initials = {
        Initials(in.map(_ -> 0).toIndexedSeq)
      }
    }

    @scala.annotation.tailrec
    def recurse(todo: List[Seq[String]], done: List[String]): List[String] = {
      todo match {
        case head :: tail =>

          @scala.annotation.tailrec
          def tryShort(i: Initials): String = {
            val res = i.result
            if (!done.contains(res)) res
            else {
              val next = i.advance
              if (next == i) {
                // nowhere to advance to, fallback to plain initials
                Initials.from(head).result
              } else {
                tryShort(next)
              }
            }
          }

          val short = tryShort(Initials.from(head))
          recurse(tail, short :: done)
        case _ =>
          done
      }
    }

    recurse(input.toList, Nil).reverse
  }

}
