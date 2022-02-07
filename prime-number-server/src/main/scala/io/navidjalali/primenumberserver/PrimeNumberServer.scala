package io.navidjalali.primenumberserver

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import io.navidjalali.primenumberserver.logic.PrimeNumberGenerator
import io.navidjalali.primes.{PrimeGen, PrimeGenHandler}
import org.apache.logging.log4j.{LogManager, Logger}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

case class PrimeNumberServer(primeGen: PrimeGen)(implicit system: ActorSystem[_]) {
  implicit val executionContext: ExecutionContextExecutor = system.executionContext

  val logger: Logger = LogManager.getLogger(classOf[PrimeNumberGenerator])

  def server(): Future[Http.ServerBinding] =
    for {
      server <- Http().newServerAt("0.0.0.0", 9000)
        .bind(PrimeGenHandler(primeGen))
        .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))
      _ = logger.info(s"Server is now up @ http://localhost:9000")
      _ <- Future.never
    } yield server
}
