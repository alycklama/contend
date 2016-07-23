package org.contend.core.routes

import java.net.URL

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.contend.core.WebServer.{CypherQuery, TemplateLocation}
import org.contend.core.{Database, TemplateNotFoundException}
import org.fusesource.scalate.TemplateEngine
import org.neo4j.driver.v1.{Record, Value}

import scala.util.Try

class ArticlesRoute(implicit database: Database, engine: TemplateEngine, errorHandler: PartialFunction[Throwable, Try[HttpResponse]],
                    templateHandler: PartialFunction[TemplateLocation, URL],
                    contentHandler: PartialFunction[(CypherQuery, TemplateLocation, Seq[String]), HttpResponse]) {

  def routes: Route = articles

  def articles = path("blog" / "articles") {
    get {
      complete {
        Try {
          contentHandler(
            (
              "MATCH (a:Article) RETURN a as article ORDER BY a.date desc;",
              "templates/articles.scaml", Seq("article")
            )
          )
        }
      }
    }
  }
}
