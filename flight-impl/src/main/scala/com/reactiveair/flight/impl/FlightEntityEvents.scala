package com.reactiveair.flight.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag}
import play.api.libs.json.{Format, Json}

object FlightEvent {
  val NumShards = 20
  val Tag = AggregateEventTag.sharded[FlightEvent](NumShards)

  implicit val flightAddedFormat: Format[FlightAdded] = Json.format
  implicit val passengerAddedFormat: Format[PassengerAdded] = Json.format
  implicit val seatSelectedFormat: Format[SeatSelected] = Json.format
  implicit val passengerRemovedFormat: Format[PassengerRemoved] = Json.format
  implicit val flightClosedFormat: Format[FlightClosed] = Json.format
}

sealed trait FlightEvent extends AggregateEvent[FlightEvent] {
  override def aggregateTag: AggregateEventShards[FlightEvent] = FlightEvent.Tag
}

final case class FlightAdded(flightId: String, callsign: String, equipment: String, departureIata: String, arrivalIata: String) extends FlightEvent

final case class PassengerAdded(flightId: String, passengerId: String, lastName: String, firstName: String, initial: String,
                                seatAssignment: Option[String]) extends FlightEvent

final case class SeatSelected(flightId: String, passengerId: String, seatAssignment: String) extends FlightEvent

final case class PassengerRemoved(flightId: String, passengerId: String) extends FlightEvent

final case class FlightClosed(flightId: String) extends FlightEvent
