package com.github.opengrabeso.cohabo.frontend.dataModel

import io.udash.HasModelPropertyCreator

case class SettingsModel(token: String = null, organization: String = null, repository: String = null, user: UserLoginModel = UserLoginModel())

object SettingsModel extends HasModelPropertyCreator[SettingsModel]
