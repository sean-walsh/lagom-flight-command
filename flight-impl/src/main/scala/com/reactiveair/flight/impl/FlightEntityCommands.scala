package com.reactiveair.flight.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}

sealed trait FlightCommand

object FlightCommand {

  implicit val addFlightFormat: Format[AddFlight] = Json.format
  implicit val addPassengerFormat: Format[AddPassenger] = Json.format
  implicit val selectSeatFormat: Format[SelectSeat] = Json.format
  implicit val removePassengerFormat: Format[RemovePassenger] = Json.format
  implicit val closeFlightFormat: Format[CloseFlight.type] = JsonFormats.singletonFormat(CloseFlight)
  implicit val addFlightReply: Format[AddFlightReply] = Json.format
}

final case class AddFlight(callsign: String, equipment: String, departureIata: String, arrivalIata: String) extends FlightCommand with ReplyType[AddFlightReply]

final case class AddPassenger(passengerId: String, lastName: String, firstName: String, initial: String,
                              seatAssignment: Option[String]) extends FlightCommand with ReplyType[Done]

final case class SelectSeat(passengerId: String, seatAssignment: String) extends FlightCommand with ReplyType[Done]

final case class RemovePassenger(passengerId: String) extends FlightCommand with ReplyType[Done]

final case object CloseFlight extends FlightCommand with ReplyType[Done]

case class AddFlightReply(reply: String)
