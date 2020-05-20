package com.github.opengrabeso.cohabo

object Highlight {
  def decorate(word: String): String = "<span class='search-highlight'>" + word + "</span>"

  def single(html: String, word: String): String = {
    @scala.annotation.tailrec
    def recurse(s: String, done: Int): String = {
      // skip any html tags
      val start = s.indexOf(word, done)
      if (start < 0) s
      else {
        val nextTag = s.indexOf('<', done)
        if (nextTag < 0 || start < nextTag) {
          val decorated = decorate(word)
          recurse(s.patch(start, decorated, word.length), start + decorated.length)
        } else {
          val tagEnd = s.indexOf('>', nextTag)
          if (tagEnd < 0) s
          else {
            recurse(s, tagEnd)
          }
        }
      }

    }
    recurse(html, 0)
  }

  def apply(html: String, highlightWords: Seq[String]): String = {
    highlightWords.foldLeft(html)((s, w) => single(s, w))
  }
}
