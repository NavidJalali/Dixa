package io.navidjalali.primenumberserver

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.navidjalali.primes.{PrimeGen, PrimeGenPayload, PrimeGenResponse}

final class PrimeNumberGenerator extends PrimeGen {
  override def generatePrimes(in: PrimeGenPayload): Source[PrimeGenResponse, NotUsed] =
    Source(0L to in.upperBound)
      .map(PrimeGenResponse(_))
}
