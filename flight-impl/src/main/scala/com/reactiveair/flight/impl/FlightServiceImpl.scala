package com.reactiveair.flight.impl

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.reactiveair.flight.api.FlightService
import com.reactiveair.flight.api.FlightServiceApi._

import scala.concurrent.ExecutionContext

/**
  * Implementation of FlightService. Persistent entity registry is injected, execution context is implicit.
  */
class FlightServiceImpl(persistentEntityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends FlightService {

  override def addFlight: ServiceCall[FlightDto, String] =
    ServiceCall { dto: FlightDto =>
      val x = newEntityRef.ask(AddFlight(dto.callsign, dto.equipment, dto.departureIata, dto.arrivalIata))
      newEntityRef.ask(AddFlight(dto.callsign, dto.equipment, dto.departureIata, dto.arrivalIata)).map(_ => "OK") // TODO: Enhance these OKs for list of failed validations OR success response
    }

  override def addPassenger: ServiceCall[PassengerDto, String] =
    ServiceCall { dto: PassengerDto =>
      entityRef(dto.flightId).ask(AddPassenger(UUID.randomUUID().toString, dto.firstName, dto.lastName, dto.initial, dto.seatAssignment)).map(_ => "OK")
    }

  override def removePassenger(flightId: UUID, passengerId: UUID): ServiceCall[NotUsed, String] =
    ServiceCall { _ =>
      entityRef(flightId).ask(RemovePassenger(passengerId.toString)).map(_ => "OK")
    }

  override def selectSeat: ServiceCall[SelectSeatDto, String] =
    ServiceCall { dto: SelectSeatDto =>
      entityRef(dto.flightId).ask(SelectSeat(dto.passengerId.toString, dto.seatAssignment)).map(_ => "OK")
    }

  override def closeFlight(flightId: UUID): ServiceCall[NotUsed, String] =
    ServiceCall { _ =>
      entityRef(flightId).ask(CloseFlight).map(_ => "OK")
    }

  /**
    * Helper function to seed a new entity reference, uses a new UUID.
    */
  private def newEntityRef = persistentEntityRegistry.refFor[FlightEntity](UUID.randomUUID().toString)

  /**
    * Helper function to look up entity ref from flightId in UUID format.
    */
  private def entityRef(id: UUID) = persistentEntityRegistry.refFor[FlightEntity](id.toString)
}
