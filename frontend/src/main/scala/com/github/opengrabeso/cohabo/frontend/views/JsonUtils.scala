package com.github.opengrabeso.cohabo.frontend.views

import com.avsystem.commons.serialization.GenCodec
import com.avsystem.commons.serialization.json.JsonStringInput
import com.avsystem.commons.serialization.json.JsonReader

object JsonUtils {

  def read[T: GenCodec](json: String) = {
    val codec = implicitly[GenCodec[T]]
    val reader = new JsonReader(json)
    codec.read(new JsonStringInput(reader))
  }

}
