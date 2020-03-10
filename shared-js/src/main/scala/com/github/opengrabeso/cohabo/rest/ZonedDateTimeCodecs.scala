package com.github.opengrabeso.cohabo.rest

import java.time.ZonedDateTime

import com.avsystem.commons.serialization.{GenCodec, GenKeyCodec, Input, Output}

trait ZonedDateTimeCodecs {

  implicit val zonedDateTimeCodec: GenCodec[ZonedDateTime] = new GenCodec[ZonedDateTime] {
    override def read(input: Input) = {
      val str = input.readSimple().readString()
      ZonedDateTime.parse(str)
    }
    override def write(output: Output, value: ZonedDateTime) = {
      val str = value.toString
      output.writeSimple().writeString(str)
    }
  }
  implicit val zonedDateTimeKeyCodec: GenKeyCodec[ZonedDateTime] = GenKeyCodec.create(ZonedDateTime.parse,_.toString)
}
