package com.reactiveair.flight.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import com.reactiveair.flight.api.FlightService
import com.reactiveair.flight.api.FlightServiceApi._

import scala.concurrent.Future

class FlightServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {

  val Callsign = "UA100"
  val Equipment = "757-800"
  val Departure = "EWR"
  val Arrival = "SFO"

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCassandra(true)
  ) { ctx =>
    new FlightApplication(ctx) with LocalServiceLocator
  }

  val client = server.serviceClient.implement[FlightService]

  override protected def afterAll() = server.stop()

  "The FlightService" should {
    "add a new flight" in {
      client.addFlight.invoke(FlightDto(Callsign, Equipment, Departure, Arrival)).map { response =>
        response.startsWith("OK") should ===(true)
      }
    }

    "add a passenger" in {
      for {
        flightId <- addFlightHelper
        response <- client.addPassenger.invoke(PassengerDto(flightId, "Walsh", "Sean", "A", Some("1A")))
      } yield {
        response.startsWith("OK") should ===(true)
      }
    }

    "remove a passenger" in {
      for {
        flightId    <- addFlightHelper
        passengerId <- addPassengerHelper(flightId)
        response    <- client.removePassenger(flightId, passengerId).invoke()
      } yield {
        response should ===("OK")
      }
    }

    "select a seat" in {
      for {
        flightId    <- addFlightHelper
        passengerId <- addPassengerHelper(flightId)
        response    <- client.selectSeat.invoke(SelectSeatDto(flightId, passengerId, "1A"))
      } yield {
        response should ===("OK")
      }
    }

    "close a flight" in {
      for {
        flightId    <- addFlightHelper
        response    <- client.closeFlight(flightId).invoke()
      } yield {
        response should ===("OK")
      }
    }
  }

  private def addFlightHelper: Future[UUID] = {
    client.addFlight.invoke(FlightDto(Callsign, Equipment, Departure, Arrival)).map { response =>
      response.startsWith("OK") should ===(true)
      UUID.fromString(response.split(":")(1))
    }
  }

  private def addPassengerHelper(flightId: UUID): Future[UUID] = {
    client.addPassenger.invoke(PassengerDto(flightId, "Walsh", "Sean", "A", Some("1A"))).map { response =>
      response.startsWith("OK") should ===(true)
      UUID.fromString(response.split(":")(1))
    }
  }
}
