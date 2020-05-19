package com.github.opengrabeso.cohabo
package frontend.views

object Highlight {
  def decorate(word: String): String = "<span class='highlight'>" + word + "</span>"

  def single(html: String, word: String): String = {
    @scala.annotation.tailrec
    def recurse(s: String, done: Int): String = {
      val start = s.indexOf(word, done)
      if (start < 0) s
      else {
        val decorated = decorate(word)
        recurse(s.patch(start, decorated, word.length), start + decorated.length)
      }

    }
    recurse(html, 0)
  }

  def apply(html: String, highlightWords: Set[String]): String = {
    highlightWords.foldLeft(html)((s, w) => single(s, w))
  }
}
