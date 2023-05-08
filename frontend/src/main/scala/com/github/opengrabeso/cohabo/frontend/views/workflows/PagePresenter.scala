package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import com.avsystem.commons.Future
import com.github.opengrabeso.github.model._
import com.github.opengrabeso.github.rest.DataWithHeaders
import java.time.ZonedDateTime
import dataModel._
import routing._
import io.udash._
import io.udash.rest.raw.HttpErrorException

import scala.annotation.nowarn
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Failure

object PagePresenter {

  case class StatsRow(
    total_duration: Long = 0,
    total_count: Long = 0,
    average_duration: Double = 0
  )

  case class Stats(
    count: Int,
    oldest: Option[ZonedDateTime],
    stats: Seq[(String, StatsRow)]
  )

  def fromRuns(pageModel: ModelProperty[PageModel]): ReadableProperty[Stats] = {
    pageModel.subSeq(_.runs).transform {
      runs =>
        Stats(
          count = runs.size,
          oldest = runs.view.map(_.created_at).minByOption(_.toInstant.toEpochMilli),
          stats = runs.groupBy(_.name).map { case (name, named) =>
            val total = named.map(w => (w.duration / 60.0).ceil.toLong).sum
            name -> StatsRow(
              total_duration = total,
              total_count = named.size,
              average_duration = total.toDouble / named.size
            )
          }.toSeq.sortBy(-_._2.total_duration)
        )
    }
  }

}

import PagePresenter._
/** Contains the business logic of this view. */
@nowarn("msg=The global execution context")
class PagePresenter(
   model: ModelProperty[PageModel],
   val userService: services.UserContextService,
   val application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[WorkflowsPageState.type] with repository_base.RepoPresenter {

  def refreshWorkflows(token: String): Unit = {
    clearAllWorkflows()
    if (token != null) {
      for (context <- props.get.activeContexts) {
        val now = ZonedDateTime.now()
        val days = model.subProp(_.timespan).get match {
          case "7 days" => 7
          case "30 days" => 30
          case _ => 1
        }
        val since = now.minusDays(days)
        doLoadWorkflows(token, context, since)
      }
    }
  }

  model.subProp(_.timespan).listen (_ => refreshWorkflows(userService.properties.subProp(_.token).get), false)

  def init(): Unit = {
    // load the settings before installing the handler
    // otherwise both handlers are called, which makes things confusing

    // install the handler
    println(s"Install loadArticles handlers, token ${currentToken()}")
    props.subProp(_.token).listen { token =>
      //unselectedArticle()
      println(s"Token changed to $token, contexts: ${props.subSeq(_.contexts).size}")
      refreshWorkflows(token)
    }

    // different from select - we set the value after installing the handlers, otherwise load is not triggered
    props.set(SettingsModel.load)

    val contexts = props.subSeq(_.contexts).get

    updateShortNames(contexts)


    (props.subProp(_.contexts) ** props.subProp(_.selectedContext) ** model.subProp(_.filterExpression)).listen { case ((cs, act), filter) =>
      val ac = act.orElse(cs.headOption)
      // it seems listenStructure handler is called before the table is displayed, listen is not
      // update short names
      val contexts = props.subSeq(_.contexts).get

      println(s"activeContexts $ac")

      updateShortNames(contexts)

      // completely empty - we can do much simpler cleanup (and shutdown any periodic handlers)
      clearAllWorkflows()
      model.subProp(_.selectedRunId).set(None)
    }


  }

  private def doLoadWorkflows(token: String, context: ContextModel, since: ZonedDateTime): Unit = {
    if (!model.subProp(_.loading).get) {
      println(s"Load workflows $context $token")
      model.subProp(_.loading).set(true)
      loadWorkflowsPage(token, context, since, 1)
    }
  }

  private def rowFromIssue(i: Run, context: ContextModel): RunModel = {
    RunModel(
      runId = i.id, name = i.name,
      html_url = i.html_url,
      branch = i.head_branch,
      run_started_at = i.run_started_at, created_at = i.created_at, updated_at = i.updated_at
    )
  }

  def loadWorkflowsPage(token: String, context: ContextModel, since: ZonedDateTime, page: Int): Unit = {

    val load = loadWorkflows(context, page)
      .tap(_.onComplete {
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
          updateRateLimits()
        // settings valid, store them
      })

    load.foreach { pageWithHeaders =>
      // order newest first
      val issuesOrdered = pageWithHeaders.data.workflow_runs.map(rowFromIssue(_, context)).sortBy(w => w.created_at).reverse
      val inRange = issuesOrdered.filter(_.created_at.isAfter(since))
      model.subSeq(_.runs).append(inRange: _*)

      val stats = fromRuns(model).get
      //println(s"Rows ${issuesOrdered.size}, newest ${issuesOrdered.headOption.map(_.created_at)} oldest ${issuesOrdered.lastOption.map(_.created_at)}")
      if (inRange.nonEmpty && issuesOrdered.lastOption.exists(_.created_at.isAfter(since)) && stats.count < 50_000) {
        loadWorkflowsPage(token, context, since, page + 1)
      } else {
        model.subProp(_.loading).set(false)
      }
    }
  }

  private def updateRateLimits(): Unit = {
    userService.call(_.rate_limit).foreach { limits =>
      val c = limits.resources.core
      userService.properties.subProp(_.rateLimits).set(Some(c.limit, c.remaining, c.reset))
    }
  }

  private def loadWorkflows(context: ContextModel, page: Int): Future[DataWithHeaders[Runs]] = {
      userService.call(_
        .repos(context.organization, context.repository)
        .actions.runs(page = page, per_page = 100)
      )
  }

  //noinspection ScalaUnusedSymbol
  private def pageArticles(context: ContextModel, token: String, link: String): Future[DataWithHeaders[Runs]] = {
    // page may be a search or a plain issue list
    githubRestApiClient.requestWithHeaders[Runs](link, token)
  }

  def clearAllWorkflows(): Unit = {
    val issues = model.subSeq(_.runs).size
    model.subSeq(_.runs).tap { a =>
      a.replace(0, a.get.length)
    }
    //pagingUrls.clear()
    println(s"clearAllArticles: cleared $issues, now ${model.subSeq(_.runs).size}")
  }


  /** We don't need any initialization, so it's empty. */
  override def handleState(state: WorkflowsPageState.type): Unit = {
  }
}
