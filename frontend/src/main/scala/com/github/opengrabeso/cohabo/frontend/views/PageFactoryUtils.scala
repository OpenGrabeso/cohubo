package com.github.opengrabeso.cohabo.frontend.views

import io.udash.properties.ValidationResult
import io.udash.{DefaultValidationError, Invalid, Valid, Validator}

import scala.concurrent.Future

trait PageFactoryUtils {
  class NumericRangeValidator(from: Int, to: Int) extends Validator[Int] {
    def apply(value: Int): Future[ValidationResult] = Future.successful{
      if (value >= from && value <= to) Valid
      else Invalid(DefaultValidationError(s"Expected value between $from and $to"))
    }
  }


}
