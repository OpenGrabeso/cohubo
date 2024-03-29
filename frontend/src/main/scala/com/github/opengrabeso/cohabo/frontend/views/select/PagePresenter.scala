package com.github.opengrabeso.cohabo
package frontend
package views
package select

import java.time.temporal.ChronoUnit
import java.time.{ZoneId, ZonedDateTime}
import com.github.opengrabeso.github.{rest => githubRest}
import com.github.opengrabeso.github.model._
import githubRest.DataWithHeaders._
import sttp.client3._
import githubRest.DataWithHeaders
import dataModel._
import common.Util._
import common.ShortIds
import routing._
import io.udash._
import io.udash.rest.raw.HttpErrorException

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.scalajs.js.timers._
import scala.util.{Failure, Success, Try}
import TimeFormatting._
import QueryAST._
import io.udash.wrappers.jquery.jQ
import org.scalajs.dom
import sttp.model.Method

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

  object Title {
    // we want something robust against unexpected characters (surrogate pairs), regex is not working with them
    def unapply(text: String): Boolean = {
      text.take(1) == "#" && text.dropWhile(_ == '#').take(1) == " "
    }
  }


  def removeHeading(text: String): String = {
    // regex does not work for surrogate pairs
    text match {
      case Title() =>
        text.dropWhile(_ == '#').dropWhile(_ == ' ')
      case _ =>
        text
    }
  }

  implicit final class MarkdownTransform(private val text: String) {
    @scala.annotation.tailrec
    def removeMarkdown: String = {
      val ImageLink = "(.*)!\\[([^\\]]+)\\]\\([^)]+\\)(.*)".r
      val Link = "(.*)\\[([^\\]]+)\\]\\([^)]+\\)(.*)".r
      text match {
        case ImageLink(prefix, link, postfix) =>
          (prefix + postfix).removeMarkdown
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
      val Tag = "(.*)</?[a-zA-Z]/?>(.*)".r
      text match {
        case Tag(prefix, postfix) =>
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

  private def dropQuotes(text: String): Iterator[String] = {
    text.linesIterator.removeQuotes.removeCodePrefix.map(removeHeading).filterNot(_.isEmpty)
  }

  def bodyAbstract(text: String): String = {
    // TODO: smarter abstracts
    val textAbstract = dropQuotes(text).map(_
      .removeMarkdown
      .removeHTMLTags
      .decodeEntities
    ).find(_.nonEmpty).getOrElse("").take(120)
    removeHeading(textAbstract)
  }

  // similar to bodyAbstract, but keeps Markdown so that the quotes can be found in the original source
  def quoteFirstLine(text: String): String = {
    // TODO: smarter quote
    dropQuotes(text).toSeq.headOption.getOrElse("").take(120)
  }

  def extractQuotes(text: String): Seq[String] = {
    text.linesIterator.filter(_.startsWith(">")).map(_.drop(1).trim).filter(_.nonEmpty).toSeq
  }


  sealed trait Filter
  case class IssueFilter(state: String, labels: Seq[String], assignee: Option[String]) extends Filter
  case class SearchFilter(expression: String) extends Filter
}

import PagePresenter._

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  application: Application[RoutingState],
  userService: services.UserContextService
)(implicit ec: ExecutionContext) extends Presenter[SelectPageState] {

  val githubRestApiClient = ApplicationContext.githubRestApiClient

  def props = userService.properties
  def currentToken(): String = props.subProp(_.token).get
  def pageContexts = userService.properties.transformToSeq(_.activeContexts).get


  val stateFilterProps = model.subProp(_.filterOpen) ** model.subProp(_.filterClosed)
  val assignFilterProps = model.subProp(_.filterUser)
  val queryFilter = stateFilterProps ** assignFilterProps ** model.subProp(_.activeLabels)

  var contextChangedCallback = Option.empty[() => Unit]

  model.subProp(_.filterExpression).listen { expr =>
    // cyclical execution does not happen because the properties are set to the value they already have
    ParseFilterQuery(expr) match {
      case ParseFilterQuery.Success(result, next) =>
        val labels = result.collect { case LabelQuery(x) => x}
        val states = result.collectFirst { case StateQuery(x) => x } // when states are conflicting, prefer the first one
        val assignees = result.collectFirst { case AssigneeQuery(name) => name} // use only the first assignee
        // anything unsupported by the issue query means we have to use the search API
        val isSearch = (labels.map(LabelQuery) ++ states.map(StateQuery)).toSet != result.toSet
        // verify the labels are valid, if not, ignore the filter (happens while typing)
        val allLabels = model.subProp(_.labels).get.map(_.name).toSet
        if (labels.forall(allLabels.contains)) {
          model.subProp(_.activeLabels).set(labels)
          model.subProp(_.filterOpen).set(!states.contains(false))
          model.subProp(_.filterClosed).set(!states.contains(true))
          model.subProp(_.filterUser).set(assignees)
          model.subProp(_.useSearch).set(isSearch)
        }
      case _: ParseFilterQuery.NoSuccess =>
    }
  }


  queryFilter.streamTo(model.subProp(_.filterExpression)) { case (((open, closed), user), labels) =>
    // if search is detected, try to parse the query and adjust it as necessary
    val oldFilter = model.subProp(_.filterExpression).get
    ParseFilterQuery(oldFilter) match {
      case ParseFilterQuery.Success(oldFilterQuery, _) =>
        val openClosedQuery = (open, closed) match {
          case (true, true) => Seq.empty
          case (true, false) => Seq(StateQuery(true))
          case (false, true) => Seq(StateQuery(false))
          case (false, false) => Seq.empty // should not happen
        }
        val labelsQuery = labels.map(LabelQuery)
        val assigneeQuery = user.map(AssigneeQuery)

        val keep = oldFilterQuery.flatMap {
          case _: LabelQuery => None
          case _: StateQuery => None
          case _: AssigneeQuery => None
          case x => Some(x)
        }

        (openClosedQuery ++ labelsQuery ++ assigneeQuery ++ keep).mkString(" ")
      case _ =>
        oldFilter
    }
  }

  val pagingUrls =  mutable.Map.empty[ContextModel, Map[String, String]]

  var issuesPending = mutable.Set.empty[(ContextModel, Long)]
  var commentsPending = mutable.Set.empty[(ContextModel, Long)]

  var lastNotifications =  Option.empty[String]
  var scheduled = Option.empty[SetTimeoutHandle]

  var shortRepoIds = Map.empty[ContextModel, String]

  (model.subProp(_.selectedArticleId) ** model.subProp(_.articles)).listen { case (id, articles) =>
    //println(s"selectedArticleId callback for $id")
    val sel = articles.find(id contains _.id)
    val selParent = articles.find(id.map(_.copy(id = None)) contains _.id)
    //println(sel + " " + selParent + " from " + id)
    (sel, selParent) match {
      case (Some(s), Some(p)) =>
        if (!model.subProp(_.selectedArticle).get.contains(s)) {
          model.subProp(_.selectedArticle).set(Some(s))
          model.subProp(_.selectedArticleParent).set(Some(p))
          model.subProp(_.articleContent).set("...")
          // process long matches first (prefer highlighting the longest match whenever possible)
          val highlight = p.rawParent.text_matches.flatMap(_.matches.map(_.text)).sortBy(_.length).reverse
          renderMarkdown(s.body, s.id.context, Highlight(_, highlight.toSeq))
        }
      case _ =>
        model.subProp(_.selectedArticle).set(None)
        model.subProp(_.selectedArticleParent).set(None)
        model.subProp(_.articleContent).set("")
    }
    application.goTo(SelectPageState(id) , true)

  }

  def listFilter(state: (Boolean, Boolean)): String = {
    state match {
      case (true, true) =>
        "all"
      case (false, true) =>
        "closed"
      case _ => // always list at least open
        "open"
    }
  }

  def filterState(): Filter = {
    if (model.subProp(_.useSearch).get) {
      SearchFilter(model.subProp(_.filterExpression).get)
    } else {
      val labels = model.subProp(_.activeLabels).get
      val state = listFilter(stateFilterProps.get)
      val assignee = model.subProp(_.filterUser).get
      IssueFilter(state, labels, assignee)
    }
  }


  // make sure always at least one of open / closed is active
  model.subProp(_.filterOpen).listen { s =>
    if (!s) model.subProp(_.filterClosed).set(true)
  }
  model.subProp(_.filterClosed).listen { s =>
    if (!s) model.subProp(_.filterOpen).set(true)
  }

  queryFilter.listen { _ =>
    // it seems we could load only the difference when extending the filter, but the trouble is with paging URLs, they need updating as well
    clearAllArticles() // this should not be necessary, contexts.touch should handle it, but this way it is more efficient
    model.subProp(_.loading).set(true)
    unselectedArticle()
    // touch contexts to reload all repositories
    props.subSeq(_.contexts).touch()
  }

  private def updateRateLimits(): Unit = {
    userService.call(_.rate_limit).foreach { limits =>
      val c = limits.resources.core
      userService.properties.subProp(_.rateLimits).set(Some(c.limit, c.remaining, c.reset))
    }
  }


  private def initArticles(context: ContextModel, filter: Filter): Future[DataWithHeaders[Seq[Issue]]] = {
    filter match {
      case i: IssueFilter =>
        userService.call(_
          .repos(context.organization, context.repository)
          .issues(sort = "updated", state = i.state, labels = i.labels.mkString(","), assignee = i.assignee.orNull)
        )
      case SearchFilter(expr) =>
        ParseFilterQuery(expr) match {
          case ParseFilterQuery.Success(query, _) =>
            assert(expr.nonEmpty) // empty should be handled as IssueFilter
            val q = "repo:" + context.relativeUrl + " " + expr
            userService.call(_.search.issues(q)).map(d => DataWithHeaders(d.data.items, d.headers))
          case _ =>
            Future.failed(new UnsupportedOperationException("Bad search query"))
        }
    }
  }

  //noinspection ScalaUnusedSymbol
  private def pageArticles(context: ContextModel, token: String, link: String): Future[DataWithHeaders[Seq[Issue]]] = {
    // page may be a search or a plain issue list
    if (!model.subProp(_.useSearch).get) {
      githubRestApiClient.requestWithHeaders[Seq[Issue]](link, token)
    } else {
      githubRestApiClient.requestWithHeaders[SearchResultIssues](link, token, Seq("Accept" -> "application/vnd.github.v3.text-match+json"))
        .map(d => DataWithHeaders(d.data.items, d.headers))
    }
  }

  private def localZoneId: ZoneId = {
    // it seems ZoneId.systemDefault is not implemented properly, we provide our own implementation
    ZoneId.of(new DateTimeFormatX().resolvedOptions().timeZone.getOrElse("Etc/GMT"))
  }

  private val FullCommentHeader = "> \\*+[A-Za-z0-9_]+\\*+ _([0-9]+)\\. ?([0-9]+)\\. ?([0-9]+) ([0-9]+):([0-9]+):([0-9]+)_ \\*+Tags:\\*+ .*".r
  private val DotNoteHeader = "> \\*+[A-Za-z0-9_]+\\** _*([0-9]+)\\. ?([0-9]+)\\. ?([0-9]+) ([0-9]+):([0-9]+):([0-9]+)_*\\**".r
  private val SlashNoteHeader = "> \\*+[A-Za-z0-9_]+\\** _*([0-9]+)/([0-9]+)/([0-9]+) ([0-9]+):([0-9]+):([0-9]+)_*\\**".r

  def removeColaboHeaders(body: String): String = {
    val bodyLines = body.linesIterator.toSeq
    val linesWithoutHeaders = bodyLines.take(2).map {
      case FullCommentHeader(_*) => None
      case DotNoteHeader(_*) => None
      case SlashNoteHeader(_*) => None
      case x =>
        Some(x)
    } match {
      case Seq(Some(Title()), None) => bodyLines.drop(2)// second line is a timestamp, first line is a title - drop both
      case Seq(None, _) => bodyLines.drop(1) // first line is a timestamp, drop it
      case _ => bodyLines
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
      case _ =>
        None
    }.headOption
  }

  private def overrideCreatedAt(body: String): Option[ZonedDateTime] = {
    extractQuoteHeader(body).orElse(extractCommentNoteHeader(body))
  }

  private def rowFromIssue(i: Issue, context: ContextModel): ArticleRowModel = {
    val p = ArticleIdModel(context.organization, context.repository, i.number, None)
    val explicitCreated = overrideCreatedAt(i.body)

    // when updated_at is much newer than created_at, it means it is a real timestamp of some update (edit or comment)
    // because import never takes that long
    val updatedAt = if (ChronoUnit.HOURS.between(i.created_at, i.updated_at) > 24) {
      i.updated_at
    } else {
      explicitCreated.getOrElse(i.updated_at)
    }

    ArticleRowModel(
      p, 0, i.comments > 0, true, 0, i.title, i.body, i.state == "closed", i.labels.toSeq, i.assignees.toSeq, Option(i.milestone).map(_.title), i.user, i,
      explicitCreated.getOrElse(i.created_at), explicitCreated.getOrElse(i.created_at), updatedAt
    )
  }


  private def rowFromComment(articleId: ArticleIdModel, replyNumber: Int, i: Comment, parent: Issue): ArticleRowModel = {
    val explicitCreated = overrideCreatedAt(i.body)
    val highlight = Set.empty[String]
    ArticleRowModel(
      articleId, replyNumber, false, false, 0, bodyAbstract(i.body), i.body, false, Seq.empty, Seq.empty, None, i.user, parent,
      explicitCreated.getOrElse(i.created_at), explicitCreated.getOrElse(i.updated_at), explicitCreated.getOrElse(i.updated_at)
    )
  }

  private def findByQuote(quote: String, findIn: List[ArticleRowModel]): List[ArticleRowModel] = {
    findIn.filter { i =>
      val withoutQuotes = i.body.linesIterator.removeQuotes
      withoutQuotes.exists(_.contains(quote))
    }
  }

  private def processIssueComments(issue: ArticleRowModel, comments: Seq[ArticleRowModel], token: String, context: ContextModel, filter: Filter): Unit = { // the comments

    if (!loadStillWanted(token, context, filter)) {
      println(s"Discard pending issue for $context, filter $filter, current $pageContexts, filter ${filterState()}")
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
            val mostQuoted = byQuote.flatten.groupBy(identity).view.mapValues(_.size).maxBy(_._2)._1
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
    val childrenOf = processLast(fromEnd.toList, Nil).groupBy(_._1).view.mapValues(_.map(_._2))
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

    // TODO: we may reorder the issue once its comments are processed
    val a = model.subSeq(_.articles)
    val (before, issueAndAfter) = a.get.span(!_.id.sameIssue(issue.id))
    if (issueAndAfter.isEmpty) { // a new issue
      //println(s"Insert issue at top: ${issue.id}")
      a.replace(0, 0, hierarchyWithComments:_*)
    } else { // replace the one we have loaded
      val after = issueAndAfter.dropWhile(_.id.sameIssue(issue.id))
      //println(s"Replace issue at ${before.length}: ${issue.id}")
      a.replace(before.length, issueAndAfter.length - after.length, hierarchyWithComments:_*)
    }
  }

  private def focusElement(toFocus: String) = {
    jQ(dom.document).find(toFocus).trigger("focus")
  }

  def focusEditText(): Unit = {
    focusElement("#edit-text-area")
      .prop("selectionEnd", 0)
      .scrollTop(0)
  }

  def focusEdit(): Unit = {
    if (model.subProp(_.selectedArticleId).get.isEmpty) {
      focusEditTitle()
    } else {
      focusEditText()
    }
  }

  def focusEditTitle(): Unit = {
    focusElement("#edit-title")
  }


  def clearAllArticles(): Unit = {
    val issues = model.subSeq(_.articles).size
    model.subSeq(_.articles).tap { a =>
      a.replace(0, a.get.length)
    }
    pagingUrls.clear()
    println(s"clearAllArticles: cleared $issues, now ${model.subSeq(_.articles).size}")
  }

  def obtainCollaborators(context: ContextModel): Unit = {
    userService.call(api => api.repos(context.organization, context.repository).collaborators()).onComplete {
      case Success(users) =>
        model.subProp(_.selectedContextCollaborators).set(users)
      case Failure(ex@HttpErrorException(code, _, _)) =>
        println(s"HTTP Error $code loading collaborators from ${context.relativeUrl}: $ex")
        model.subProp(_.selectedContextCollaborators).set(Seq.empty)
      case Failure(ex) =>
        println(s"Error loading collaborators from ${context.relativeUrl}: $ex")
        ex.printStackTrace()
        model.subProp(_.selectedContextCollaborators).set(Seq.empty)
    }
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


  var loadInProgress = mutable.Set.empty[Product]

  private def loadStillWanted(token: String, context: ContextModel, filter: Filter): Boolean = {
    token == currentToken() &&
    pageContexts.contains(context) &&
    filterState() == filter
  }

  def loadIssueComments(id: ArticleIdModel, token: String, filter: Filter, issue: Issue): Future[Unit] = {
    userService.call { api =>
      val context = id.context
      val apiDone = Promise[Unit]()
      // get current row data and change them
      val oldIssue = model.subSeq(_.articles).get.find(_.id == id).get

      val issue = oldIssue.copy(hasChildren = false, preview = false)

      def processComments(done: Seq[Comment], resp: DataWithHeaders.Headers): Unit = {

        resp.paging.get("next") match {
          case Some(next) =>
            githubRestApiClient.requestWithHeaders[Seq[Comment]](next, token).map { c =>
              processComments(done ++ c.data, c.headers)
            }.failed.foreach(apiDone.failure)
          case None =>
            val commentRows = done.zipWithIndex.map { case (c, i) =>
              rowFromComment(
                ArticleIdModel(context.organization, context.repository, id.issueNumber, Some(c.id)),
                i,
                c, issue.rawParent
              )
            }
            processIssueComments(issue, commentRows, token, context, filter)
            apiDone.success(())
        }

      }

      api.repos(context.organization, context.repository).issuesAPI(id.issueNumber).comments().map(c => processComments(c.data, c.headers)).failed.foreach(apiDone.failure)

      apiDone.future
    }.tap(_.failed.foreach(_.printStackTrace()))

  }

  def insertIssues(preview: Seq[ArticleRowModel]): Unit = {
    model.subSeq(_.articles).tap { as =>
      //println(s"Insert articles at ${a.size}")
      for (i <- preview) { // insert the issues one by one, each at the suitable location
        import common.Util._
        // if the issue already exists, remove it first, we will update it
        val existingLocationStart = as.get.indexWhere(a => a.id.sameIssue(i.id))

        if (existingLocationStart >= 0) {
          val existingLocationEnd = as.get.indexWhere(a => a.id.sameIssue(i.id), existingLocationStart)
          println(s"Remove existing issue $i from $existingLocationStart..$existingLocationEnd")
          as.replace(existingLocationStart, existingLocationEnd + 1 - existingLocationStart)
        }
        // find an article which is not newer then we are, insert before it
        // we must insert only above a top-level article (issue, not a comment)
        val insertLocation = as.get.indexWhere(a => a.id.id.isEmpty && a.updatedAt <= i.updatedAt)
        //println(s"Insert location for ${i.id} $insertLocation of ${as.size}")
        as.replace(if (insertLocation >= 0) insertLocation else as.size, 0, i)
      }
    }
  }

  def loadArticlesPage(token: String, context: ContextModel, mode: String, filter: Filter): Unit = {
    val loadId = (token, context, mode, filter)
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
        pagingUrls.remove(context)
        initArticles(context, filter).tap(_.onComplete {
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

      // verify the result is still wanted

      loadInProgress -= loadId

      if (loadStillWanted(token, context, filter)) {

        println(s"loadArticlesPage $context: Issues present ${model.subSeq(_.articles).size}")


        pagingUrls += context -> issuesWithHeaders.headers.paging

        val is = issuesWithHeaders.data

        val issuesOrdered = is.sortBy(_.updated_at).reverse


        // preview the issues
        val preview = issuesOrdered.map(rowFromIssue(_, context))

        //println(s"Loaded issues ${preview.map(_.id)}")
        insertIssues(preview)
        model.subProp(_.loading).set(false)

        if (true) {
          val loadMaxComments = 10
          val selectedId = model.subProp(_.selectedArticleId).get
          def wantIssue(i: Issue, r: ArticleRowModel) = {
            // force expansion for the selected issue - important for the state (URL) change
            i.comments > 0 && (i.comments <= loadMaxComments ||  selectedId.exists(r.id.sameIssue))
          }
          val issueFutures = (issuesOrdered zip preview).filter(c => wantIssue(c._1, c._2)).map { case (i, row) => // parent issue
            loadIssueComments(row.id, token, filter, i)
          }

          //Future.sequence(issueFutures).onComplete(_ => updateRateLimits())
        } else {
          //updateRateLimits()
        }
      }

    }

  }

  def clearNotifications(): Unit = {
    println("clearNotifications")
    clearScheduled()
    lastNotifications = None
    issuesPending = mutable.Set.empty
    commentsPending = mutable.Set.empty
  }

  private def clearScheduled(): Unit = {
    scheduled.foreach(clearTimeout)
    scheduled = None
  }

  def loadNotifications(token: String): Unit  = {
    val logging = true
    val filter = filterState()

    clearScheduled()

    val defaultInterval = 60
    def scheduleNext(sec: Int): Unit = {
      if (logging) println(s"scheduleNext $sec")
      scheduled = Some(setTimeout(sec.seconds) {
        scheduled = None
        loadNotifications(token)
      })
    }
    println(s"Load notifications since $lastNotifications")
    userService.call(_.notifications.get(ifModifiedSince = lastNotifications.orNull, all = false)).map { notifications =>
      // TODO:  we need paging if there are many notifications
      if (logging) println(s"Notifications ${notifications.data.size} headers ${notifications.headers}")

      val newUnreadData = notifications.data.filter(_.unread).filter(_.subject.`type` == "Issue").flatMap{ n =>
        //println(s"Unread ${n.subject}")
        //println(s"  last_read_at ${n.last_read_at}, updated_at: ${n.updated_at}")
        // URL is like: https://api.github.com/repos/gamatron/colabo/issues/26
        val NotificationSource = ".*/repos/(.+)/(.+)/issues/([0-9]+)".r
        val issueId = n.subject.url match {
          case NotificationSource(owner, repo, number) =>
            Some((ContextModel(owner, repo), number.toLong))
          case _ =>
            None
        }

        // comment URL is like: https://api.github.com/repos/stravissimo/cohubo-test/issues/comments/629224546
        // a proper way would be to download the article from the URL instead of parsing it
        // an alternative would be do download all "new" articles (since the most recent one known)
        val NotificationComment = ".*/repos/(.+)/(.+)/issues/comments/([0-9]+)".r
        val commentId = n.subject.latest_comment_url match {
          case NotificationComment(owner, repo, number) =>
            Some((ContextModel(owner, repo), number.toLong))
          case _ =>
            None
        }


        // TODO: it might be better to delay processing of first notifications after the first article list is loaded
        // check if we known the issue / comment, if not, make sure we download it
        for {
          id <- issueId
          if pageContexts.contains(ContextModel(n.repository.owner.login, n.repository.name))
        } {
          val as = model.subSeq(_.articles).get
          val issueKnown = as.find(a => a.id.issueNumber == id._2 && a.id.context == id._1)
          val commentKnown = commentId.exists(cid => as.exists(a => a.id.id.contains(cid) && a.id.context == cid._1))
          if (issueKnown.isEmpty) {
            println(s"Unknown issue $issueId")

            if (!issuesPending.contains(id)) {
              println(s"  Download issue $id, pending $issuesPending")
              issuesPending += id

              // do not download
              userService.call { u =>
                u.repos(id._1.organization, id._1.repository).issuesAPI(id._2).get
              }.foreach { i =>
                println(s"  Downloaded issue $id")
                issuesPending -= id
                val row = rowFromIssue(i, ContextModel(id._1.organization, id._1.repository))
                insertIssues(Seq(row))
                // TODO: insert the issue
                assert(i.number == id._2)
                val aid = ArticleIdModel(id._1.organization, id._1.repository, i.number, None)
                loadIssueComments(aid, token, filter, i)
              }
            }
          } else if (!commentKnown) {
            println(s"Unknown comment $commentId for issue $issueId")
            if (!commentsPending.contains(id) && !issuesPending.contains(id)) {
              commentsPending += id
              println(s"  Download comments for $id")
              val filter = filterState()
              val aid = ArticleIdModel(id._1.organization, id._1.repository, id._2, None)
              loadIssueComments(aid, token, filter, issueKnown.get.rawParent).foreach(_ => commentsPending -= id)
            }
          }
        }

        issueId.map(_ -> UnreadInfo(n.updated_at, n.last_read_at, n.url))

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


  private def doLoadArticles(token: String, context: ContextModel, filter: Filter): Unit = {
    println(s"Load articles $context $token filter = $filter")
    loadArticlesPage(token, context, "init", filter = filter)
  }

  def unselectedArticle(): Unit = {
    model.subProp(_.selectedArticle).set(None)
    model.subProp(_.selectedArticleId).set(None)
    model.subProp(_.selectedArticleParent).set(None)
  }

  def init(): Unit = {
    // load the settings before installing the handler
    // otherwise both handlers are called, which makes things confusing
    props.set(SettingsModel.load)
    // install the handler
    println(s"Install loadArticles handlers, token ${currentToken()}")
    props.subProp(_.token).listen { token =>
      model.subProp(_.loading).set(true)
      unselectedArticle()
      println(s"Token changed to $token, contexts: ${props.subSeq(_.contexts).size}")
      clearAllArticles()
      if (token != null) {
        val state = filterState()
        for (context <- props.get.activeContexts) {
          doLoadArticles(token, context, state)
        }
      }
    }

    (props.subProp(_.contexts) ** props.subProp(_.selectedContext) ** model.subProp(_.filterExpression) ).listen { case ((cs, act), filter) =>
      val ac = act.orElse(cs.headOption)
      // it seems listenStructure handler is called before the table is displayed, listen is not
      // update short names
      val contexts = props.subSeq(_.contexts).get

      println(s"activeContexts $ac")

      val names = contexts.toSeq.map(c => Seq(c.organization, c.repository))
      val shortNames = ShortIds.compute(names)
      shortRepoIds = (contexts zip shortNames).toMap

      val token = currentToken()
      // completely empty - we can do much simpler cleanup (and shutdown any periodic handlers)
      clearAllArticles()
      unselectedArticle()


      if (act.isEmpty) {
        clearNotifications()
      }

      // load labels
      for (context <- ac) {
        userService.call(api => api.repos(context.organization, context.repository).labels()).onComplete {
          case Success(value) =>
            model.subProp(_.labels).set(value)
          case Failure(ex) =>
            printf(s"Error loading labels: $ex")
            model.subProp(_.labels).set(Seq.empty)
        }
        obtainCollaborators(context)
      }

      val state = filterState()
      ac.filter(_.valid).foreach(doLoadArticles(token, _, state))

      // we currently always remember all notifications
      // this could change if is shows there is too many of them - we could remember only the ones for the repositories we handle
      if (scheduled.isEmpty && act.nonEmpty) {
        loadNotifications(token)
      }
      SettingsModel.store(props.get)
      contextChangedCallback.foreach(_())
      contextChangedCallback = None
    }
    // handlers installed, execute them
    // do not touch token, that would initiate another login
    model.subProp(_.loading).set(true)
    unselectedArticle()
    props.subSeq(_.contexts).touch()

  }


  override def handleState(state: SelectPageState): Unit = {
    println(s"handleState ${state.id}")

    val contextSwitch = for {
      ctx <- state.id.map(_.context)
      if (!pageContexts.contains(ctx))
    } yield {
      println(s"Switch context to $ctx")
      // TODO: if the context is not listed in contexts, add it
      ctx -> userService.properties.subProp(_.selectedContext)
    }

    contextSwitch.map { case (ctx, s) =>
      // will this callback be executed last?
      contextChangedCallback = Some { () =>
        model.subProp(_.selectedArticleId).set(state.id)
        //println(s"contextSwitch callback $ctx")
      }
      s.set(Some(ctx))
    }.getOrElse {
      model.subProp(_.selectedArticleId).set(state.id)
    }
  }

  def loadMore(): Unit = {
    // TODO: be smart, decide which repositories need more issues
    val token = currentToken()
    for (context <- pageContexts) {
      loadArticlesPage(token, context, "next", filter = filterState()) // state should not matter for next page
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

  def adjustLinks(html: String): String = {
    // adjust github issue links to Cohubo ones
    val pageUrl = dom.window.location.href
    val baseUrl = pageUrl.takeWhile(_ != '#') + "#/"
    val IssueUrl = """(?s)(.*href=")https://[^"]*github.com/([^"]*/issues/[^"]+)(".*)""".r
    IssueUrl.replaceAllIn(html, { r =>
      val prefix = r.group(1)
      val issue = r.group(2)
      val postfix = r.group(3)
      // verify issue is something we support
      if (ArticleIdModel.parse(issue).nonEmpty) {
        prefix + baseUrl + issue + postfix
      } else {
        r.group(0)
      }
    })
  }

  def renderMarkdown(body: String, context: ContextModel, postprocess: String => String = identity): Unit = {
    val selectedId = model.subProp(_.selectedArticleId).get
    //println(s"renderMarkdown $selectedId")
    val strippedBody = removeColaboHeaders(body)
    val htmlResult = markdownCache(strippedBody -> context)
    // update the local data: article display and article content in the article table
    htmlResult.map { html =>
      // before setting the value make sure the article is still selected
      // if the selection has changed while the Future was flying, ignore the result
      if (model.subProp(_.selectedArticleId).get.exists(selectedId.contains)) {
        model.subProp(_.articleContent).set(postprocess(adjustLinks(html)))
      }
    }.failed.foreach { ex =>
      markdownCache.remove(strippedBody -> context) // avoid caching failed requests
      model.subProp(_.articleContent).set(s"Markdown error $ex")
    }
  }

  def editCancel(): Unit = {
    model.subProp(_.editing).set((false, false))
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

  def wasEditing(): Boolean = model.subProp(_.editing).get._1

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
        case ArticleIdModel(_, _, issueId, Some(commentId)) =>
          api.repos(context.organization, context.repository).editComment(commentId, body).map(_.body)
        case ArticleIdModel(_, _, issueId, None) =>
          val issueAPI = api.repos(context.organization, context.repository).issuesAPI(issueId)
          issueAPI.get.flatMap { i =>
            issueAPI.update(
              i.title,
              body,
              i.state,
              Option(i.milestone).map(_.number).getOrElse(-1),
              i.labels.toSeq.map(_.name),
              i.assignees.toSeq.map(_.login)
            )
          }.map(_.body)
      }
    }.onComplete {
      case Failure(ex) =>
        println(s"Edit failure $ex")
      case Success(body) =>
        model.subProp(_.editing).set((false, false))
        renderMarkdown(body, context)
        model.subSeq(_.articles).tap { as =>
          val index = as.get.indexWhere(_.id == selectedId)
          val a = as.get(index)
          as.replace(index, 1, a.copy(body = body))
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
          comments = articles.filter(a => a.id.sameIssue(selectedId) && a.id.id.nonEmpty)
        } {
          val parent = model.subProp(_.articles).get.find(_.id == selectedId)
          val newId = ArticleIdModel(context.organization, context.repository, selectedId.issueNumber, Some(c.id))
          val newRow = rowFromComment(newId, comments.size, c, parent.get.rawParent)
          processIssueComments(i, comments :+ newRow, currentToken(), context, filterState())
        }
      }
    }.onComplete {
      case Failure(ex) =>
        println(s"Reply failure $ex")
      case Success(_) =>
        model.subProp(_.editing).set((false, false))
    }
  }

  def newIssueDone(title: String, body: String): Unit = {
    // there should be always one repository selected
    for (context <- pageContexts.headOption) { // remember the context across the futures, so that we can verify it has not changed
      userService.call { api =>
        api.repos(context.organization, context.repository).createIssue(
          title,
          body
        )
      }.onComplete {
        case Failure(ex) =>
          println(s"New issue failure $ex")
        case Success(s) =>
          val newRow = rowFromIssue(s, context)
          processIssueComments(newRow, Seq.empty, currentToken(), context, filterState())
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
        newIssueDone(model.subProp(_.editedArticleTitle).get, body)
    }
  }

  def markAsRead(id: ArticleIdModel): Unit = {
    val unreadInfo = model.subProp(_.unreadInfo).get
    for (unread <- unreadInfo.get(id.context -> id.issueNumber)) {
      println(s"markAsRead $id, unread $unread")
      githubRestApiClient.request[Unit](method = Method.PATCH, uri = unread.threadURL, token = currentToken()).map{_ =>
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
      val replies = model.subProp(_.articles).get.filter(_.id.sameIssue(id))
      // there always must exists at least the reply we are replying to, it does not have to be a comment, though
      val maxReplyNumber = replies.flatMap(_.id.id).maxOption

      val isLast = (id.id, maxReplyNumber) match {
        case (Some(iid), Some(r)) if iid == r =>
          true
        case (None, None) =>
          true
        case _ =>
          false
      }

      val quote = if (!isLast) {
        val replyTo = replies.find(_.id == id).get
        "> " + quoteFirstLine(replyTo.body) + "\n\n"
      } else ""

      println(s"Reply to $id")
      model.subProp(_.editing).set((true, true))
      model.subProp(_.selectedArticleId).set(Some(id))
      model.subProp(_.editedArticleMarkdown).set(quote)
      model.subProp(_.editedArticleHTML).set("")
      focusEdit()
    }
  }

  private def parentArticle(ar: ArticleRowModel): ArticleRowModel = {
    val a = model.subSeq(_.articles)
    val as = a.get
    val parentId = ar.id.copy(issueNumber = ar.rawParent.number, id = None)
    val parentI = as.indexWhere(_.id.sameIssue(parentId))
    as(parentI)
  }

  private def changeLabels(data: ArticleIdModel, f: Seq[String] => Seq[String]): Unit = {
    val a = model.subSeq(_.articles)
    val as = a.get
    as.find(_.id == data).foreach { ar =>
      // beware: the parent article may have changed meanwhile
      val i = parentArticle(ar)
      userService.call(
        _.repos(ar.id.context.organization, ar.id.context.repository).issuesAPI(ar.id.issueNumber).update(
          labels = f(i.labels.map(_.name))
        )
      ).foreach { i =>
        val before = as.indexWhere(_.id.sameIssue(ar.id))
        if (before >= 0) {
          val newIssue = rowFromIssue(i, ar.id.context)
          a.replace(before, 1, ar.copy(labels = newIssue.labels))

        }
      }
    }
  }

  private def changeAssignees(data: ArticleIdModel, f: Seq[String] => Seq[String]): Unit = {
    val a = model.subSeq(_.articles)
    val as = a.get
    as.find(_.id == data).foreach { ar =>
      val i = parentArticle(ar)
      userService.call(
        _.repos(ar.id.context.organization, ar.id.context.repository).issuesAPI(ar.id.issueNumber).update(
          assignees = f(i.assignees.map(_.login))
        )
      ).foreach { i =>
        println(s"Changed assign on $i")
        val before = as.indexWhere(_.id.sameIssue(ar.id))
        if (before >= 0) {
          val newIssue = rowFromIssue(i, ar.id.context)
          a.replace(before, 1, ar.copy(assignees = newIssue.assignees))
        }
      }
    }
  }

  def removeLabel(data: ArticleIdModel, name: String): Unit = {
    changeLabels(data, _ diff Seq(name))
  }

  def addLabel(data: ArticleIdModel, name: String): Unit = {
    changeLabels(data, name +: _)
  }

  def removeAssignee(data: ArticleIdModel, name: String): Unit = {
    changeAssignees(data, _ diff Seq(name))
  }

  def addAssignee(data: ArticleIdModel, name: String): Unit = {
    changeAssignees(data, name +: _)
  }

  def newIssue(): Unit = {
    if (!wasEditing()) {
      model.subProp(_.editing).set((true, true))
      model.subProp(_.selectedArticleId).set(None)
      model.subProp(_.editedArticleTitle).set("")
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
          i.labels.toSeq.map(_.name),
          i.assignees.toSeq.map(_.login)
        )
      }.map(_.body)
    }.onComplete {
      case Success(_) =>
        val a = model.subSeq(_.articles)
        val as = a.get
        val before = as.indexWhere(_.id.sameIssue(id))
        val after = as.indexWhere(!_.id.sameIssue(id), before)
        if (!model.subProp(_.filterClosed).get) {
          // by default we do not display closed issues - the default reaction is to remove the one we have closed
          // instead of removing it we could only mark it as closed, that would be less disruptive
          a.replace(before, after - before)
        } else {
          // closed issues displayed - only mark
          for (index <- before until after) {
            val old = as(index)
            a.replace(index, 1, old.copy(closed = true))
          }
        }

      case Failure(ex) =>
        println(s"Error closing #${id.issueNumber}: $ex")
    }

  }


  def gotoSettings(): Unit = {
    application.goTo(SettingsPageState)
  }

}
