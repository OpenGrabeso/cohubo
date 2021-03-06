package com.github.opengrabeso.cohabo.common

object ShortIds {

  case class Initials(data: IndexedSeq[(String, Int)]) {
    def advance: Initials = {
      // always advance the smaller one if possible, reverse ti prefer advancing the last one
      if (data.nonEmpty) {
        val minIndex = data.zipWithIndex.reverse.minBy {
          case ((name, pos), _) =>
            if (pos >= name.length - 1) Int.MaxValue // nowhere to advance to
            else pos
        }._2
        val current = data(minIndex)
        val newPos = current._2 + 1 min current._1.length - 1
        Initials(data.patch(minIndex, Seq(current.copy(_2 = newPos)), 1))
      } else this
    }
    def result: String = data.map { case (str, pos) =>
      if (str.isEmpty) str
      else str(pos).toUpper
    }.mkString
  }

  object Initials {
    def from(in: Seq[String]): Initials = {
      Initials(in.map(_ -> 0).toIndexedSeq)
    }
  }

  def compute(input: Seq[Seq[String]]): Seq[String] = {
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
