import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import org.neo4j.driver.v1.Record

import scala.util.{Failure, Success, Try}

object WebServer extends org.contend.core.WebServer {
  override def init(): Unit = {
    super.init()

    database.execute(
      """MERGE (a1:Article { name : "Welcome", date : "2016-01-01" })
        |MERGE (a2:Article { name : "Hello world!", date : "2016-02-01" })
      """.stripMargin
    )
    // TODO : Error handling
  }

  override def routes: Route = path("blog" / "articles") {
    get {
      val results = database.execute("MATCH (a:Article) RETURN a.name as name, a.date as date ORDER BY a.date desc;")

      results match {
        case Failure(e) =>
          complete {
            Try(errorHandler(e))
          }
        case Success(articles) => complete {
          Try {
            val template = getClass().getResource("templates/articles.scaml")
            val html = engine.layout(template.getFile, Map[String, Iterator[Record]]("articles" -> articles))
            HttpEntity(ContentTypes.`text/html(UTF-8)`, html)
          }.recover {
            errorHandler
          }
        }
      }
    }
  }
}
