package com.github.opengrabeso.cohabo
package rest

import java.time.ZonedDateTime

import com.avsystem.commons.meta.MacroInstances
import com.avsystem.commons.serialization.{GenCodec, GenKeyCodec, HasGenCodecWithDeps, Input, Output}
import io.udash.rest._
import io.udash.rest.openapi.{RestSchema, RestStructure}

trait EnhancedRestImplicits extends DefaultRestImplicits {

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

object EnhancedRestImplicits extends EnhancedRestImplicits {
  object ZonedDateTimeAU {
    def apply(string: String): ZonedDateTime = ZonedDateTime.parse(string)
    def unapply(dateTime: ZonedDateTime): Option[String] = Some(dateTime.toString)
  }
}

abstract class EnhancedRestDataCompanion[T](
  implicit macroCodec: MacroInstances[EnhancedRestImplicits.type, () => GenCodec[T]]
) extends HasGenCodecWithDeps[EnhancedRestImplicits.type, T] {
  implicit val instances: MacroInstances[DefaultRestImplicits, CodecWithStructure[T]] = implicitly[MacroInstances[DefaultRestImplicits, CodecWithStructure[T]]]
  implicit lazy val restStructure: RestStructure[T] = instances(DefaultRestImplicits, this).structure
  implicit lazy val restSchema: RestSchema[T] = RestSchema.lazySchema(restStructure.standaloneSchema)
}


