package org.contend.core

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.fusesource.scalate.TemplateEngine
import org.neo4j.driver.v1.Record

import scala.io.StdIn
import scala.util.Try

trait WebServer {
  implicit val system = ActorSystem("actor-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  implicit val config = ConfigFactory.load()
  implicit val database = new Database
  implicit val engine = new TemplateEngine
  implicit val errorHandler: PartialFunction[Throwable, Try[HttpResponse]] = {
    case e =>
      Try {
        val template = this.getClass().getClassLoader.getResource("templates/error.scaml")
        val html = engine.layout(template.getFile, Map("exception" -> e))
        HttpResponse(StatusCodes.InternalServerError, entity = (HttpEntity(ContentTypes.`text/html(UTF-8)`, html)))
      }
  }

  implicit val templateHandler: PartialFunction[String, URL] = {
    case path => {
      val template = this.getClass().getClassLoader.getResource(path)
      if (template == null)
        throw new TemplateNotFoundException(s"template not found: ${path}")
      template
    }
  }

  sys.addShutdownHook({
    database.close
  })

  init()

  def getRouteContent(path: Uri.Path): Try[Iterator[Record]] = {
    val query = s"""MATCH (r:Route { path: "${path.toString()}"})-[:CONTENT]->(c) RETURN c"""
    database.execute(query)
  }

//  def routesHandler: Flow[HttpRequest, HttpResponse, Any] = {
//    Flow.fromFunction[HttpRequest, HttpResponse](
//      request => {
//        // val path = request.uri.path.toString()
//        getRouteContent(request.uri.path)
//          .map(f => HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), s"Route: ${request.uri.path}")))
//          .recover {
//            case ex =>
//              HttpResponse(StatusCodes.InternalServerError, entity = errorHandler(ex))
//          }.get
//      }
//    )
//  }

  /**
    * Override to add any required initialization
    */
  def init(): Unit = {}

  def routes: Route

  def main(args: Array[String]) {
    val route = routes

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}