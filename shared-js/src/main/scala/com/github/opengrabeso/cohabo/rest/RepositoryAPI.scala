package com.github.opengrabeso.cohabo
package rest

import com.avsystem.commons.rpc.{AsRawReal, AsReal}
import com.avsystem.commons.serialization.{GenCodec, Input, transientDefault}

import scala.concurrent.Future
import common.model._
import io.udash.rest._
import io.udash.rest.raw._
import Issue._
import com.avsystem.commons.serialization.json.{JsonReader, JsonStringInput}

case class IssuesWithHeaders(paging: String, issues: Seq[Issue])

object IssuesWithHeaders {
  implicit val asResponse: AsRawReal[RestResponse, IssuesWithHeaders] = AsRawReal.create(
    _ => throw new NotImplementedError(), // we are not implementing the server, no need to be able to compose the response
    { resp =>
      val codec = implicitly[GenCodec[Seq[Issue]]]
      val input = new JsonStringInput(new JsonReader(resp.body.readText()))
      val issues = codec.read(input)
      IssuesWithHeaders(resp.headers("Link").value, issues)
    }
  )
  // note: if OpenAPI is required, we should implement restResponses
}


trait RepositoryAPI {
  @GET
  def issues(state: String = "open", @transientDefault creator: String = null): Future[IssuesWithHeaders]

  @Prefix("issues")
  def issuesAPI(number: Long): IssuesAPI
}

object RepositoryAPI extends RestApiCompanion[EnhancedRestImplicits,RepositoryAPI](EnhancedRestImplicits)
