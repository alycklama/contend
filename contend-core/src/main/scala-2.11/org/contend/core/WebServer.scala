package org.contend.core

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.contend.core.WebServer.{CypherQuery, TemplateLocation}
import org.fusesource.scalate.TemplateEngine
import org.neo4j.driver.internal.value.NodeValue
import org.neo4j.driver.v1.{Record, Value}

import scala.io.StdIn
import scala.util.Try

object WebServer {
  type CypherQuery = String
  type TemplateLocation = String
}

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
        val html = templateHandler("templates/error.scaml", e)
        HttpResponse(StatusCodes.InternalServerError, entity = (HttpEntity(ContentTypes.`text/html(UTF-8)`, html)))
      }
  }

  implicit val templateHandler: PartialFunction[(TemplateLocation, Any), String] = {
    case (path, content) => {
      val template = this.getClass().getClassLoader.getResource(path)
      if (template == null)
        throw new TemplateNotFoundException(s"template not found: ${path}")

      engine.layout(
        template.getFile,
        Map[String, Any]("content" -> content)
      )
    }
  }

  implicit val cypherQueryHandler: PartialFunction[(CypherQuery, TemplateLocation, Seq[String]), HttpResponse] = {
    case (cypherQuery, templateLocation, mapper) => {
      contentHandler(database.execute(cypherQuery), templateLocation, mapper)
    }
  }

  implicit val contentHandler: PartialFunction[(Try[Iterator[Record]], TemplateLocation, Seq[String]), HttpResponse] = {
    case (content, templateLocation, mapper) => {
      content
        .map(
          records => {
            // Transform a list of return values to a single tupleN
            def mapContent: (Iterator[_]) = {
              records.map(record => {
                // There is some weird behavior when returning a Tuple1, not being able to use get(key) on the object.
                // As a workaround the object itself will be returned if the mapper only has a single value
                if (mapper.size == 1) {
                  record.get(mapper.head)
                } else {
                  Tuples.toTuple(mapper.map(key => record.get(key)))
                }
              })
            }

            val html = templateHandler(templateLocation, mapContent)
            HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
          }
        ).recoverWith[HttpResponse] {
        errorHandler
      }.get
    }
  }

  sys.addShutdownHook({
    database.close
  })

  init()

//  def getRouteContent(path: Uri.Path): Option[(Record, Record)] = {
//    val query = s"""MATCH (r:Route { path: "${path.toString()}"})-[:CONTENT]->(c) RETURN c, r"""
//    database.execute(query).map(iterator => {
//      if (iterator.isEmpty) {
//        None
//      } else if (iterator.size == 2) {
//        Some(iterator.next(), iterator.next())
//      }
//      else {
//        throw new Exception("Multiple nodes")
//      }
//    }).get
//  }

//  def dynamicRoutesHandler: Flow[HttpRequest, HttpResponse, Any] = {
//    Flow.fromFunction[HttpRequest, HttpResponse](
//      request => {
//        // val path = request.uri.path.toString()
//        getRouteContent(request.uri.path)
//          .map(f => HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), s"Route: ${request.uri.path}")))
//          .recoverWith {
//            errorHandler
//          }.get
//      }
//    )
//  }

//  def dynamicRoutesHandler: Route = {
//    context => {
//      val request = context.request
//      context.complete {
//        getRouteContent(request.uri.path)
//          .map(f =>
//            f match {
//              case iterator: Iterator[_] if iterator.isEmpty =>
//                val html = templateHandler("templates/404.scaml", context)
//                val content = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), html)
//                HttpResponse(StatusCodes.NotFound, entity = content)
//              case _ =>
//                val content = HttpEntity(ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`), s"Dynamic Route: ${request.uri.path}")
//                HttpResponse(StatusCodes.OK, entity = content)
//            }
//          )
//          .recoverWith {
//            errorHandler
//          }.get
//      }
//    }
//  }

  /**
    * Override to add any required initialization
    */
  def init(): Unit = {}

  def staticRoutes: Route

  def main(args: Array[String]) {
    val route = staticRoutes

    val bindingFuture = Http().bindAndHandle(staticRoutes, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}