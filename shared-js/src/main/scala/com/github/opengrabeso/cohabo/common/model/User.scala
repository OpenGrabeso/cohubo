package com.github.opengrabeso.cohabo
package common.model

import rest.EnhancedRestDataCompanion

case class User( // https://developer.github.com/v3/users/#get-the-authenticated-user
  login: String,
  id: Long,
  url: String = null,
  name: String = null
)

object User extends EnhancedRestDataCompanion[User]
