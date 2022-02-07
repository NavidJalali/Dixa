package io.navidjalali.primenumberserver.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, SpawnProtocol}

object Root {
  def apply(): Behavior[SpawnProtocol.Command] =
    Behaviors.setup {
      context => {
        SpawnProtocol()
      }
    }
}
