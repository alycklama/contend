package org.contend.core

import java.io.File

import com.typesafe.config.ConfigFactory
import org.neo4j.graphdb.factory.{GraphDatabaseFactory, GraphDatabaseSettings}
import org.scalatest.FlatSpec

class DatabaseTest extends FlatSpec {
  implicit val config = ConfigFactory.load

  val graphDb = new GraphDatabaseFactory()
    .newEmbeddedDatabaseBuilder(new File("target/neo4j"))
    .setConfig("dbms.connector.bolt.type", "BOLT")
    .setConfig("dbms.connector.bolt.enabled", "true")
    .setConfig("dbms.connector.bolt.tls_level", GraphDatabaseSettings.BoltConnector.EncryptionLevel.DISABLED.toString)
    .setConfig("dbms.connector.bolt.address", "bolt://localhost:9999")
    .newGraphDatabase();

  val database = new Database()

  "Embedded Neo4j database" should "be able to connect and return the results of a cypher query" in {
    val result = database.execute("MATCH (n) RETURN n;")
    assert(result.isSuccess, result.failed)
    assert(result.get.size == 0)
  }
}
