package auctionsystem

import akka.actor.{Cancellable, ActorRef, Actor}
import akka.event.LoggingReceive
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Auction {
  case class Bid(amount: Int) {
    require(amount >= 0)
  }
  object BidTimerExpires
  object DeleteTimerExpires
  object Relist
}

class Auction(startingPrice: Int) extends Actor {
  import Auction._
  import Buyer._

  val BID_TIMEOUT = 5.seconds
  val DELETE_TIMEOUT = 2.seconds

  // validation
  require(startingPrice >= 0)

  // state
  var price: Int = startingPrice
  var buyers: Set[ActorRef] = Set()
  var winner: ActorRef = null

  var bidTimer: Cancellable = null
  var deleteTimer: Cancellable = null

  // start auction
  bidTimer = context.system.scheduler.scheduleOnce(BID_TIMEOUT, self, BidTimerExpires)
  def receive = created

  // == CREATED ==
  def created: Receive = LoggingReceive {
    case Bid(amount) if amount > price =>
      price = amount
      buyers += sender
      winner = sender
      context become activated
    case BidTimerExpires =>
      deleteTimer = context.system.scheduler.scheduleOnce(DELETE_TIMEOUT, self, DeleteTimerExpires)
      context become ignored
  }

  // == IGNORED ==
  def ignored: Receive = LoggingReceive {
    case DeleteTimerExpires =>
      context.stop(self)
    case Relist =>
      deleteTimer.cancel()
      context become created
  }

  // == ACTIVATED ==
  def activated: Receive = LoggingReceive {
    case Bid(amount) if amount > price =>
      price = amount
      buyers += sender
      winner = sender
    case BidTimerExpires =>
      buyers.foreach {
        case w if w equals winner =>
          w ! AuctionWon(self)
        case b =>
          b ! AuctionLost(self)
      }
      deleteTimer = context.system.scheduler.scheduleOnce(DELETE_TIMEOUT, self, DeleteTimerExpires)
      context become sold
  }

  // == SOLD ==
  def sold: Receive = LoggingReceive {
    case DeleteTimerExpires =>
      context.stop(self)
  }
}
