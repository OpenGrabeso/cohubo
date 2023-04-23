package com.github.opengrabeso.cohabo
package frontend
package views
package workflows

import com.avsystem.commons.Future
import com.github.opengrabeso.github.model._
import com.github.opengrabeso.github.rest.DataWithHeaders
import dataModel._
import routing._
import io.udash._
import io.udash.rest.raw.HttpErrorException

import scala.annotation.nowarn
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Failure

object PagePresenter {
  sealed trait Filter
  case class SearchFilter(expression: String) extends Filter
}

import PagePresenter._
/** Contains the business logic of this view. */
@nowarn("msg=The global execution context")
class PagePresenter(
   model: ModelProperty[PageModel],
   val userService: services.UserContextService,
   val application: Application[RoutingState]
)(implicit ec: ExecutionContext) extends Presenter[WorkflowsPageState.type] with repository_base.RepoPresenter {

  var loadInProgress = mutable.Set.empty[Product]
  val pagingUrls =  mutable.Map.empty[ContextModel, Map[String, String]]

  def init(): Unit = {
    // load the settings before installing the handler
    // otherwise both handlers are called, which makes things confusing

    // install the handler
    println(s"Install loadArticles handlers, token ${currentToken()}")
    props.subProp(_.token).listen { token =>
      model.subProp(_.loading).set(true)
      //unselectedArticle()
      println(s"Token changed to $token, contexts: ${props.subSeq(_.contexts).size}")
      clearAllWorkflows()
      if (token != null) {
        val state = filterState()
        for (context <- props.get.activeContexts) {
          doLoadWorkflows(token, context, state)
        }
      }
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

      val token = currentToken()
      // completely empty - we can do much simpler cleanup (and shutdown any periodic handlers)
      clearAllWorkflows()
      model.subProp(_.selectedRunId).set(None)

      val state = filterState()
      ac.filter(_.valid).foreach(doLoadWorkflows(token, _, state))
    }


  }

  def loadMore(): Unit = {
    // TODO: be smart, decide which repositories need more issues
    val token = currentToken()
    for (context <- pageContexts) {
      loadWorkflowsPage(token, context, "next", filter = filterState()) // state should not matter for next page
    }
  }
  def filterState(): Filter = {
    SearchFilter(model.subProp(_.filterExpression).get)
  }

  private def doLoadWorkflows(token: String, context: ContextModel, filter: Filter): Unit = {
    println(s"Load articles $context $token filter = $filter")
    loadWorkflowsPage(token, context, "init", filter = filter)
  }

  def insertIssues(preview: Seq[RunModel]): Unit = {
    model.subSeq(_.runs).tap { as =>
      //println(s"Insert articles at ${a.size}")
      // TODO: clear / merge
      as.insert(as.length, preview:_*)
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

  def loadWorkflowsPage(token: String, context: ContextModel, mode: String, filter: Filter): Unit = {
    val loadId = (token, context, mode, filter)

    if (loadInProgress.contains(loadId)) {
      return
    }
    loadInProgress += loadId

    val loadIssue: Future[DataWithHeaders[Runs]] = mode match {
      case "next" =>
        pagingUrls.get(context).flatMap(_.get(mode)).map { link =>
          println(s"Page $mode articles $context")
          pageArticles(context, token, link)
        }.getOrElse {
          Future.successful(DataWithHeaders(Runs()))
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

    loadIssue.foreach { issuesWithHeaders =>
      loadInProgress -= loadId

      pagingUrls += context -> issuesWithHeaders.headers.paging

      val is = issuesWithHeaders.data
      val issuesOrdered = is.workflow_runs.map(rowFromIssue(_, context)).sortBy(_.updated_at).reverse
      insertIssues(issuesOrdered)

    }

  }

  private def initArticles(context: ContextModel, filter: Filter): Future[DataWithHeaders[Runs]] = {
      userService.call(_
        .repos(context.organization, context.repository)
        .actions.runs()
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
