package org.contend.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.fusesource.scalate.TemplateEngine
import org.neo4j.driver.v1.StatementResult

import scala.io.StdIn
import scala.util.{Failure, Success, Try}

trait WebServer {
  implicit val system = ActorSystem("actor-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val config = ConfigFactory.load()
  implicit val database = new Database()
  implicit val engine = new TemplateEngine

  val errorHandler: PartialFunction[Throwable, HttpEntity.Strict] = {
    case e =>
      val template = getClass().getResource("templates/error.scaml")
      val html = engine.layout(template.getFile, Map("exception" -> e))
      HttpEntity(ContentTypes.`text/html(UTF-8)`, html)
  }

  sys.addShutdownHook({
    database.close
  })

  def main(args: Array[String]) {
    val route =
      path("blog" / "articles") {
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
                val html = engine.layout(template.getFile, Map[String, StatementResult]("result" -> result))
                HttpEntity(ContentTypes.`text/html(UTF-8)`, html)
              }.recover {
                errorHandler
              }
            }
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}