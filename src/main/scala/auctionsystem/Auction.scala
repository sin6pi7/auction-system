package auctionsystem

import akka.actor.{ActorRef, Actor}
import akka.event.LoggingReceive
import scala.concurrent.duration._

object Auction {
  case class Bid(amount: Int) {
    require(amount >= 0)
  }
  case object Price
}

class Auction(startingPrice: Int) extends Actor {
  import Auction._

  val BID_TIMEOUT = 5.seconds
  val DELETE_TIMEOUT = 5.seconds

  require(startingPrice >= 0)
  var price: Int = startingPrice
  var buyers: Set[ActorRef] = Set()

  def receive = LoggingReceive {
    case Bid(amount) if amount > price =>
      price = amount
      buyers + sender
  }
}
