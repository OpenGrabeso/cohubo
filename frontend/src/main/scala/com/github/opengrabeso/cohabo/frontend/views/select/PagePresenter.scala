package com.github.opengrabeso.cohabo
package frontend
package views
package select

import com.github.opengrabeso.cohabo.rest.IssuesWithHeaders
import dataModel._
import common.model._
import common.Util._
import routing._
import io.udash._
import io.udash.rest.raw.HttpErrorException

import scala.concurrent.{ExecutionContext, Future}
import scala.annotation.tailrec
import scala.util.Failure

/** Contains the business logic of this view. */
class PagePresenter(
  model: ModelProperty[PageModel],
  application: Application[RoutingState],
  userService: services.UserContextService
)(implicit ec: ExecutionContext) extends Presenter[SelectPageState.type] {

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
        val context = props.organization + "/" + props.repository
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

  def removeQuotes(text: String): Iterator[String] = {
    val Mention = "(?:@[^ ]+ )+(.*)".r
    text.linesIterator.filterNot(_.startsWith(">")).map {
      case Mention(rest) =>
        rest
      case s =>
        s
    }.filterNot(_.isEmpty)
  }

  def bodyAbstract(text: String): String = {
    val dropQuotes = removeQuotes(text)
    // TODO: smarter abstracts
    dropQuotes.toSeq.head.take(80)
  }

  def extractQuotes(text: String): Seq[String] = {
    text.linesIterator.filter(_.startsWith(">")).map(_.drop(1).trim).filter(_.nonEmpty).toSeq
  }

  def repoValid(valid: Boolean): Unit = {
    model.subProp(_.repoError).set(!valid)
  }


  def initArticles(org: String, repo: String): Future[IssuesWithHeaders] = {
    userService.call(_.repos(org, repo).issues())
  }

  def loadArticlesPage(org: String, repo: String, mode: String): Unit = {
    val loadA = initArticles(org, repo)

    val load = loadA.flatMap{ isH =>
      model.subProp(_.pagingUrls).set(isH.paging)

      val is = isH.issues

      val issuesOrdered = is.sortBy(_.updated_at).reverse

      // preview the issues
      val preview = issuesOrdered.map { id =>

        val p = ArticleIdModel(org, repo, id.number, None)
        val issue = ArticleRowModel(p, id.comments > 0, true, 0, id.title, id.body, Option(id.milestone).map(_.title), id.user.displayName, id.updated_at)
        // consider adding some comments placeholder?
        issue
      }

      model.subProp(_.articles).set(preview)
      model.subProp(_.loading).set(false)

      // issue requests one by one
      // TODO: some parallel or lazy requester
      def requestNext(todo: List[Issue], done: List[(Issue, Seq[Comment])]): Future[List[(Issue, Seq[Comment])]] = {
        todo match {
          case head :: tail =>
            userService.call { api =>
              api.repos(org, repo).issuesAPI(head.number).comments.map { cs =>
                (head -> cs) :: done
              }.flatMap { d =>
                requestNext(tail, d)
              }
            }
          case _ =>
            Future.successful(done)
        }
      }
      requestNext(issuesOrdered.toList, Nil).map(_.reverse)
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
        // settings valid, store them
        SettingsModel.store(userService.properties.get)
        repoValid(true)
        x
    }

    for (issues <- load) {
      val log = false

      val allIssues = issues.flatMap { case (id, comments) =>

        val p = ArticleIdModel(org, repo, id.number, None)

        val issue = ArticleRowModel(p, false, false, 0, id.title, id.body, Option(id.milestone).map(_.title), id.user.displayName, id.updated_at)
        val issueWithComments = issue +: comments.zipWithIndex.map { case (i, index) =>
          val articleId = ArticleIdModel(org, repo, id.number, Some((index + 1, i.id)))
          ArticleRowModel(articleId, false, false, 0, bodyAbstract(i.body), i.body, None, i.user.displayName, i.updated_at)
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
              if (log) println(s"quotes ${quotes.toArray.mkString("[", ",", "]")}")
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
        if (log) println(s"childrenOf ${childrenOf.map { case (k, v) => k.id -> v.map(_.id) }}")
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

  def loadArticles(): Unit = {

    val props = userService.properties
    val sourceParameters = props.subProp(_.token).combine(props.subProp(_.organization))(_ -> _).combine(props.subProp(_.repository))(_ -> _)

    // install the handler
    sourceParameters.listen(
      { case ((token, org), repo) =>
        loadArticlesPage(org, repo, "init")
      }, initUpdate = true
    )
  }

  override def handleState(state: SelectPageState.type): Unit = {}

  def loadMore(): Unit = {

  }

  def gotoSettings(): Unit = {
    application.goTo(SettingsPageState)
  }

}
