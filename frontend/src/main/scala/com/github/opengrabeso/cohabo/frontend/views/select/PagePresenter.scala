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
import io.udash.rest.raw.HttpErrorException

import scala.concurrent.{ExecutionContext, Future, Promise}
import services.UserContextService

import scala.annotation.tailrec
import scala.collection.immutable.TreeSet
import scala.util.{Failure, Success}

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  application: Application[RoutingState],
  userService: services.UserContextService
)(implicit ec: ExecutionContext) extends Presenter[SelectPageState.type] {

  model.subProp(_.selectedArticleId).listen { id =>
    val sel = model.subProp(_.articles).get.find(id contains _.id)
    val content = sel.map(_.body).getOrElse("Empty article")

    model.subProp(_.articleContent).set(content)
  }

  def removeQuotes(text: String): Iterator[String] = {
    text.linesIterator.filterNot(_.startsWith(">")).filterNot(_.isEmpty)
  }

  def bodyAbstract(text: String): String = {
    val dropQuotes = removeQuotes(text)
    // TODO: smarter abstracts
    dropQuotes.toSeq.head.take(120)
  }

  def extractQuotes(text: String): Seq[String] = {
    text.linesIterator.filter(_.startsWith(">")).map(_.drop(1).trim).filter(_.nonEmpty).toSeq
  }

  def repoValid(valid: Boolean) = {
    model.subProp(_.repoError).set(!valid)
  }

  def loadActivities() = {

    val props = userService.properties
    val sourceParameters = props.subProp(_.user).combine(props.subProp(_.organization))(_ -> _).combine(props.subProp(_.repository))(_ -> _)

    sourceParameters.listen { case ((user, org), repo) =>
      val load = userService.call { api =>
        val repoAPI = api.repos(org, repo)
        val issues = repoAPI.issues()
        issues.flatMap { is =>
          model.subProp(_.loading).set(true)
          model.subProp(_.articles).set(Nil)
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
          case Failure(ex@HttpErrorException(code, _, _)) =>
            if (code != 404) {
              println("Error loading issues from $org/$repo: $ex")
            }
            repoValid(false)
            Failure(ex)
          case Failure(ex) =>
            repoValid(false)
            println("Error loading issues from $org/$repo: $ex")
            Failure(ex)
          case x =>
            repoValid(true)
            x
        }
      }

      for (issues <- load) {

        val log = false

        val allIssues = issues.toSeq.sortBy(_._1.updated_at).reverse.flatMap { case (id, comments) =>

          val p = ArticleIdModel(org, repo, id.number, None)

          val issue = ArticleRowModel(p, false, 0, id.title, id.body, Option(id.milestone).map(_.title), id.user.displayName, id.updated_at)
          val issueWithComments = issue +: comments.zipWithIndex.map { case (i, index) =>
            val articleId = ArticleIdModel(org, repo, id.number, Some((index + 1, i.id)))
            ArticleRowModel(articleId, false, 0, bodyAbstract(i.body), i.body, None, i.user.displayName, i.updated_at)
          }

          val fromEnd = issueWithComments.reverse

          def findByQuote(quote: String, previousFromEnd: List[ArticleRowModel]) = {
            previousFromEnd.filter { i =>
              val withoutQuotes = removeQuotes(i.body)
              withoutQuotes.exists(_.contains(quote))
            }
          }

          @tailrec
          def processLast(todo: List[ArticleRowModel], doneChildren: List[(ArticleRowModel, ArticleRowModel)]): List[(ArticleRowModel, ArticleRowModel)] = {
            todo match {
              case head :: tail =>
                // take last article, find its parent in the original order (check quotes TODO: check references)
                val quotes = extractQuotes(head.body)
                if (log) println(s"quotes ${quotes.toArray.mkString("[",",","]")}")
                val byQuote = quotes.map(findByQuote(_, tail)).filter(_.nonEmpty)
                if (byQuote.nonEmpty) {
                  if (log) println(s"byQuote ${byQuote.map(_.map(_.id)).toVector}")
                  // try to find an intersection (article containing all quotes)
                  val allQuotes = byQuote.tail.foldLeft(byQuote.head) { (all, withQuote) =>
                    all.intersect(withQuote)
                  }
                  // if there is no common intersection, use just the first quote
                  val fallback = if (allQuotes.isEmpty) byQuote.head.headOption.toSeq else allQuotes
                  val parent = if (fallback.nonEmpty) fallback.headOption else tail.headOption
                  processLast(tail, parent.map(_ -> head).toList ++ doneChildren)
                } else {
                  // when nothing is found, take previous article
                  processLast(tail, tail.headOption.map(_ -> head).toList ++ doneChildren)
                }

              case _ =>
                doneChildren
            }
          }
          if (log) println(s"fromEnd ${fromEnd.map(_.body)}")
          val childrenOf = processLast(fromEnd.toList, Nil).groupBy(_._1).mapValues(_.map(_._2))
          if (log) println(s"root $issue")
          if (log) println(s"childrenOf ${childrenOf.map{case (k,v) => k.id -> v.map(_.id)}}")
          def traverseDepthFirst(i: ArticleRowModel, level: Int): List[ArticleRowModel] = {
            if (log) println("  " * level + i.body)
            val children = childrenOf.get(i).toList.flatten
            i.copy(indent = level, hasChildren = children.nonEmpty) :: children.flatMap(traverseDepthFirst(_, level + 1))
          }
          traverseDepthFirst(issue, 0).tap { h =>
            if (log) println(s"Hierarchy ${h.map(_.id)}")
          }
        }


        model.subProp(_.articles).set(allIssues)

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
