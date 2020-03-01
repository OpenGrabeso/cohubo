package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.{ZoneOffset, ZonedDateTime}

import common.model._
import common.Util._
import routing._
import io.udash._

import scala.concurrent.{ExecutionContext, Future, Promise}
import services.UserContextService

import scala.scalajs.js
import scala.util.{Failure, Success}

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  application: Application[RoutingState],
  userService: services.UserContextService
)(implicit ec: ExecutionContext) extends Presenter[SelectPageState.type] {

  /*
  model.subProp(_.showAll).listen { p =>
    loadActivities(p)
  }

   */

  model.subProp(_.selectedArticleId).listen { id =>
    model.subProp(_.articleContent).set("Loading...")
    import scala.scalajs.js.timers._
    val content = Promise[String]()
    setTimeout(200){ // simulate async loading
      content.success(s"Article content of $id")
    }

    content.future.map{ content =>
      model.subProp(_.articleContent).set(content)
    }

  }

  def loadActivities() = {
    val load = userService.loadCached()

    if (!load.isCompleted) {
      // if not completed immediately, show as pending
      model.subProp(_.loading).set(true)
      model.subProp(_.articles).set(Nil)
    }

    for (UserContextService.LoadedActivities(stagedActivities) <- load) {

      model.subProp(_.articles).set(stagedActivities.map(id => ArticleRow(id.id, "??? " + id.id.toString, selected = false)))
      model.subProp(_.loading).set(false)
    }

  }

  override def handleState(state: SelectPageState.type): Unit = {}

  def uploadNewActivity() = {
    /*
    val selectedFiles = model.subSeq(_.uploads.selectedFiles).get

    val userId = userService.userId.get

    val uploader = new FileUploader(Url(s"/rest/user/$userId/upload"))
    val uploadModel = uploader.upload("files", selectedFiles, extraData = Map(("timezone":js.Any) -> (TimeFormatting.timezone:js.Any)))
    uploadModel.listen { p =>
      model.subProp(_.uploads.state).set(p)
      for {
        response <- p.response
        responseJson <- response.text
      } {
        val activities = JsonUtils.read[Seq[ActivityHeader]](responseJson)
        // insert the activities into the list
        model.subProp(_.activities).set {
          val oldAct = model.subProp(_.activities).get
          val newAct = activities.filterNot(a => oldAct.exists(_.staged.exists(_.id == a.id)))
          val all = oldAct ++ newAct.map { a=>
            println(s"Add $a")
            ActivityRow(Some(a), None, selected = true)
          }
          all.sortBy(a => a.staged.orElse(a.strava).get.id.startTime)
        }
      }
    }
   */
  }

  def gotoSettings(): Unit = {
    application.goTo(SettingsPageState)
  }

}
