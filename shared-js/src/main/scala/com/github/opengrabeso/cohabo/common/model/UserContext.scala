package com.github.opengrabeso.cohabo.common.model

import io.udash.rest.RestDataCompanion

case class UserContext(userId: String, authCode: String)
object UserContext extends RestDataCompanion[UserContext]
