package com.github.opengrabeso.cohabo
package frontend

import io.udash.ReadableProperty

package object views {
  implicit class TupleCombine[T](a: ReadableProperty[T]) {
    def tuple[X](b: ReadableProperty[X]): ReadableProperty[(T, X)] = a.combine(b)(_ -> _)
    @inline def **[X](b: ReadableProperty[X]): ReadableProperty[(T, X)] = a.tuple(b)
  }
  implicit class BooleanCombine(a: ReadableProperty[Boolean]) {
    def || (b: ReadableProperty[Boolean]): ReadableProperty[Boolean] = a.combine(b)(_ || _)
    def && (b: ReadableProperty[Boolean]): ReadableProperty[Boolean] = a.combine(b)(_ && _)
  }


}
