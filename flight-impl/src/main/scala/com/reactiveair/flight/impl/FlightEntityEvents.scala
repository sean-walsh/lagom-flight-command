package com.reactiveair.flight.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventShards, AggregateEventTag}
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import org.joda.time.DateTime

object FlightEvent {
  val NumShards = 20
  val Tag = AggregateEventTag.sharded[FlightEvent](NumShards)

  import play.api.libs.json._
  private implicit val flightInfoFormat = Json.format[FlightInfo]

  val serializers = Vector(
    JsonSerializer(Json.format[FlightAdded]),
    JsonSerializer(Json.format[PassengerAdded]),
    JsonSerializer(Json.format[SeatSelected]),
    JsonSerializer(Json.format[PassengerRemoved]),
    JsonSerializer(Json.format[FlightClosed])
  )
}

sealed trait FlightEvent extends AggregateEvent[FlightEvent] {
  override def aggregateTag: AggregateEventShards[FlightEvent] = FlightEvent.Tag
}

// Events
final case class FlightAdded(flightId: String, callsign: String, equipment: String, departureIata: String, arrivalIata: String,
                             departureTime: DateTime, arrivalTime: DateTime) extends FlightEvent
final case class PassengerAdded(flightId: String, passengerId: String, lastName: String, firstName: String, initial: String,
                                seatAssignment: Option[String]) extends FlightEvent
final case class SeatSelected(flightId: String, passengerId: String, seatAssignment: String) extends FlightEvent
final case class PassengerRemoved(flightId: String, passengerId: String) extends FlightEvent
final case class FlightClosed(flightId: String) extends FlightEvent
