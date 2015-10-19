package auctionsystem

import akka.actor.{Actor, ActorRef, LoggingFSM}

import scala.concurrent.duration._

object Auction {
  // received events
  case class Bid(amount: Int) {
    require(amount >= 0)
  }
  object Initialize
  object BidTimerExpires
  object DeleteTimerExpires
  object Relist
}

// states
sealed trait State
case object Uninitialized extends State
case object Created extends State
case object Ignored extends State
case object Activated extends State
case object Sold extends State

// data
sealed trait Data
case class AuctionData(price: Int, buyers: Set[ActorRef], winner: ActorRef) extends Data

class Auction(startingPrice: Int) extends Actor with LoggingFSM[State, Data] {
  import Auction._
  import Buyer._

  val BID_TIMEOUT = 6 seconds
  val DELETE_TIMEOUT = 2 seconds

  startWith(Uninitialized, null)

  when (Uninitialized) {
    case Event(Initialize, _) =>
      goto(Created) using AuctionData(startingPrice, Set().empty, null)
  }

  when (Created) {
    case Event(Bid(amount), data: AuctionData) if amount > data.price =>
      goto(Activated) using data.copy(price = amount, buyers = Set(sender), winner = sender)
    case Event(BidTimerExpires, data: AuctionData) =>
      goto(Ignored) using data
  }

  when (Ignored, stateTimeout = DELETE_TIMEOUT) {
    case Event(Relist, data: AuctionData) =>
      goto(Created) using data
    case Event(StateTimeout, _) =>
      stop()
  }

  when (Activated) {
    case Event(Bid(amount), data: AuctionData) if amount > data.price =>
      stay using data.copy(price = amount, buyers = data.buyers + sender, winner = sender)
    case Event(BidTimerExpires, data: AuctionData) =>
      data.buyers.foreach {
        case w if w equals data.winner =>
          w ! AuctionWon(self)
        case b =>
          b ! AuctionLost(self)
      }
      goto(Sold)
  }

  when (Sold, stateTimeout = DELETE_TIMEOUT) {
    case Event(StateTimeout, _) =>
      stop()
  }

  onTransition {
    case _ -> Created =>
      setTimer("bidTimer", BidTimerExpires, BID_TIMEOUT)
  }

  initialize()
}
