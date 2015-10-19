package auctionsystem

import akka.actor._
import auctionsystem.Buyer.BidOnAuction
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object AuctionSystem extends App {
  // create actor system
  val system = ActorSystem("AuctionSystem")

  // create auctions
  val auction1 = system.actorOf(Props(new Auction(50)), "auction1")
  val auction2 = system.actorOf(Props(new Auction(0)), "auction2")

  // create buyers
  val buyer1 = system.actorOf(Props(new Buyer(Set(auction1, auction2))), "buyer1")
  val buyer2 = system.actorOf(Props(new Buyer(Set(auction1, auction2))), "buyer2")

  // lets bid a bit
  buyer1 ! BidOnAuction(auction2, 10)
  buyer2 ! BidOnAuction(auction2, 20)
  buyer1 ! BidOnAuction(auction2, 10)

  system.scheduler.scheduleOnce(5 seconds, buyer1, BidOnAuction(auction1, 40))

  // wait for termination - do not close prematurely
  system.awaitTermination()
}