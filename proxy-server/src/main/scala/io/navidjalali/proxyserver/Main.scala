package io.navidjalali.proxyserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.event.Logging
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{LogEntry, LoggingMagnet}
import akka.http.scaladsl.server.{Route, RouteResult, ValidationRejection}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import io.navidjalali.primes.{PrimeGenClient, PrimeGenPayload}
import org.apache.logging.log4j.{LogManager, Logger}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn
import scala.util.{Failure, Success}

object Main {

  private val logger: Logger = LogManager.getRootLogger

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "my-system")
    implicit val executionContext: ExecutionContextExecutor = system.executionContext
    implicit val streamingSupport: JsonEntityStreamingSupport =
      EntityStreamingSupport
        .json
        .withFramingRenderer(Flow[ByteString].intersperse(ByteString.empty, ByteString(", "), ByteString.empty))

    val clientSettings = GrpcClientSettings
      .connectToServiceAt("localhost", 8001)
      .withTls(false)

    val primeNumberClient = PrimeGenClient(clientSettings)

    def primes(successfulComplete: Source[Long, Any] => Route, upTo: Long): Route =
      successfulComplete(
        primeNumberClient.generatePrimes(PrimeGenPayload(upTo))
          .map(_.value)
      )

    def withLogging = logRequestResult(LoggingMagnet.forRequestResponseFromFullShow(
      req => result => Some(LogEntry(s"${req.method} ${req.uri.path} ${
        result match {
          case RouteResult.Complete(response) => response.status.toString
          case rejected: RouteResult.Rejected => rejected.toString
        }}", Logging.InfoLevel))
    ))

    val route =
      withLogging {
        path("stream" / "\\-?\\d+".r) { upperRaw =>
          get {
            onComplete(Unmarshal(upperRaw).to[Long]) {
              case Failure(exception) =>
                reject(ValidationRejection("Failed to unmarshal path segment", Some(exception)))
              case Success(upTo) =>
                import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
                primes(source => complete(source), upTo)
            }
          }
        }
      }

    (for {
      server <- Http().newServerAt("localhost", 8000).bind(route)
      _ = logger.info(s"Server is now up @ http://localhost:8000")
      _ = StdIn.readLine()
      _ = logger.info(s"Shutting down")
      _ <- server.unbind()
    } yield ())
      .onComplete(_ => system.terminate())
  }
}
