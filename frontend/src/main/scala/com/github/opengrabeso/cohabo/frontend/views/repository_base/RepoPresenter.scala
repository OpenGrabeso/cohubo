package com.github.opengrabeso.cohabo
package frontend
package views
package repository_base

import com.avsystem.commons.BSeq
import com.github.opengrabeso.cohabo.frontend.routing.{RoutingState, SettingsPageState}
import dataModel._
import io.udash.Application
import io.udash.properties.model.ModelProperty
import org.scalajs.dom

import scala.scalajs.js
import scala.util.Try

trait RepoPresenter {
  def userService: services.UserContextService
  def application: Application[RoutingState]


  val githubRestApiClient: ApplicationContext.githubRestApiClient.type = ApplicationContext.githubRestApiClient

  def props: ModelProperty[SettingsModel] = userService.properties

  def currentToken(): String = props.subProp(_.token).get

  def pageContexts: BSeq[ContextModel] = userService.properties.transformToSeq(_.activeContexts).get


  var shortRepoIds = Map.empty[ContextModel, String]


  def copyToClipboard(text: String): Unit = {
    dom.window.navigator.asInstanceOf[js.Dynamic].clipboard.writeText(text)
  }

  def gotoUrl(url: String): Unit = {
    dom.window.location.href = url
  }

  def gotoSettings(): Unit = {
    application.goTo(SettingsPageState)
  }


  def addRepository(repo: String): Unit = {
    val repos = props.subSeq(_.contexts)
    Try(ContextModel.parse(repo)).foreach { ctx =>
      if (!repos.get.contains(ctx)) {
        repos.replace(repos.size, 0, ctx)
        SettingsModel.store(props.get)
      }
    }
  }

  def removeRepository(context: ContextModel): Unit = {
    val repos = props.subSeq(_.contexts)
    val find = repos.get.indexOf(context)
    if (find >= 0) {
      repos.replace(find, 1)
      SettingsModel.store(props.get)
    }
  }

}
