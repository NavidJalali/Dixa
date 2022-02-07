package io.navidjalali.primenumberserver.logic

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import io.navidjalali.primenumberserver.actors.Memo
import io.navidjalali.primes.{PrimeGen, PrimeGenPayload, PrimeGenResponse}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

final case class PrimeNumberGenerator(memoActor: ActorRef[Memo.Message])
                                     (implicit system: ActorSystem[SpawnProtocol.Command]) extends PrimeGen {
  implicit private val executionContext: ExecutionContextExecutor = system.executionContext

  implicit val timeout: Timeout = Timeout(5.seconds)

  override def generatePrimes(in: PrimeGenPayload): Source[PrimeGenResponse, NotUsed] =
    Source.future(memoActor ? Memo.Get)
      .flatMapConcat {
        memo =>
          if (in.upperBound < 2)
            Source.empty
          else if (in.upperBound <= memo.head)
            Source(memo.reverse).takeWhile(_ <= in.upperBound)
          else primes(memo).takeWhile(_ <= in.upperBound)
      }
      .map(PrimeGenResponse(_))

  private def from(n: Long): Source[Long, NotUsed] = Source.unfold(n)(x => Some(x + 2, x))

  private def primes(memo: Vector[Long]): Source[Long, NotUsed] = {
    Source(memo.drop(1).reverse) ++ Source.unfoldAsync(memo) {
      current =>
        from(current.head + 2)
          .filter(x => current.tail.forall(x % _ > 0)).take(1)
          .runWith(Sink.head)
          .map(current.prepended)
          .map(Some(_, current.head))
          .andThen(_ => memoActor ! Memo.Set(current))
    }
  }
}
