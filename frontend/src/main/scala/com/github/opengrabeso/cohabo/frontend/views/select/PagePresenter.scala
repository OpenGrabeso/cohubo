package com.github.opengrabeso.cohabo
package frontend
package views
package select

import com.sun.net.httpserver.Authenticator.Success
import dataModel._
import common.model._
import common.Util._
import routing._
import io.udash._

import scala.concurrent.{ExecutionContext, Future, Promise}
import services.UserContextService

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
    val sel = model.subProp(_.articles).get.find(id contains _.id)
    val content = sel.map(_.body).getOrElse("Empty article")

    model.subProp(_.articleContent).set(content)
  }

  def bodyAbstract(text: String): String = {
    val dropQuotes = text.linesIterator.filterNot(_.startsWith(">")).filterNot(_.isEmpty)
    // TODO: smarter abstracts
    dropQuotes.toSeq.head.take(120)
  }

  def loadActivities() = {

    val props = userService.properties
    val sourceParameters = props.subProp(_.user).combine(props.subProp(_.organization))(_ -> _).combine(props.subProp(_.repository))(_ -> _)

    sourceParameters.listen { case ((user, org), repo) =>
      val load = userService.call { api =>
        val repoAPI = api.repos(org, repo)
        val issues = repoAPI.issues()
        issues.flatMap { is =>
          // issue requests one by one
          // TODO: some parallel requester
          def requestNext(todo: List[Issue], done: Map[Issue, Seq[Comment]]): Future[Map[Issue, Seq[Comment]]] = {
            todo match {
              case head :: tail =>
                repoAPI.issuesAPI(head.number).comments.map { cs =>
                  done + (head -> cs)
                }.flatMap { d =>
                  requestNext(tail, d)
                }
              case _ =>
                Future.successful(done)
            }
          }
          requestNext(is.toList, Map.empty)
        }.transform {
          case Failure(ex) =>
            ex.printStackTrace()
            Failure(ex)
          case x =>
            x
        }
      }
      if (!load.isCompleted) {
        // if not completed immediately, show as pending
        model.subProp(_.loading).set(true)
        model.subProp(_.articles).set(Nil)
      }

      for (issues <- load) {
        // TODO: handle multilevel parent / children

        model.subProp(_.articles).set(issues.toSeq.flatMap { case (id, comments) =>

          val p = ArticleIdModel(org, repo, id.number, None)

          ArticleRowModel(
            p, None, comments.nonEmpty, 0, id.title, id.body, id.user.displayName, id.updated_at
          ) +: comments.zipWithIndex.map { case (i, index) =>
            val articleId = ArticleIdModel(org, repo, id.number, Some((index + 1, i.id)))
            ArticleRowModel(articleId, None, false, 1, bodyAbstract(i.body), i.body, i.user.displayName, i.updated_at)
          }
        })
        model.subProp(_.loading).set(false)
      }
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
