import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import io.navidjalali.primenumberserver.actors.{Memo, Root}
import io.navidjalali.primenumberserver.logic.PrimeNumberGenerator
import io.navidjalali.primes.PrimeGenPayload
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.Matchers
import org.specs2.mutable.SpecificationWithJUnit

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}

class PrimeSpecs(implicit ee: ExecutionEnv) extends SpecificationWithJUnit with Matchers {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(Root(), "prime-test")
  implicit val executor: ExecutionContextExecutor = system.executionContext
  implicit val startupTimeout: Timeout = 10.seconds


  val memo: ActorRef[Memo.Message] = Await.result(
    system.ask[ActorRef[Memo.Message]](replyTo => SpawnProtocol.Spawn(Memo(), "memo", Props.empty, replyTo)),
    startupTimeout.duration
  )

  val primeGen: PrimeNumberGenerator = PrimeNumberGenerator(memo)

  "primes" should {
    "return empty stream if under 2" in {
      primeGen.generatePrimes(PrimeGenPayload(0)).map(_.value)
        .runWith(Sink.collection)
        .map(_.toList) must beEqualTo(List.empty).await

      primeGen.generatePrimes(PrimeGenPayload(1)).map(_.value)
        .runWith(Sink.collection)
        .map(_.toList) must beEqualTo(List.empty).await
    }

    "Works when bound is prime" in {
      primeGen.generatePrimes(PrimeGenPayload(3)).map(_.value)
        .runWith(Sink.collection)
        .map(_.toList) must beEqualTo(List(2, 3)).await
    }

    "return primes correctly" in {
      primeGen.generatePrimes(PrimeGenPayload(100)).map(_.value)
        .runWith(Sink.collection)
        .map(_.toList) must beEqualTo(
        List(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97)
      ).await
    }
  }
}
