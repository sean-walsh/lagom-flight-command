package com.reactiveair.flight.impl

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import org.scalatest._

class FLightEntitySpec extends WordSpec with Matchers with BeforeAndAfterAll with Inside {
  
  private val system = ActorSystem("flight-entity-spec",
    JsonSerializerRegistry.actorSystemSetupFor(FlightSerializationRegistry))

  val flightId = UUID.randomUUID()
  val DateTimePattern = "yyyy-MM-dd HH:mm:ss"
  val Callsign = "UA100"
  val Equipment = "757-800"
  val Departure = "EWR"
  val Arrival = "SFO"

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
      val outcome = driver.run(AddFlight(Callsign, Equipment, Departure, Arrival))
      outcome.state shouldBe (FlightState(Some(FlightInfo(flightId.toString, Callsign, Equipment, Departure, Arrival, false)), Nil))
      //outcome.events should contain only AuctionStarted(auction)
    }
  }
}
