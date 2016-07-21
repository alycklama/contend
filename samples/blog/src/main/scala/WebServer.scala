import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import org.neo4j.driver.v1.Record

import scala.util.{Failure, Success, Try}

object WebServer extends org.contend.core.WebServer {
  override def routes: Route = path("blog" / "articles") {
    get {
      val results = database.execute("MATCH (n) RETURN COUNT(n);")

      results match {
        case Failure(e) =>
          complete {
            Try(errorHandler(e))
          }
        case Success(result) => complete {
          Try {
            val template = getClass().getResource("templates/articles.scaml")
            val html = engine.layout(template.getFile, Map[String, Iterator[Record]]("result" -> result))
            HttpEntity(ContentTypes.`text/html(UTF-8)`, html)
          }.recover {
            errorHandler
          }
        }
      }
    }
  }
}
