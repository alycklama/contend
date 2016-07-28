package org.contend.core.routes

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.contend.core.Database
import org.contend.core.WebServer.{CypherQuery, TemplateLocation}
import org.fusesource.scalate.TemplateEngine

import scala.util.Try

class ArticlesRoute(implicit database: Database, engine: TemplateEngine, errorHandler: PartialFunction[Throwable, Try[HttpResponse]],
                    templateHandler: PartialFunction[(TemplateLocation, Any), String],
                    contentHandler: PartialFunction[(CypherQuery, TemplateLocation, Seq[String]), HttpResponse]) {

  def routes: Route = articles ~ article

  def articles = path("blog" / "articles") {
    get {
      complete {
        Try {
          contentHandler(
            (
              "MATCH (a:Article)--(r:Route) RETURN a, r ORDER BY a.date desc;",
              "templates/articles.scaml", Seq("a", "r")
            )
          )
        }
      }
    }
  }

  def article = path("blog" / "articles" / Segment) {
    path =>
      get {
        context => {
          context.complete(
            Try {
              contentHandler(
                (
                  s"""MATCH (r:Route { path: "${context.request.uri.path}" })--(a:Article) RETURN a;""",
                  "templates/article.scaml", Seq("a")
                )
              )
            }
          )
        }
      }
  }
}
