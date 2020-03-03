package com.github.opengrabeso.cohabo
package frontend
package views
package select

import dataModel._
import common.model._
import common.Util._
import routing._
import io.udash._

import scala.concurrent.{ExecutionContext, Promise}
import services.UserContextService

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
    val load = userService.loadIssues()

    if (!load.isCompleted) {
      // if not completed immediately, show as pending
      model.subProp(_.loading).set(true)
      model.subProp(_.articles).set(Nil)
    }

    for (UserContextService.LoadedActivities(allArticles) <- load) {
      def idToModel(id: ArticleId) = ArticleIdModel(id.issue, id.comment)

      // TODO: handle multilevel parent / children
      val roots = allArticles.filter(_.comment.isEmpty).map(_.issue).distinct
      val children = allArticles.groupBy(_.issue).mapValues(_.filter(_.comment.isDefined))
      val parents = children.toSeq.flatMap { case (parent, ch) =>
        ch.map(_ -> parent)
      }.toMap

      model.subProp(_.articles).set(roots.flatMap { id =>

        val ch = children.get(id).toSeq.flatten.map(idToModel)
        val p = ArticleIdModel(id, None)

        ArticleRowModel(p, None, ch, 0, "??? " + id.toString) +:
        ch.map(i => ArticleRowModel(i, Some(p), Seq.empty, 1, "??? " + i.toString))
      })
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
