package org.contend.core

import com.typesafe.config.Config
import org.neo4j.driver.v1.{AuthTokens, GraphDatabase, Record, Config => Neo4jConfig}

import scala.collection.JavaConverters._
import scala.util.Try

object Database {
}

class Database()(implicit config: Config) {
  private val connectionString = s"${config.getString("neo4j.dbms.connector.bolt.address")}"
  private val username = config.getString("neo4j.dbms.connector.username")
  private val password = config.getString("neo4j.dbms.connector.password")

  private val driver = GraphDatabase.driver(
    connectionString,
    AuthTokens.basic(username, password),
    Neo4jConfig.build().withEncryptionLevel(
      Neo4jConfig.EncryptionLevel.valueOf(config.getValue("neo4j.dbms.connector.bolt.tls_level").unwrapped().toString)
    ).toConfig
  );

  def execute(cypher: String, parameters: Map[String, AnyRef] = Map.empty): Try[Iterator[Record]] = Try {
    val session = driver.session()
    val results = session.run(cypher, parameters.asJava);
    session.close()
    results.asScala
  }

  def insert(cypher: String) = Try {
    val session = driver.session()
    val transaction = session.beginTransaction()

    val tryR = Try {
      val results = Some(session.run(cypher).asScala)
      transaction.success()
      results
    }.recover[Option[Iterator[Record]]] {
      case ex =>
        transaction.failure()
        None
    }.get
  }

  def close: Unit = {
    driver.close()
  }
}