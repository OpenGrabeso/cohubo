package com.github.opengrabeso.cohabo
package common.model

import com.avsystem.commons.rpc.AsRawReal
import io.udash.rest.raw.{HttpBody, IMapping, PlainValue, RestResponse}

case class TextData(data: String)

// text encoding, needed for some parameters which require plain text (GitHub /markdown/raw)

object TextData extends rest.EnhancedRestDataCompanion[TextData] {
  implicit val rawReal: AsRawReal[RestResponse, TextData] = AsRawReal.create(
    real => RestResponse(200, IMapping[PlainValue](), HttpBody.textual(real.data)),
    raw => TextData(raw.body.readText())
  )

}
