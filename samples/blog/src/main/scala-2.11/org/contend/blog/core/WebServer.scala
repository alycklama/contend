package org.contend.blog.core

import akka.http.scaladsl.server.Route
import org.contend.core.routes.ArticlesRoute

object WebServer extends org.contend.core.WebServer {
  override def init(): Unit = {
    super.init()

    database.execute(
      """MERGE (r1:Route { path: "/blog/articles/welcome", template: "templates/article.scaml" })
        |MERGE (r2:Route { path: "/blog/articles/hello_world", template: "templates/article.scaml" })
        |MERGE (a1:Article { name: "Welcome", date: "2016-01-01" })
        |MERGE (a2:Article { name: "Hello world!", date: "2016-02-01" })
        |MERGE (r1)-[r1_rel:CONTENT]->(a1)
        |MERGE (r2)-[r2_rel:CONTENT]->(a2)
      """.stripMargin
    )

    // TODO : Error handling
  }

  override def staticRoutes: Route = new ArticlesRoute().routes
}