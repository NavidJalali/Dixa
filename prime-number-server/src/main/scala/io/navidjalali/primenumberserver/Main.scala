package io.navidjalali.primenumberserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.logRequestResult
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.directives.{LogEntry, LoggingMagnet}
import io.navidjalali.primes.PrimeGenHandler
import org.apache.logging.log4j.{LogManager, Logger}

import scala.io.StdIn

object Main {
  private val logger: Logger = LogManager.getRootLogger

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "primes")
    implicit val executionContext = system.executionContext

    def withLogging = logRequestResult(LoggingMagnet.forRequestResponseFromFullShow(
      req => result => Some(LogEntry(s"${req.method} ${req.uri.path} ${
        result match {
          case RouteResult.Complete(response) => response.status.toString
          case rejected: RouteResult.Rejected => rejected.toString
        }}", Logging.InfoLevel))
    ))

    (for {
      server <- Http().newServerAt("localhost", 8001)
        .bind(PrimeGenHandler(new PrimeNumberGenerator))
      _ = logger.info(s"Server is now up @ http://localhost:8001")
      _ = StdIn.readLine()
      _ = logger.info(s"Shutting down")
      _ <- server.unbind()
    } yield ())
      .onComplete(_ => system.terminate())
  }
}
