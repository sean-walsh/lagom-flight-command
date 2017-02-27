package com.reactiveair.flight.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import FlightState._
import FlightCommand._

/**
  * Created by sean on 2/22/17.
  */
object FlightSerializationRegistry extends JsonSerializerRegistry {

  override val serializers = Vector(
    JsonSerializer[FlightInfo],
    JsonSerializer[FlightState],
    JsonSerializer[Passenger],
    JsonSerializer[AddFlight],
    JsonSerializer[AddPassenger],
    JsonSerializer[SelectSeat],
    JsonSerializer[RemovePassenger],
    JsonSerializer[CloseFlight.type],
    JsonSerializer[FlightAdded],
    JsonSerializer[PassengerAdded],
    JsonSerializer[SeatSelected],
    JsonSerializer[PassengerRemoved],
    JsonSerializer[FlightClosed],
    JsonSerializer[AddFlightReply]
  )
}
