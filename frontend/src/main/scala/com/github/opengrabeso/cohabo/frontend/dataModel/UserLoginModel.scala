package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.HasModelPropertyCreator

case class UserLoginModel(login: String = null, fullName: String = null)

object UserLoginModel extends HasModelPropertyCreator[UserLoginModel]
