# Prerequisites
- JDK 11
- SBT
# How to run
Open 2 terminals and run `sbt proxyServer/run` and `sbt primeNumberServer/run`.
# How it was implemented
I used Akka gRPC to generate some code.
From then on it was just a matter of implementing some trait and setting up a HTTP server.
