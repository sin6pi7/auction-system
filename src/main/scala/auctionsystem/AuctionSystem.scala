package auctionsystem

import akka.actor._

object AuctionSystem extends App {
  // create actor system
  val system = ActorSystem("AuctionSystem")

  // create auctions
  val auction1 = system.actorOf(Props(new Auction(50)))
  val auction2 = system.actorOf(Props(new Auction(0)))

  // create buyers
  val buyer1 = system.actorOf(Props(new Buyer(Set(auction1, auction2))))

  // lets bid a bit
  buyer1 ! Buyer.BidOnAuction(auction2, 10)

  // wait for termination - do not close prematurely
  system.awaitTermination()
}