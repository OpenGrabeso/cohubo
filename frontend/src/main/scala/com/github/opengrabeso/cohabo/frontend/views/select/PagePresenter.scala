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

import scala.util.Success

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
        }
      }
      if (!load.isCompleted) {
        // if not completed immediately, show as pending
        model.subProp(_.loading).set(true)
        model.subProp(_.articles).set(Nil)
      }

      for (issues <- load) {
        // TODO: handle multilevel parent / children
        //println(issues)
        /*
        val roots = allArticles.filter(_.comment.isEmpty).map(_.issue).distinct
        val children = allArticles.groupBy(_.issue).mapValues(_.filter(_.comment.isDefined))
        val parents = children.toSeq.flatMap { case (parent, ch) =>
          ch.map(_ -> parent)
        }.toMap
        */

        model.subProp(_.articles).set(issues.toSeq.flatMap { case (id, comments) =>

          val p = ArticleIdModel(id.number.toString, None)

          ArticleRowModel(p, None, comments.nonEmpty, 0, id.title, id.user.displayName, id.updated_at) +:
            comments.map(i => ArticleRowModel(p, Some(p), false, 1, i.body, i.user.displayName, i.updated_at))
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
