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

case class IssuesWithHeaders(issues: Seq[Issue], paging: Map[String, String])

object IssuesWithHeaders {

  def linkHeaders(linkHeader: String): Map[String, String] = {
    // https://developer.github.com/v3/guides/traversing-with-pagination/
    /*
    Link: <https://api.github.com/search/code?q=addClass+user%3Amozilla&per_page=50&page=2>; rel="next",
    <https://api.github.com/search/code?q=addClass+user%3Amozilla&per_page=50&page=20>; rel="last"
    */
    val extract = """<([^>]*)>; *rel="([^"]*)"""".r
    extract.findAllMatchIn(linkHeader).map { m =>
      m.group(2) -> m.group(1)
    }.toMap
  }
  def fromString(text: String, linkHeader: String): IssuesWithHeaders = {
    val codec = implicitly[GenCodec[Seq[Issue]]]
    val input = new JsonStringInput(new JsonReader(text))
    val issues = codec.read(input)

    IssuesWithHeaders(issues, linkHeaders(linkHeader))
  }


  implicit def asResponse(implicit fromBody: AsReal[HttpBody, Seq[Issue]]): AsReal[RestResponse, IssuesWithHeaders] = AsReal.create {
    resp =>
      IssuesWithHeaders(fromBody.asReal(resp.body), linkHeaders(resp.headers("Link").value))
  }
  // note: if OpenAPI is required, we should implement restResponses
}


trait RepositoryAPI {
  @GET
  def issues(state: String = "open", @transientDefault creator: String = null): Future[IssuesWithHeaders]

  @Prefix("issues")
  def issuesAPI(number: Long): IssuesAPI
}

object RepositoryAPI extends RestClientApiCompanion[EnhancedRestImplicits,RepositoryAPI](EnhancedRestImplicits)
