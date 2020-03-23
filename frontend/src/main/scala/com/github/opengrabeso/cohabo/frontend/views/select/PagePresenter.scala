package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.ZonedDateTime

import rest.DataWithHeaders._
import com.softwaremill.sttp.Method
import rest.{DataWithHeaders, RestAPIClient}
import dataModel._
import common.model._
import common.Util._
import routing._
import io.udash._
import io.udash.rest.raw.HttpErrorException

import scala.concurrent.{ExecutionContext, Future}
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.scalajs.js.timers._
import scala.util.{Failure, Success, Try}
import TimeFormatting._


object PagePresenter {
  def removeQuotes(text: String): Iterator[String] = {
    val Mention = "(?:@[^ ]+ )+(.*)".r
    text.linesIterator.filterNot(_.startsWith(">")).map {
      case Mention(rest) =>
        rest
      case s =>
        s
    }
  }

  def removeHeading(text: String): String = {
    val Heading = "#+ *(.*)".r
    text match {
      case Heading(rest) =>
        rest
      case _ =>
        text
    }
  }
  @scala.annotation.tailrec
  def removeMarkdown(text: String): String = {
    val Link = "(.*)\\[([^\\]]+)\\]\\([^)]+\\)(.*)".r
    text match {
      case Link(prefix, link, postfix) =>
        removeMarkdown(prefix + link + postfix) // there may be multiple links on one line
      case _ =>
        text
    }
  }

  def bodyAbstract(text: String): String = {
    val dropQuotes = removeQuotes(text).map(removeHeading).filterNot(_.isEmpty)
    // TODO: smarter abstracts
    removeMarkdown(dropQuotes.toSeq.head).take(120)
  }

  def extractQuotes(text: String): Seq[String] = {
    text.linesIterator.filter(_.startsWith(">")).map(_.drop(1).trim).filter(_.nonEmpty).toSeq
  }


}

import PagePresenter._

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  application: Application[RoutingState],
  userService: services.UserContextService
)(implicit ec: ExecutionContext) extends Presenter[SelectPageState.type] {

  def props = userService.properties
  val sourceParameters = props.subProp(_.token).combine(props.subProp(_.context))(_ -> _)
  var lastNotifications =  Option.empty[String]
  var scheduled = Option.empty[SetTimeoutHandle]

  model.subProp(_.selectedArticleId).listen { id =>
    val sel = model.subProp(_.articles).get.find(id contains _.id)
    val selParent = model.subProp(_.articles).get.find(id.map(_.copy(id = None)) contains _.id)
    //println(sel + " " + selParent + " from " + id)
    (sel, selParent) match {
      case (Some(s), Some(p)) =>
        model.subProp(_.selectedArticle).set(Some(s))
        model.subProp(_.selectedArticleParent).set(Some(p))
        model.subProp(_.articleContent).set("...")
        val content = s.body
        val props = userService.properties.get
        val context = props.context.relativeUrl
        val renderMarkdown = userService.call(_.markdown.markdown(content, "gfm", context))
        renderMarkdown.map { html =>
          model.subProp(_.articleContent).set(html.data)
        }.failed.foreach { ex =>
          model.subProp(_.articleContent).set(s"Markdown error $ex")
        }
      case _ =>
        model.subProp(_.selectedArticle).set(None)
        model.subProp(_.selectedArticleParent).set(None)
        model.subProp(_.articleContent).set("")
    }

    userService.call(_.rate_limit).foreach { limits =>
      val c = limits.resources.core
      userService.properties.subProp(_.rateLimits).set(Some(c.limit, c.remaining, c.reset))
    }
  }

  def repoValid(valid: Boolean): Unit = {
    model.subProp(_.repoError).set(!valid)
  }


  def initArticles(context: ContextModel): Future[DataWithHeaders[Seq[Issue]]] = {
    userService.call(_.repos(context.organization, context.repository).issues())
  }

  def pageArticles(context: ContextModel, token: String, link: String): Future[DataWithHeaders[Seq[Issue]]] = {
    RestAPIClient.requestWithHeaders[Issue](link, token)
  }

  def rowFromIssue(id: Issue, context: ContextModel) = {
    val p = ArticleIdModel(context.organization, context.repository, id.number, None)
    ArticleRowModel(
      p, id.comments > 0, true, 0, id.title, id.body, Option(id.milestone).map(_.title), id.user.displayName,
      id.created_at, id.created_at, id.updated_at
    )
  }


  def rowFromComment(articleId: ArticleIdModel, i: Comment) = ArticleRowModel(
    articleId, false, false, 0, bodyAbstract(i.body), i.body, None, i.user.displayName,
    i.created_at, i.updated_at, i.updated_at
  )


  def processIssueComments(issue: ArticleRowModel, comments: Seq[ArticleRowModel], context: ContextModel): Unit = { // the comments

    val log = false

    // hasChildren will be set later in traverseDepthFirst if necessary

    val issueWithComments = issue +: comments

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
          if (log) println(s"quotes ${quotes.toArray.mkString("[", ",", "]")}")
          val byQuote = quotes.map(findByQuote(_, tail)).filter(_.nonEmpty)
          if (byQuote.nonEmpty) {
            if (log) println(s"byQuote ${byQuote.map(_.map(_.id)).toVector}")
            // try to find an intersection (article containing all quotes)
            // check
            val mostQuoted = byQuote.flatten.groupBy(identity).mapValues(_.size).maxBy(_._2)._1
            processLast(tail, (mostQuoted -> head) :: doneChildren)
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
    if (log) println(s"childrenOf ${childrenOf.map { case (k, v) => k.id -> v.map(_.id) }}")
    def traverseDepthFirst(i: ArticleRowModel, level: Int): List[ArticleRowModel] = {
      if (log) println("  " * level + i.body)
      val children = childrenOf.get(i).toList.flatten
      i.copy(indent = level, hasChildren = children.nonEmpty) :: children.flatMap(traverseDepthFirst(_, level + 1))
    }
    val hierarchyWithComments = traverseDepthFirst(issue, 0).tap { h =>
      if (log) println(s"Hierarchy ${h.map(_.id)}")
    }

    val a = model.subProp(_.articles)
    // replace the one we have loaded
    val (before, issueAndAfter) = a.get.span(_.id.issueNumber != issue.id.issueNumber)
    val after = issueAndAfter.dropWhile(_.id.issueNumber == issue.id.issueNumber)
    a.set(before ++ hierarchyWithComments ++ after)
    // TODO: we may have to re-sort the issues
  }


  def loadArticlesPage(token: String, context: ContextModel, mode: String): Unit = {
    val loadIssue = mode match {
      case "next" =>
        model.subProp(_.pagingUrls).get.get(mode).map { link =>
          pageArticles(context, token, link)
        }.getOrElse {
          Future.successful(DataWithHeaders(Nil))
        }
      case _ =>
        initArticles(context).tap(_.onComplete {
          case Failure(ex@HttpErrorException(code, _, _)) =>
            if (code != 404) {
              println(s"Error loading issues from ${context.relativeUrl}: $ex")
            }
            repoValid(false)
            Failure(ex)
          case Failure(ex) =>
            repoValid(false)
            println(s"Error loading issues from ${context.relativeUrl}: $ex")
            Failure(ex)
          case x =>
            // settings valid, store them
            SettingsModel.store(userService.properties.get)
            repoValid(true)
        })
    }

    loadIssue.foreach {issuesWithHeaders =>
      model.subProp(_.pagingUrls).set(issuesWithHeaders.headers.paging)

      val is = issuesWithHeaders.data

      val issuesOrdered = is.sortBy(_.updated_at).reverse


      // preview the issues
      val preview = issuesOrdered.map(rowFromIssue(_, context))

      model.subProp(_.articles).tap { a =>
        a.set(a.get ++ preview)
      }
      model.subProp(_.loading).set(false)

      issuesOrdered.map { id => // parent issue
        userService.call { api =>
          api.repos(context.organization, context.repository).issuesAPI(id.number).comments.map { comments => // the comments
            val issue = rowFromIssue(id, context).copy(hasChildren = false, preview = false)
            val commentRows = comments.zipWithIndex.map { case (c, i) =>
              rowFromComment(ArticleIdModel(context.organization, context.repository, id.number, Some(i, c.id)), c)
            }
            processIssueComments(issue, commentRows, context)
          }
        }
      }

    }

  }

  def loadNotifications(token: String, context: ContextModel): Unit  = {
    val logging = true
    scheduled.foreach(clearTimeout)
    scheduled = None

    val defaultInterval = 60
    def scheduleNext(sec: Int): Unit = {
      if (logging) println(s"scheduleNext $sec")
      scheduled = Some(setTimeout(sec.seconds) {
        scheduled = None
        loadNotifications(token, context)
      })
    }
    println(s"Load notifications since $lastNotifications")
    userService.call(_.repos(context.organization, context.repository).notifications(ifModifiedSince = lastNotifications.orNull, all = false)).map { notifications =>
      // TODO:  we need paging if there are many notifications
      if (logging) println(s"Notifications ${notifications.data.size} headers ${notifications.headers}")

      val newUnreadData = notifications.data.filter(_.unread).flatMap{ n =>
        //println(s"Unread ${n.subject}")
        //println(s"  last_read_at ${n.last_read_at}, updated_at: ${n.updated_at}")
        // URL is like: https://api.github.com/repos/gamatron/colabo/issues/26
        val Number = ".*/issues/([0-9]+)".r
        val issueNumber = n.subject.url match {
          case Number(number) =>
            Seq(number.toLong)
          case _ =>
            Seq.empty
        }
        issueNumber.map(_ -> UnreadInfo(n.updated_at, n.last_read_at, n.url))
      }.toMap

      if (lastNotifications.nonEmpty) {
        lastNotifications = None
        // we seem to have received the delta only, update the data we have
        // and make sure the next call will get the full data
        model.subProp(_.unreadInfo).set(model.subProp(_.unreadInfo).get ++ newUnreadData)
      } else {
        model.subProp(_.unreadInfo).set(newUnreadData)
        lastNotifications = notifications.headers.lastModified orElse lastNotifications
      }

      object ParseLastModified {
        def unapply(s: String): Option[ZonedDateTime] = {
          Try(parseHttpTimestamp(s)).toOption
        }
      }
      for {
        ParseLastModified(lastModified) <- notifications.headers.lastModified
      } {
        model.subProp(_.unreadInfoFrom).set(Some(lastModified))
      }
      scheduleNext(notifications.headers.xPollInterval.map(_.toInt).getOrElse(defaultInterval))
    }.failed.foreach {
      case HttpErrorExceptionWithHeaders(ex, headers) =>
        // expected - this mean nothing had changed and there is nothing to do
        if (logging) println(s"Notification headers $headers")
        scheduleNext(headers.xPollInterval.map(_.toInt).getOrElse(defaultInterval))
        if (ex.code != 304) { // // 304 is expected - this mean nothing had changed and there is nothing to do
          println(s"Notifications failed $ex")
        }
      case ex  =>
        scheduleNext(60)
        println(s"Notifications failed $ex")

    }
  }



  def loadArticles(): Unit = {
    // install the handler
    sourceParameters.listen(
      { case (token, context) =>
        model.subProp(_.loading).set(true)
        model.subProp(_.articles).set(Seq.empty)
        loadArticlesPage(token, context, "init")
        lastNotifications = None
        loadNotifications(token, context)
      }, initUpdate = true
    )
  }

  override def handleState(state: SelectPageState.type): Unit = {}

  def loadMore(): Unit = {
    val (token, context) = sourceParameters.get
    loadArticlesPage(token, context, "next")
  }


  def refreshNotifications(): Unit = {
    println("refreshNotifications")
    sourceParameters.get.tap {
      case (token, context) =>
        loadNotifications(token, context)
    }
  }

  def editCurrentArticle(): Unit = {
    val wasEditing = model.subProp(_.editing).get._1
    if (!wasEditing) {
      for {
        id <- model.subProp(_.selectedArticleId).get
        sel <- model.subProp(_.articles).get.find(id == _.id)
      } {
        model.subProp(_.editedArticleMarkdown).set(sel.body)
        model.subProp(_.editing).set((true, false))
      }
    }
  }

  def editCancel(): Unit = {
    model.subProp(_.editing).set((false, false))
  }

  def editOK(): Unit = {
    for {
      selectedId <- model.subProp(_.selectedArticleId).get
      if model.subProp(_.editing).get._1
      context = userService.properties.get.context
    } {
      val body = model.subProp(_.editedArticleMarkdown).get
      if (!model.subProp(_.editing).get._2) {
        println(s"Edit $selectedId")
        // plain edit
        userService.call { api =>
          selectedId match {
            case ArticleIdModel(_, _, issueId, Some((_, commentId))) =>
              api.repos(context.organization, context.repository).editComment(commentId, body).map(_.body)
            case ArticleIdModel(_, _, issueId, None) =>
              val issueAPI = api.repos(context.organization, context.repository).issuesAPI(issueId)
              issueAPI.get.flatMap { i =>
                issueAPI.update(
                  i.title,
                  i.body,
                  i.state,
                  i.milestone.number,
                  i.labels.map(_.name),
                  i.assignees.map(_.login)
                )
              }.map(_.body)
          }
        }.onComplete {
          case Failure(ex) =>
            println(s"Edit failure $ex")
          case Success(body) =>
            model.subProp(_.editing).set((false, false))
            val renderMarkdown = userService.call(_.markdown.markdown(body, "gfm", context.relativeUrl))
            // update the local data: article display and article content in the article table
            renderMarkdown.map { html =>
              model.subProp(_.articleContent).set(html.data)
            }.failed.foreach { ex =>
              model.subProp(_.articleContent).set(s"Markdown error $ex")
            }
            model.subProp(_.articles).tap { as =>
              as.set(as.get.map { a =>
                if (a.id == selectedId) a.copy(body = body)
                else a
              })
            }
        }
      } else {
        // reply (create a new comment)
        userService.call { api =>
          api.repos(context.organization, context.repository).issuesAPI(selectedId.issueNumber).createComment(body).map { c =>
            // add the comment to the article list
            val articles = model.subProp(_.articles).get
            for {
              i <- articles.find(_.id == ArticleIdModel(context.organization, context.repository, selectedId.issueNumber, None))
              comments = articles.filter(a => a.id.issueNumber == selectedId.issueNumber && a.id.id.nonEmpty)
            } {
              val newId = ArticleIdModel(context.organization, context.repository, selectedId.issueNumber, Some(comments.length, c.id))
              val newRow = rowFromComment(newId, c)
              processIssueComments(i, comments :+ newRow, context)
            }
          }
        }
      }.onComplete {
        case Failure(ex) =>
          println(s"Reply failure $ex")
        case Success(s) =>
          model.subProp(_.editing).set((false, false))
      }
    }
  }

  def markAsRead(id: ArticleIdModel): Unit = {
    val unreadInfo = model.subProp(_.unreadInfo).get
    for (unread <- unreadInfo.get(id.issueNumber)) {
      println(s"markAsRead $id, unread $unread")
      RestAPIClient.request[Unit](method = Method.PATCH, uri = unread.threadURL, token = props.subProp(_.token).get).map{_ =>
        println(s"markAsRead done - adjust unreadInfo")
      }.failed.foreach(ex =>
        println(s"Mark as read error $ex")
      )

    }

  }

  def reply(id: ArticleIdModel): Unit = {
    val wasEditing = model.subProp(_.editing).get._1
    if (!wasEditing) {
      // TODO: autoquote if needed
      println(s"Reply to $id")
      model.subProp(_.editing).set((true, true))
      model.subProp(_.selectedArticleId).set(Some(id))
      model.subProp(_.editedArticleMarkdown).set("")
      model.subProp(_.editedArticleHTML).set("")
    }
  }

  def gotoSettings(): Unit = {
    application.goTo(SettingsPageState)
  }

}
