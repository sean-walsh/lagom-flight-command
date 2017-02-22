/**
  * Commands for Flight persistent entity.
  */
package com.reactiveair.flight.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
import org.joda.time.DateTime
import play.api.libs.json.Json

sealed trait FlightCommand {

  import JsonSerializer.emptySingletonFormat

  val serializers = Vector(
    JsonSerializer(Json.format[AddFlight]),
    JsonSerializer(Json.format[AddPassenger]),
    JsonSerializer(Json.format[RemovePassenger]),
    JsonSerializer(Json.format[SelectSeat]),
    JsonSerializer(emptySingletonFormat(CloseFlight))
  )
}

final case class AddFlight(callsign: String, equipment: String, departureIata: String, arrivalIata: String,
                           departureTime: DateTime, arrivalTime: DateTime) extends FlightCommand with ReplyType[Done]

final case class AddPassenger(passengerId: String, lastName: String, firstName: String, initial: String,
                              seatAssignment: Option[String]) extends FlightCommand with ReplyType[Done]

final case class SelectSeat(passengerId: String, seatAssignment: String) extends FlightCommand with ReplyType[Done]

final case class RemovePassenger(passengerId: String) extends FlightCommand with ReplyType[Done]

final case object CloseFlight extends FlightCommand with ReplyType[Done]
