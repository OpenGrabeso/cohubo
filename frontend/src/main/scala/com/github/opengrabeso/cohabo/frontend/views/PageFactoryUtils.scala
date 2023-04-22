package com.github.opengrabeso.cohabo.frontend.views

import io.udash.bootstrap.form._

import scala.concurrent.Future

trait PageFactoryUtils {
  class NumericRangeValidator(from: Int, to: Int) extends Validator[Int] {
    def apply(value: Int): Validation = {
      if (value >= from && value <= to) Valid
      else Invalid(DefaultValidationError(s"Expected value between $from and $to"))
    }
  }
}
