package io.navidjalali.primenumberserver.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

class Memo {

  import Memo._

  def start(initial: State): Behavior[Message] = running(initial)

  def running(state: State): Behavior[Message] =
    Behaviors.receiveMessage[Message] {
      case Set(value) =>
        state.value.headOption match {
          case Some(current) =>
            value
              .headOption
              .map(_ > current)
              .fold[Behavior[Message]](Behaviors.same) {
                if (_) running(State(value)) else Behaviors.same
              }
          case None => running(State(value))
        }
      case Memo.Get(replyTo) =>
        replyTo ! state.value
        Behaviors.same
    }
}

object Memo {
  case class State(value: Vector[Long])

  sealed trait Message

  case class Set(value: Vector[Long]) extends Message

  case class Get(replyTo: ActorRef[Vector[Long]]) extends Message

  def apply(): Behavior[Message] = new Memo().running(Memo.State(Vector(3L, 2L)))
}
