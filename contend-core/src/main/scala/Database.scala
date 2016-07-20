package org.contend.core

import com.typesafe.config.Config
import org.neo4j.driver.v1.{AuthTokens, GraphDatabase, StatementResult}

import scala.collection.JavaConverters._
import scala.util.Try

object Database {
}

class Database()(implicit config: Config) {
  private val connectionString = s"bolt://${config.getString("neo4j.url")}"
  private val username = config.getString("neo4j.username")
  private val password = config.getString("neo4j.password")

  private val driver = GraphDatabase.driver(
    connectionString,
    AuthTokens.basic(username, password)
  );

  def execute(cypher: String, parameters: Map[String, AnyRef] = Map.empty): Try[StatementResult] = Try {
    val session = driver.session()
    val results = session.run(cypher, parameters.asJava);
    session.close()
    results
  }

  def close: Unit = {
    driver.close()
  }
}