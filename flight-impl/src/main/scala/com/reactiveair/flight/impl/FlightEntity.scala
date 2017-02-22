package com.reactiveair.flight.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence._
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

/**
  * The flight persistent entity.
  */
class FlightEntity extends PersistentEntity {

  override type State = FlightState
  override type Command = FlightCommand
  override type Event = FlightEvent

  override def initialState: FlightState = FlightState.empty

  override def behavior: Behavior = {
    case state if state.flightInfo.isEmpty         => initial
    case state if state.flightInfo.get.doorsClosed => closed
    case _                                         => available
  }

  /**
    * Initial pre-creation state to handle the creation command/event.
    */
  private val initial: Actions =
    Actions()
      .onCommand[AddFlight, Done] {
        case (AddFlight(callsign, equipment, departureIata, arrivalIata), ctx, state) =>
          ctx.thenPersist(FlightAdded(entityId, callsign, equipment, departureIata, arrivalIata)) { _ =>
            // After persist is done additional side effects can be performed
            ctx.reply(Done)
          }
      }
      .onEvent {
        case (FlightAdded(flightId, callsign, equipment, departureIata, arrivalIata), state) =>
          FlightState(Some(FlightInfo(flightId, callsign, equipment, departureIata, arrivalIata, false)))
      }

  /**
    * Available flights may have passengers added, removed, seats assigned and the flight may be closed for booking.
    */
  private val available: Actions =
    Actions()
      .onCommand[AddPassenger, Done] {
        case (AddPassenger(passengerId, lastName, firstName, initial, seatAssignment), ctx, state) =>
          ctx.thenPersist(PassengerAdded(entityId, passengerId, lastName, firstName, initial, seatAssignment)) { _ =>
            ctx.reply(Done)
          }
      }
      .onEvent {
        case (PassengerAdded(_, passengerId, lastName, firstName, initial, seatAssignment), state) =>
          state.copy(passengers = state.passengers.+: (Passenger(passengerId, lastName, firstName, initial, seatAssignment)))
      }
      .onCommand[SelectSeat, Done] {
        case (SelectSeat(passengerId, seatAssignment), ctx, state) =>
          ctx.thenPersist(SeatSelected(entityId, passengerId, seatAssignment)) { _ =>
            ctx.reply(Done)
          }
      }
      .onEvent {
        case (SeatSelected(_, passengerId, seatAssignment), state) =>
          val passenger = (state.passengers.find(_.passengerId == passengerId) match {
            case Some(p) => p
            case None    => throw new Exception(s"passenger $passengerId does not exist!")
          }).copy(seatAssignment = Some(seatAssignment))
          state.copy(passengers = state.passengers.filterNot(_.passengerId == passengerId).+:(passenger))
      }
      .onCommand[RemovePassenger, Done] {
        case (RemovePassenger(passengerId), ctx, state) =>
          ctx.thenPersist(PassengerRemoved(entityId, passengerId)) { _ =>
            ctx.reply(Done)
          }
      }
      .onEvent {
        case (PassengerRemoved(_, passengerId), state) =>
          state.copy(passengers = state.passengers.filterNot(_.passengerId == passengerId))
      }
      .onCommand[CloseFlight.type, Done] {
        case (CloseFlight, ctx, state) =>
          ctx.thenPersist(FlightClosed(entityId)) { _ =>
            ctx.reply(Done)
          }
      }
      .onEvent {
        case (FlightClosed(_), state) =>
          state.copy(flightInfo = state.flightInfo.map(i => i.copy(doorsClosed = true)))
      }

  /**
    * A closed flight can have a passenger yanked off the plane.
    */
  private val closed: Actions =
    Actions()
      .onCommand[RemovePassenger, Done] {
        case (RemovePassenger(passengerId), ctx, state) =>
          ctx.thenPersist(PassengerRemoved(entityId, passengerId)) { _ =>
            ctx.reply(Done)
          }
      }
      .onEvent {
        case (PassengerRemoved(_, passengerId), state) =>
          state.copy(passengers = state.passengers.filterNot(_.passengerId == passengerId))
      }
}

final case class Passenger(
  passengerId: String,
  lastName: String,
  firstName: String,
  initial: String,
  seatAssignment: Option[String] = None)

final case class FlightInfo(
  flightId: String,
  callsign: String,
  equipment: String,
  departureIata: String,
  arrivalIata: String,
  doorsClosed: Boolean)

final case class FlightState(flightInfo: Option[FlightInfo], passengers: Seq[Passenger] = Seq.empty)

object FlightState {
  val empty = FlightState(None, Seq.empty)

  implicit val flightInfoFormat: Format[FlightInfo] = Json.format[FlightInfo]
  implicit val passengerFormat: Format[Passenger] = Json.format[Passenger]
  implicit val flightStateFormat: Format[FlightState] = Json.format[FlightState]
}
