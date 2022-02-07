import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTest, RouteTestTimeout, Specs2RouteTest}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.navidjalali.primes.{PrimeGen, PrimeGenPayload, PrimeGenResponse}
import io.navidjalali.proxyserver.ProxyServer
import org.specs2.mutable.SpecificationWithJUnit

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

class RouteSpecs extends SpecificationWithJUnit with RouteTest with Specs2RouteTest with FailFastCirceSupport {
  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(15.seconds)

  def createService(primeGen: PrimeGen): Route = {
    implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "proxy-test")
    implicit val executor: ExecutionContextExecutor = system.executionContext
    implicit val scheduler = system.scheduler
    implicit val startupTimeout: Timeout = 10.seconds

    ProxyServer(primeGen).route
  }

  val primeGen: PrimeGen = new PrimeGen {
    override def generatePrimes(in: PrimeGenPayload): Source[PrimeGenResponse, NotUsed] =
      Source(List(1L, 2L, 3L, 4L)).map(PrimeGenResponse(_))
  }

  val service: Route = Route.seal(createService(primeGen))

  "primes route" should {
    "should work properly" in {
      Get("/prime/10") ~> service ~> check {
        status === StatusCodes.OK
        response.entity.isChunked() === true
        response.entity.toStrict(1.second).map(_.data.utf8String === "1, 2, 3, 4")
      }
    }
    "only accept longs" in {
      Get("/prime/1.2") ~> service ~> check {
        status === StatusCodes.NotFound
      }

      Get("/prime/-12") ~> service ~> check {
        status === StatusCodes.NotFound
      }

      Get("/prime/twelve") ~> service ~> check {
        status === StatusCodes.NotFound
      }
    }
  }
}
