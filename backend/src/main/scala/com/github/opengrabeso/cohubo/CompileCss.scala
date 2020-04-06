package com.github.opengrabeso.cohubo

import java.io.FileOutputStream

import org.apache.commons.io.IOUtils
import com.github.opengrabeso.cohabo.common.css._
import io.udash.css.CssStringRenderer
import scalacss.internal.{Renderer, StringRenderer}

object CompileCss {
  def main(args: Array[String]): Unit = {

    require(args.length == 2, " Expected two arguments: target path and pretty print flag")

    val renderPretty = java.lang.Boolean.parseBoolean(args(1))
    val path = args(0)

    val styles = Seq(
      GlobalStyles,
      SelectPageStyles,
      NameColorStyles,
      SettingsPageStyles,
      EditPageStyles
    )

    implicit val renderer: Renderer[String] = if (renderPretty) StringRenderer.defaultPretty else StringRenderer.formatTiny

    val cssString = new CssStringRenderer(styles).render()

    val out = new FileOutputStream(path + "/main.css")
    try {

      IOUtils.write(cssString, out)
      IOUtils.write("\n", out) // prevent empty file by always adding an empty line, empty file not handled well by Spark framework
    } finally {
      out.close()
    }


  }
}