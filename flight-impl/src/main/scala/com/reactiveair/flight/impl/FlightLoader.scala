package com.reactiveair.flight.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents

import com.reactiveair.flight.api.FlightService
import com.softwaremill.macwire._

class FlightLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new FlightApplication(context) {
      override def serviceLocator: ServiceLocator = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new FlightApplication(context) with LagomDevModeComponents
}

abstract class FlightApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with AhcWSComponents {

  // Bind the services that this server provides
  override lazy val lagomServer = LagomServer.forServices(
    bindService[FlightService].to(wire[FlightServiceImpl])
  )

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry = FlightSerializationRegistry

  // Register the Hello persistent entity
  persistentEntityRegistry.register(wire[FlightEntity])
}
