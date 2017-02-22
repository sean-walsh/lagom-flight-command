package com.reactiveair.flight.api

import java.util.UUID

import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import FlightServiceApi._
import akka.NotUsed

trait FlightService extends Service {

  /**
    * Adds a new Flight available for adding passengers.
    * Example: curl -H "Content-Type: application/json" -X POST -d '{"message":
    * "Hi"}' http://localhost:9000/api/create-customer
    */
  def addFlight: ServiceCall[FlightDto, String]

  /**
    * Add a passenger to a flight.
    */
  def addPassenger: ServiceCall[PassengerDto, String]

  /**
    * Select a seat.
    */
  def selectSeat: ServiceCall[SelectSeatDto, String]

  /**
    * Removes a passenger from a flight.
    */
  def removePassenger(flightId: UUID, passengerId: UUID): ServiceCall[NotUsed, String]

  /**
    * Close a flight due to cancellation or doors closed for takeoff.
    */
  def closeFlight(flightId: UUID): ServiceCall[NotUsed, String]

  def descriptor = {
    import Service._
    import com.lightbend.lagom.scaladsl.api.transport.Method
    named("flights").withCalls(
      restCall(Method.POST,   "/flights/add-flight", addFlight _),
      restCall(Method.POST,   "/flights/add-passenger", addPassenger _),
      restCall(Method.POST,   "/flights/select-seat", selectSeat _),
      restCall(Method.POST,   "/flights/remove-passenger/flight-id/:flightId/passengerId/:passengerId", removePassenger _),
      restCall(Method.POST,   "/flights/close-flight/flight-id/:flightId", closeFlight _)
    ).withAutoAcl(true)
  }
}

object FlightServiceApi {

  case class PassengerDto(
    flightId: UUID,
    lastName: String,
    firstName: String,
    initial: String,
    seatAssignment: Option[String])

  case class FlightDto(
    callsign: String,
    equipment: String,
    departureIata: String,
    arrivalIata: String,
    departureTime: DateTime,
    arrivalTime: DateTime)

  case class SelectSeatDto(flightId: UUID, passengerId: UUID, seatAssignment: String)

  implicit val addFlightFormat: Format[FlightDto] = Json.format[FlightDto]
  implicit val addPassengerFormat: Format[PassengerDto] = Json.format[PassengerDto]
  implicit val selectSeatFormat: Format[SelectSeatDto] = Json.format[SelectSeatDto]
}
