package io.navidjalali.proxyserver

import akka.actor.typed.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{LogEntry, LoggingMagnet}
import akka.http.scaladsl.server.{Directive0, Route, RouteResult, ValidationRejection}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import io.navidjalali.primes.{PrimeGen, PrimeGenPayload}
import org.apache.logging.log4j.{LogManager, Logger}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class ProxyServer(primeGen: PrimeGen)(implicit system: ActorSystem[_]) {
  val logger: Logger = LogManager.getLogger(classOf[ProxyServer])
  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val streamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport
      .json
      .withFramingRenderer(Flow[ByteString].intersperse(ByteString.empty, ByteString(", "), ByteString.empty))

  def primes(successfulComplete: Source[Long, Any] => Route, upTo: Long): Route =
    successfulComplete(
      primeGen.generatePrimes(PrimeGenPayload(upTo))
        .map(_.value)
    )

  def withLogging: Directive0 = logRequestResult(LoggingMagnet.forRequestResponseFromFullShow(
    req => result => Some(LogEntry(s"${req.method} ${req.uri.path} ${
      result match {
        case RouteResult.Complete(response) => response.status.toString
        case rejected: RouteResult.Rejected => rejected.toString
      }
    }", Logging.InfoLevel))
  ))

  val route =
    withLogging {
      path("prime" / "\\d+".r) { upperRaw =>
        get {
          onComplete(Unmarshal(upperRaw).to[Long]) {
            case Failure(exception) =>
              reject(ValidationRejection("Failed to unmarshal path segment", Some(exception)))
            case Success(upTo) =>
              import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
              primes(source => complete(source), upTo)
          }
        }
      } ~ path("healthcheck") {
        get {
          complete("OK")
        }
      }
    }

  def server(): Future[Http.ServerBinding] = for {
    server <- Http().newServerAt("0.0.0.0", 8000)
      .bind(route)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))
    _ = logger.info(s"Server is now up @ http://localhost:8000")
  } yield server
}
