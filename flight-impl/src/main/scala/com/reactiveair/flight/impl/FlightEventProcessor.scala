package com.reactiveair.flight.impl

import java.util.UUID

import akka.Done
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}

import scala.concurrent.{ExecutionContext, Future}

/**
  * A read side event processor for flights.
  */
class FlightEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[FlightEvent] {

  private var insertFlightStatement: PreparedStatement = null
  private var deleteFlightStatement: PreparedStatement = null

  def buildHandler =
    readSide.builder[FlightEvent]("itemEventOffset")
      .setGlobalPrepare(createTables)
      .setPrepare(_ => prepareStatements())
      .setEventHandler[FlightAdded](e => insertFlight(UUID.fromString(e.event.flightId), e.event.callsign))
      .setEventHandler[FlightClosed](e => deleteFlight(UUID.fromString(e.event.flightId)))
      .build

  private def createTables() =
    for {
      _ <- session.executeCreateTable("""
        CREATE TABLE IF NOT EXISTS activeFlights (
          flightId UUID,
          callSign text,,
          PRIMARY KEY (flightId)
        ) WITH CLUSTERING ORDER BY (flightId ASC)
      """)
    } yield Done

  private def prepareStatements() = {
    for {
      insertFlight <- session.prepare("""
        INSERT INTO activeFlights(
          flightId,
          callSign
        ) VALUES (?, ?)
      """)
      deleteFlight <- session.prepare("""
        DELETE FROM activeFlights
        WHERE flightId = ?
      """)
    } yield {
      insertFlightStatement = insertFlight
      deleteFlightStatement = deleteFlight
      Done
    }
  }

  private def insertFlight(flightId: UUID, callSign: String) = {
    Future.successful(List(
      insertFlightStatement.bind(flightId, callSign)
    ))
  }

  private def deleteFlight(flightId: UUID) = {
    Future.successful(List(
      deleteFlightStatement.bind(flightId)
    ))
  }

  def aggregateTags = FlightEvent.Tag.allTags
}
