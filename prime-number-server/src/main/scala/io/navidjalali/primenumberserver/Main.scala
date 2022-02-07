package io.navidjalali.primenumberserver

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import io.navidjalali.primenumberserver.actors.{Memo, Root}
import io.navidjalali.primenumberserver.logic.PrimeNumberGenerator

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContextExecutor}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(Root(), "primes")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    implicit val timeout: Timeout = Timeout(3.seconds)

    val app = for {
      memo <- system.ask[ActorRef[Memo.Message]](SpawnProtocol.Spawn(Memo(), "memo", Props.empty, _))
      primeGen = new PrimeNumberGenerator(memo)
      _ <- PrimeNumberServer(primeGen).server()
    } yield ()

    sys.addShutdownHook({
      system.terminate()
    })

    Await.result(app, Duration.Inf)
  }
}
