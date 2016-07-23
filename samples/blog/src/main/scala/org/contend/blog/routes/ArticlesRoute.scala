package org.contend.core.routes

import java.net.URL

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.contend.core.{Database, TemplateNotFoundException}
import org.fusesource.scalate.TemplateEngine
import org.neo4j.driver.v1.{Record, Value}

import scala.util.Try

class ArticlesRoute(implicit database: Database, engine: TemplateEngine, errorHandler: PartialFunction[Throwable, Try[HttpResponse]],
                    templateHandler: PartialFunction[String, URL]) {

  def routes: Route = articles

  def articles = path("blog" / "articles") {
    get {
      complete {
        Try {
          database.execute("MATCH (a:Article) RETURN a as article ORDER BY a.date desc;")
            .map(
              records => {
                val articles = records.map(record => record.get("article"))
                val html = engine.layout(
                  templateHandler("templates/articles.scaml").getFile,
                  Map[String, Iterator[Value]]("articles" -> articles)
                )
                HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
              }
            ).recoverWith[HttpResponse] {
              errorHandler
            }.get
        }
      }
    }
  }
}
