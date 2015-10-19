package auctionsystem

import akka.actor.{ActorRef, Actor}
import akka.event.LoggingReceive

object Buyer {
  case class BidOnAuction(auction: ActorRef, amount: Int) {
    require(amount > 0)
  }
}

class Buyer(auctions: Set[ActorRef]) extends Actor {
  import Buyer._
  import Auction._

  def receive = LoggingReceive {
    case BidOnAuction(auction, amount) if auctions.contains(auction) =>
      auction ! Bid(amount)
  }
}
