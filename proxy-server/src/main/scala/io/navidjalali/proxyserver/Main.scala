package io.navidjalali.proxyserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import io.navidjalali.primes.PrimeGenClient

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "proxy-server")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext

    val app = ProxyServer(
      PrimeGenClient(
        GrpcClientSettings
          .connectToServiceAt("localhost", 9000)
          .withTls(false)
      )
    ).server().flatMap(_ => Future.never)

    sys.addShutdownHook({
      system.terminate()
    })

    Await.result(app, Duration.Inf)
  }
}
