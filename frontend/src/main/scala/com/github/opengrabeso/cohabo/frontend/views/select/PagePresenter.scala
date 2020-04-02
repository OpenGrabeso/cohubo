package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.{ZoneId, ZonedDateTime}

import rest.DataWithHeaders._
import com.softwaremill.sttp.Method
import rest.{DataWithHeaders, RestAPIClient}
import dataModel._
import common.model._
import common.Util._
import routing._
import io.udash._
import io.udash.rest.raw.HttpErrorException

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.scalajs.js.timers._
import scala.util.{Failure, Success, Try}
import TimeFormatting._
import io.udash.wrappers.jquery.jQ
import org.scalajs.dom

import scala.collection.mutable
import scala.scalajs.js

object PagePresenter {
  implicit class ProcessLines(lines: Iterator[String]) {
    def removeQuotes: Iterator[String] = {
      val Mention = "(?:@[^ ]+ )+(.*)".r
      lines.filterNot(_.startsWith(">")).filterNot(_.startsWith("***▽ ")).map {
        case Mention(rest) =>
          rest
        case s =>
          s
      }
    }
    def removeCodePrefix: Iterator[String] = lines.filterNot(_.startsWith("```"))
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

  implicit final class MarkdownTransform(val text: String) {
    @scala.annotation.tailrec
    def removeMarkdown: String = {
      val Link = "(.*)\\[([^\\]]+)\\]\\([^)]+\\)(.*)".r
      text match {
        case Link(prefix, link, postfix) =>
          (prefix + link + postfix).removeMarkdown // there may be multiple links on one line
        case _ =>
          text
      }
    }
    def decodeEntities: String = {
      js.Dynamic.global.he.decode(text).asInstanceOf[String]
    }
    @scala.annotation.tailrec
    def removeHTMLTags: String = {
      val Link = "(.*)</?[a-zA-Z]/?>(.*)".r
      text match {
        case Link(prefix, postfix) =>
          (prefix + postfix).removeHTMLTags // there may be multiple links on one line
        case _ =>
          text
      }
    }
  }


  def rowTitle(text: String, parentTitle: String): String = {
    // if there is a comment title (Colabo export, but anyone can do it as well), use it
    val lines = text.linesIterator
    val heading = lines.removeQuotes.dropWhile(_.isEmpty).takeWhile(_.startsWith("#")).map(removeHeading).toSeq.headOption
    // otherwise use the parent issue title
    heading.getOrElse(parentTitle)
  }

  def bodyAbstract(text: String): String = {
    val dropQuotes = text.linesIterator.removeQuotes.removeCodePrefix.map(removeHeading).filterNot(_.isEmpty)
    // TODO: smarter abstracts
    dropQuotes.toSeq.headOption.getOrElse("")
      .removeMarkdown
      .removeHTMLTags
      .decodeEntities
      .take(120)
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
  def pageContexts = userService.properties.subSeq(_.contexts).get

  val pagingUrls =  mutable.Map.empty[ContextModel, Map[String, String]]

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
        renderMarkdown(s.body, s.id.context)
      case _ =>
        model.subProp(_.selectedArticle).set(None)
        model.subProp(_.selectedArticleParent).set(None)
        model.subProp(_.articleContent).set("")
    }

  }

  private def updateRateLimits(): Unit = {
    userService.call(_.rate_limit).foreach { limits =>
      val c = limits.resources.core
      userService.properties.subProp(_.rateLimits).set(Some(c.limit, c.remaining, c.reset))
    }
  }


  private def initArticles(context: ContextModel): Future[DataWithHeaders[Seq[Issue]]] = {
    userService.call(_.repos(context.organization, context.repository).issues())
  }

  private def pageArticles(context: ContextModel, token: String, link: String): Future[DataWithHeaders[Seq[Issue]]] = {
    RestAPIClient.requestWithHeaders[Issue](link, token)
  }

  private def localZoneId: ZoneId = {
    // it seems ZoneId.systemDefault is not implemented properly, we provide our own implementation
    ZoneId.of(new DateTimeFormatX().resolvedOptions().timeZone.getOrElse("Etc/GMT"))
  }

  private val FullCommentHeader = "> \\*+[A-Za-z0-9_]+\\*+ _([0-9]+)\\.([0-9]+)\\.([0-9]+) ([0-9]+):([0-9]+):([0-9]+)_ \\*+Tags:\\*+ .*".r
  private val DotNoteHeader = "> \\*+[A-Za-z0-9_]+\\** _*([0-9]+)\\.([0-9]+)\\.([0-9]+) ([0-9]+):([0-9]+):([0-9]+)_*\\**".r
  private val SlashNoteHeader = "> \\*+[A-Za-z0-9_]+\\** _*([0-9]+)/([0-9]+)/([0-9]+) ([0-9]+):([0-9]+):([0-9]+)_*\\**".r

  def removeColaboHeaders(body: String): String = {
    val Title = "####.*".r
    val bodyLines = body.linesIterator.toSeq
    val linesWithoutHeaders = bodyLines.take(2).map {
      case FullCommentHeader(_*) => None
      case DotNoteHeader(_*) => None
      case SlashNoteHeader(_*) => None
      case x =>
        Some(x)
    } match {
      case Seq(Some(Title(_*)), None) => bodyLines.drop(2)// second line is a timestamp, first line is a title - drop both
      case Seq(None, _) => bodyLines.drop(1) // first line is a timestamp, drop it
      case s => bodyLines
    }
    linesWithoutHeaders.mkString("\n")

  }

  private def extractQuoteHeader(body: String): Option[ZonedDateTime] = {
    // search for an article header in a form > **<Login>** _<DD>.<MM>.<YYYY> <hh>:<mm>:<ss>_ **Tags:** <tags>
    // this must be on a first or a second line
    body.linesIterator.toSeq.take(2).flatMap {
      case FullCommentHeader(day,month,year,hour,minute,second) =>
        Try(
          ZonedDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt, second.toInt, 0, localZoneId)
        ).toOption
      case _ =>
        None
    }.headOption
  }

  private def extractCommentNoteHeader(body: String): Option[ZonedDateTime] = {
    // search for an article header in a form
    // > ***<Login>*** <DD>.<MM>.<YYYY> _<hh>:<mm>:<ss>_
    // or
    // > ***<Login> <DD>/<MM>/<YYYY> <hh>:<mm>:<ss>***

    // this must be on a first or a second line
    body.linesIterator.toSeq.take(2).flatMap {
      case DotNoteHeader(day,month,year,hour,minute,second) =>
        Try(
          ZonedDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt, second.toInt, 0, localZoneId)
        ).toOption
      case SlashNoteHeader(month,day,year,hour,minute,second) =>
        Try(
          ZonedDateTime.of(year.toInt, month.toInt, day.toInt, hour.toInt, minute.toInt, second.toInt, 0, localZoneId)
        ).toOption
      case x =>
        None
    }.headOption
  }

  private def overrideCreatedAt(body: String): Option[ZonedDateTime] = {
    extractQuoteHeader(body).orElse(extractCommentNoteHeader(body))
  }

  private def overrideEditedAt(body: String): Option[ZonedDateTime] = {
    None
  }
  private def rowFromIssue(i: Issue, context: ContextModel) = {
    val p = ArticleIdModel(context.organization, context.repository, i.number, None)
    val explicitCreated = overrideCreatedAt(i.body)
    val explicitEdited = overrideEditedAt(i.body).orElse(explicitCreated)
    ArticleRowModel(
      p, i.comments > 0, true, 0, i.title, i.body, Option(i.milestone).map(_.title), i.user.displayName,
      explicitCreated.getOrElse(i.created_at), explicitEdited.getOrElse(i.created_at), explicitEdited.getOrElse(i.updated_at)
    )
  }


  private def rowFromComment(articleId: ArticleIdModel, i: Comment) = {
    val explicitCreated = overrideCreatedAt(i.body)
    val explicitEdited = overrideEditedAt(i.body).orElse(explicitCreated)
    ArticleRowModel(
      articleId, false, false, 0, bodyAbstract(i.body), i.body, None, i.user.displayName,
      explicitCreated.getOrElse(i.created_at), explicitEdited.getOrElse(i.updated_at), explicitEdited.getOrElse(i.updated_at)
    )
  }

  private def findByQuote(quote: String, findIn: List[ArticleRowModel]): List[ArticleRowModel] = {
    findIn.filter { i =>
      val withoutQuotes = i.body.linesIterator.removeQuotes
      withoutQuotes.exists(_.contains(quote))
    }
  }

  private def processIssueComments(issue: ArticleRowModel, comments: Seq[ArticleRowModel], context: ContextModel): Unit = { // the comments

    if (!pageContexts.contains(context)) {
      println(s"Discard pending issue for $context- repository changed to $pageContexts")
      return
    }

    val log = false

    // hasChildren will be set later in traverseDepthFirst if necessary

    val issueWithComments = issue +: comments

    val fromEnd = issueWithComments.reverse

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

    val a = model.subSeq(_.articles)
    val (before, issueAndAfter) = a.get.span(_.id.issueNumber != issue.id.issueNumber)
    if (issueAndAfter.isEmpty) { // a new issue
      a.replace(0, 0, hierarchyWithComments:_*)
    } else { // replace the one we have loaded
      val after = issueAndAfter.dropWhile(_.id.issueNumber == issue.id.issueNumber)
      a.replace(before.length, issueAndAfter.length - after.length, hierarchyWithComments:_*)
    }
  }

  private def focusEdit(): Unit = {
    jQ(dom.document).find("#edit-text-area").trigger("focus")
  }


  def clearAllArticles(): Unit = {
    val issues = model.subSeq(_.articles).size
    model.subSeq(_.articles).tap { a =>
      a.replace(0, a.get.length)
    }
    println(s"clearAllArticles: cleared $issues, now ${model.subSeq(_.articles).size}")
  }

  def clearArticles(context: ContextModel): Unit = {
    println(s"clearArticles $context")
    // using as.get.filter would be much simpler, but .replace on seq. property is much faster
    val as = model.subSeq(_.articles)
    @scala.annotation.tailrec
    def removeRecurse(): Unit = {
      val articles = as.get
      def notFoundAtEnd(i: Int) = if (i < 0) articles.length else i
      val start = notFoundAtEnd(articles.indexWhere(_.id.from(context)))
      val end = notFoundAtEnd(articles.indexWhere(!_.id.from(context), start))
      if (end > start) {
        println(s"Remove articles $start.$end")
        as.replace(start, end - start)
        removeRecurse()
      }
    }

    removeRecurse()
  }


  var loadInProgress = mutable.Set.empty[(String, ContextModel, String)]

  def loadArticlesPage(token: String, context: ContextModel, mode: String): Unit = {
    val loadId = (token, context, mode)
    // avoid the same load flying twice
    if (loadInProgress.contains(loadId)) {
      return
    }
    loadInProgress += loadId

    val loadIssue = mode match {
      case "next" =>
        pagingUrls.get(context).flatMap(_.get(mode)).map { link =>
          println(s"Page $mode articles $context")
          pageArticles(context, token, link)
        }.getOrElse {
          Future.successful(DataWithHeaders(Nil))
        }
      case _ =>
        initArticles(context).tap(_.onComplete {
          case Failure(ex@HttpErrorException(code, _, _)) =>
            if (code != 404) {
              println(s"HTTP Error $code loading issues from ${context.relativeUrl}: $ex")
            }
            Failure(ex)
          case Failure(ex) =>
            println(s"Error loading issues from ${context.relativeUrl}: $ex")
            ex.printStackTrace()
            Failure(ex)
          case _ =>
            // settings valid, store them
        })
    }

    loadIssue.foreach {issuesWithHeaders =>

      loadInProgress -= loadId

      println(s"loadArticlesPage $context: Issues present ${model.subSeq(_.articles).size}")


      pagingUrls += context -> issuesWithHeaders.headers.paging

      val is = issuesWithHeaders.data

      val issuesOrdered = is.sortBy(_.updated_at).reverse


      // preview the issues
      val preview = issuesOrdered.map(rowFromIssue(_, context))

      model.subSeq(_.articles).tap { a =>
        a.replace(a.get.length, 0, preview:_*)
      }
      model.subProp(_.loading).set(false)

      val issueFutures = issuesOrdered.map { id => // parent issue

        userService.call { api =>

          val apiDone = Promise[Unit]()
          val issue = rowFromIssue(id, context).copy(hasChildren = false, preview = false)

          def processComments(done: Seq[Comment], resp: DataWithHeaders.Headers): Unit = {

            resp.paging.get("next") match {
              case Some(next) =>
                RestAPIClient.requestWithHeaders[Comment](next, token).map(c => processComments(done ++ c.data, c.headers)).failed.foreach(apiDone.failure)
              case None =>
                val commentRows = done.zipWithIndex.map { case (c, i) =>
                  rowFromComment(ArticleIdModel(context.organization, context.repository, id.number, Some(i, c.id)), c)
                }
                processIssueComments(issue, commentRows, context)
                apiDone.success(())
            }

          }

          api.repos(context.organization, context.repository).issuesAPI(id.number).comments.map(c => processComments(c.data, c.headers)).failed.foreach(apiDone.failure)

          apiDone.future
        }.tap(_.failed.foreach(_.printStackTrace()))
      }

      Future.sequence(issueFutures).onComplete(_ => updateRateLimits())

    }

  }

  def clearNotifications(): Unit = {
    scheduled.foreach(clearTimeout)
    scheduled = None
  }

  def loadNotifications(token: String, context: ContextModel): Unit  = {
    val logging = true

    clearNotifications()

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


  private def doLoadArticles(token: String, context: ContextModel): Unit = {
    println(s"Load articles $context $token")
    loadArticlesPage(token, context, "init")
    //lastNotifications = None
    //loadNotifications(token, context)
  }

  def init(): Unit = {
    // load the settings before installing the handler
    // otherwise both handlers are called, which makes things confusing
    props.set(SettingsModel.load)
    // install the handler
    println(s"Install loadArticles handlers, token ${props.subProp(_.token).get}")
    props.subProp(_.token).listen { token =>
      model.subProp(_.loading).set(true)
      println(s"Token changed to $token, contexts: ${props.subSeq(_.contexts).size}")
      clearAllArticles()
      for (context <- props.subProp(_.contexts).get) {
        doLoadArticles(token, context)
      }
    }

    props.subSeq(_.contexts).listenStructure { patch =>
      val token = props.subProp(_.token).get
      println(s"listenStructure add ${patch.added.map(_.get).mkString(",")} remove ${patch.removed.map(_.get).mkString(",")} token: $token")
      if (patch.clearsProperty || token.isEmpty) {
        // completely empty - we can do much simpler cleanup (and shutdown any periodic handlers)
        clearAllArticles()
      } else {
        patch.removed.map(_.get).filter(_.valid).foreach(clearArticles)
        patch.added.map(_.get).filter(_.valid).foreach(doLoadArticles(token, _))
        // TODO: handle notifications properly
        /*
        loadArticlesPage(token, context, "init")
        lastNotifications = None
         */
        clearNotifications()
        lastNotifications = None
      }
    }
    // handlers installed, execute them
    // do not touch token, that would initiate another login
    model.subProp(_.loading).set(true)
    props.subSeq(_.contexts).touch()

  }


  override def handleState(state: SelectPageState.type): Unit = {}

  def loadMore(): Unit = {
    // TODO: be smart, decide which repositories need more issues
    val token = props.subProp(_.token).get
    for (context <- pageContexts) {
      loadArticlesPage(token, context, "next")
    }
  }


  def refreshNotifications(): Unit = {
    /*
    println("refreshNotifications")
    sourceParameters.get.tap {
      case (token, context) =>
        loadNotifications(token, context)
    }
    */
  }

  val markdownCache = new Cache[(String, ContextModel), Future[String]](100, source =>
    userService.call(_.markdown.markdown(source._1, "gfm", source._2.relativeUrl)).map(_.data)
  )

  def renderMarkdown(body: String, context: ContextModel): Unit = {
    val strippedBody = removeColaboHeaders(body)
    val htmlResult = markdownCache(strippedBody -> context)
    // update the local data: article display and article content in the article table
    htmlResult.map { html =>
      model.subProp(_.articleContent).set(html)
    }.failed.foreach { ex =>
      markdownCache.remove(strippedBody -> context) // avoid caching failed requests
      model.subProp(_.articleContent).set(s"Markdown error $ex")
    }
  }

  def editCancel(): Unit = {
    model.subProp(_.editing).set((false, false))
  }

  def addRepository(): Unit = {
    val repos = props.subSeq(_.contexts)
    val repo = model.subProp(_.newRepo).get
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

  private def wasEditing(): Boolean = model.subProp(_.editing).get._1

  def isEditingProperty: ReadableProperty[Boolean] = model.subProp(_.editing).transform(_._1)

  def editCurrentArticle(): Unit = {
    if (!wasEditing()) {
      for {
        id <- model.subProp(_.selectedArticleId).get
        sel <- model.subProp(_.articles).get.find(id == _.id)
      } {
        model.subProp(_.editedArticleMarkdown).set(sel.body)
        model.subProp(_.editing).set((true, false))
        focusEdit()
      }
    }
  }

  def editDone(selectedId: ArticleIdModel, body: String): Unit = {
    println(s"Edit $selectedId")
    // plain edit
    val context = selectedId.context
    userService.call { api =>
      selectedId match {
        case ArticleIdModel(_, _, issueId, Some((_, commentId))) =>
          api.repos(context.organization, context.repository).editComment(commentId, body).map(_.body)
        case ArticleIdModel(_, _, issueId, None) =>
          val issueAPI = api.repos(context.organization, context.repository).issuesAPI(issueId)
          issueAPI.get.flatMap { i =>
            issueAPI.update(
              i.title,
              body,
              i.state,
              Option(i.milestone).map(_.number).getOrElse(-1),
              i.labels.map(_.name),
              i.assignees.map(_.login)
            )
          }.map(_.body)
      }
    }.onComplete {
      case Failure(ex) =>
        println(s"Edit failure $ex")
      case Success(body) =>
        // if the selection has changed while the Future was flying, ignore the result
        if (model.subProp(_.selectedArticleId).get.contains(selectedId)) {
          model.subProp(_.editing).set((false, false))
          renderMarkdown(body, context)
          model.subSeq(_.articles).tap { as =>
            val index = as.get.indexWhere(_.id == selectedId)
            val a = as.get(index)
            as.replace(index, 1, a.copy(body = body))
          }
        }
    }
  }

  def replyDone(selectedId: ArticleIdModel, body: String): Unit = {
    // reply (create a new comment)
    val context = selectedId.context
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
    }.onComplete {
      case Failure(ex) =>
        println(s"Reply failure $ex")
      case Success(s) =>
        model.subProp(_.editing).set((false, false))
    }
  }

  def newIssueDone(body: String): Unit = {
    // TODO: which repository?
    for (context <- pageContexts.headOption) { // remember the context across the futures, so that we can verify it has not changed
      userService.call { api =>
        api.repos(context.organization, context.repository).createIssue(
          bodyAbstract(body), // TODO: proper title
          body
          // TODO: allow providing more properties
        )
      }.onComplete {
        case Failure(ex) =>
          println(s"New issue failure $ex")
        case Success(s) =>
          val newRow = rowFromIssue(s, context)
          processIssueComments(newRow, Seq.empty, context)
          model.subProp(_.editing).set((false, false))
      }
    }

  }
  def editOK(): Unit = {
    if (!model.subProp(_.editing).get._1) return
    val editedId = model.subProp(_.selectedArticleId).get
    val body = model.subProp(_.editedArticleMarkdown).get
    (editedId,model.subProp(_.editing).get._2) match {
      case (Some(selectedId), false) =>
        editDone(selectedId, body)
      case (Some(selectedId), true) =>
        replyDone(selectedId, body)
      case (None, _) => // New issue
        newIssueDone(body)
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
    if (!wasEditing()) {
      // TODO: autoquote if needed
      // check all existing replies to the issue
      val replies = model.subProp(_.articles).get.filter(a => a.id.issueNumber == id.issueNumber)
      // there always must exists at least the reply we are replying to, it does not have to be a comment, though
      val maxReplyNumber = replies.flatMap(_.id.id).map(_._1).maxOpt

      val isLast = (id.id.map(_._1), maxReplyNumber) match {
        case (Some(iid), Some(r)) if iid == r =>
          true
        case (None, None) =>
          true
        case _ =>
          false
      }

      val quote = if (!isLast) {
        val replyTo = replies.find(_.id == id).get
        // TODO: smarter quote
        "> " + bodyAbstract(replyTo.body) + "\n\n"
      } else ""

      println(s"Reply to $id")
      model.subProp(_.editing).set((true, true))
      model.subProp(_.selectedArticleId).set(Some(id))
      model.subProp(_.editedArticleMarkdown).set(quote)
      model.subProp(_.editedArticleHTML).set("")
      focusEdit()
    }
  }

  def newIssue(): Unit = {
    if (!wasEditing()) {
      model.subProp(_.editing).set((true, true))
      model.subProp(_.selectedArticleId).set(None)
      model.subProp(_.editedArticleMarkdown).set("")
      model.subProp(_.editedArticleHTML).set("")
      focusEdit()
    }
  }

  def copyToClipboard(text: String): Unit = {
    dom.window.navigator.asInstanceOf[js.Dynamic].clipboard.writeText(text)
  }

  def copyLink(id: ArticleIdModel): Unit = {
    val link: String = id.issueUri
    copyToClipboard(link)
  }

  def gotoUrl(url: String): Unit = {
    dom.window.location.href = url
  }

  def gotoGithub(id: ArticleIdModel): Unit = {
    gotoUrl(id.issueUri)
  }

  def closeIssue(id: ArticleIdModel): Unit = {
    val context = id.context
    userService.call { api =>
      val issueAPI = api.repos(context.organization, context.repository).issuesAPI(id.issueNumber)
      issueAPI.get.flatMap { i =>
        issueAPI.update(
          i.title,
          i.body,
          "closed",
          Option(i.milestone).map(_.number).getOrElse(-1),
          i.labels.map(_.name),
          i.assignees.map(_.login)
        )
      }.map(_.body)
    }.onComplete {
      case Success(_) =>
        // by default we do not display closed issues - the default reaction is to remove the one we have closed
        // TODO: we could probably mark is somehow instead, that would be less distruptive
        val a = model.subSeq(_.articles)
        val as = a.get
        val before = as.indexWhere(_.id.issueNumber == id.issueNumber)
        val after = as.indexWhere(_.id.issueNumber != id.issueNumber, before)

        a.replace(before, after - before)

      case Failure(ex) =>
        println(s"Error closing #${id.issueNumber}: $ex")
    }

  }


  def gotoSettings(): Unit = {
    application.goTo(SettingsPageState)
  }

}
