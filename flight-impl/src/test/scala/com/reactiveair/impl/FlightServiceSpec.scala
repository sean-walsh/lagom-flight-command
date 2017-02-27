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
        f <- addFlightHelper
        d =  PassengerDto(f, "Walsh", "Sean", "A", Some("1A"))
        p <- client.addPassenger.invoke(d)
      } yield {
        p should ===("OK")
      }
    }
  }

  private def addFlightHelper: Future[UUID] = {
    client.addFlight.invoke(FlightDto(Callsign, Equipment, Departure, Arrival)).map { response =>
      response.startsWith("OK") should ===(true)
      UUID.fromString(response.split(":")(1))
    }
  }
}
