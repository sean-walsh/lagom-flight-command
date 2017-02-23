package com.reactiveair.flight.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalatest._

import scala.collection.immutable.Seq

class FLightEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with Inside {
  
  private val system = ActorSystem("flight-entity-spec",
    JsonSerializerRegistry.actorSystemSetupFor(FlightSerializationRegistry))

  val flightId = UUID.randomUUID()
  val DateTimePattern = "yyyy-MM-dd HH:mm:ss"
  val Callsign = "UA100"
  val Equipment = "757-800"
  val Departure = "EWR"
  val Arrival = "SFO"

  val Passenger1 = Passenger(UUID.randomUUID().toString, "Walsh", "Sean", "A", Some("1A"))
  val Passenger2 = Passenger(UUID.randomUUID().toString, "Smith", "John", "P", None)

  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  private def withTestDriver(block: PersistentEntityTestDriver[FlightCommand, FlightEvent, FlightState] => Unit): Unit = {
    val driver = new PersistentEntityTestDriver(system, new FlightEntity, flightId.toString)
    block(driver)
    if (driver.getAllIssues.nonEmpty) {
      driver.getAllIssues.foreach(println)
      fail("There were issues " + driver.getAllIssues.head)
    }
  }

  "The flight entity" should {
    "instantiate as a new flight" in withTestDriver { driver =>
      val outcome = addFlightHelper(driver)
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq.empty))
      outcome.events should contain only FlightAdded(flightId.toString, Callsign, Equipment, Departure, Arrival)
    }

    "add a passenger to a flight with a seat assignment" in withTestDriver { driver =>
      addFlightHelper(driver)
      val outcome = driver.run(AddPassenger(Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment))
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger1)))
      outcome.events should contain only PassengerAdded(flightId.toString, Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment)
    }

    "add a passenger to a flight with no seat assignment" in withTestDriver { driver =>
      addFlightHelper(driver)
      val outcome = driver.run(AddPassenger(Passenger2.passengerId, Passenger2.lastName, Passenger2.firstName, Passenger2.initial, Passenger2.seatAssignment))
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger2)))
      outcome.events should contain only PassengerAdded(flightId.toString, Passenger2.passengerId, Passenger2.lastName, Passenger2.firstName, Passenger2.initial, Passenger2.seatAssignment)
    }

    "select a seat" in withTestDriver { driver =>
      val SeatAssignment = "1B"
      addFlightHelper(driver)
      val outcome = driver.run(AddPassenger(Passenger2.passengerId, Passenger2.lastName, Passenger2.firstName, Passenger2.initial, Passenger2.seatAssignment))
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger2)))
      outcome.events should contain only PassengerAdded(flightId.toString, Passenger2.passengerId, Passenger2.lastName, Passenger2.firstName, Passenger2.initial, Passenger2.seatAssignment)

      val outcome2 = driver.run(SelectSeat(Passenger2.passengerId, SeatAssignment))
      outcome2.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger2.copy(seatAssignment = Some(SeatAssignment)))))
      outcome2.events should contain only SeatSelected(flightId.toString, Passenger2.passengerId, SeatAssignment)
    }

    "select a new seat" in withTestDriver { driver =>
      val SeatAssignment = "1B"
      addFlightHelper(driver)
      val outcome = driver.run(AddPassenger(Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment))
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger1)))
      outcome.events should contain only PassengerAdded(flightId.toString, Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment)

      val outcome2 = driver.run(SelectSeat(Passenger1.passengerId, SeatAssignment))
      outcome2.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger1.copy(seatAssignment = Some(SeatAssignment)))))
      outcome2.events should contain only SeatSelected(flightId.toString, Passenger1.passengerId, SeatAssignment)
    }

    "remove a passenger from open flight" in withTestDriver { driver =>
      addFlightHelper(driver)
      val outcome = driver.run(AddPassenger(Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment))
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger1)))
      outcome.events should contain only PassengerAdded(flightId.toString, Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment)

      val outcome2 = driver.run(RemovePassenger(Passenger1.passengerId))
      outcome2.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq.empty))
      outcome2.events should contain only PassengerRemoved(flightId.toString, Passenger1.passengerId)
    }

    "close a flight" in withTestDriver { driver =>
      addFlightHelper(driver)
      val outcome = driver.run(CloseFlight)
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, true)), Seq.empty))
      outcome.events should contain only FlightClosed(flightId.toString)
    }

    "remove a passenger from a closed flight" in withTestDriver { driver =>
      addFlightHelper(driver)

      val outcome = driver.run(AddPassenger(Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment))
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Seq(Passenger1)))
      outcome.events should contain only PassengerAdded(flightId.toString, Passenger1.passengerId, Passenger1.lastName, Passenger1.firstName, Passenger1.initial, Passenger1.seatAssignment)

      val outcome2 = driver.run(CloseFlight)
      outcome2.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, true)), Seq(Passenger1)))
      outcome2.events should contain only FlightClosed(flightId.toString)

      val outcome3 = driver.run(RemovePassenger(Passenger1.passengerId))
      outcome3.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, true)), Seq.empty))
      outcome3.events should contain only PassengerRemoved(flightId.toString, Passenger1.passengerId)
    }
  }

  private def addFlightHelper(driver: PersistentEntityTestDriver[FlightCommand, FlightEvent, FlightState]) =
    driver.run(AddFlight(Callsign, Equipment, Departure, Arrival))
}
