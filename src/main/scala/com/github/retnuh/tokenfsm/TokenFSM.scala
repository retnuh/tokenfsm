package com.github.retnuh.tokenfsm

import scala.annotation.tailrec
import scala.collection.immutable.SortedSet

object TokenFSM {
  private implicit def lengthOrdering[T](implicit ordering: Ordering[T]) = new Ordering[Seq[T]] {
    override def compare(x: Seq[T], y: Seq[T]): Int = {
      x.lengthCompare(y.length) match {
        case lc if lc != 0 => lc
        case lc => ordering.compare(x.head, y.head) match {
          case fc if fc != 0 => fc
          case fc => compare(x.tail, y.tail)
        }
      }
    }
  }

  def apply[T](states: Seq[Seq[T]])(implicit ordering: Ordering[T]): TokenFSM[T] = {
    val initialStates = states.foldLeft(Map.empty[T, SortedSet[Seq[T]]])((m, ts) => {
      m + (ts.head -> (m.getOrElse(ts.head, SortedSet.empty[Seq[T]](lengthOrdering)) + ts))
    })
    new TokenFSM(initialStates)
  }
}

class TokenFSM[T](initialStates: Map[T, SortedSet[Seq[T]]])(implicit ordering: Ordering[T]) {

  import TokenFSM._

  @tailrec
  private def matchEach(tokens: Seq[T], pos: Int, inProgress: MachineState[T]): Seq[(Seq[T], (Int, Int))] = tokens.toSeq match {
    case empty if empty.isEmpty => inProgress.complete.collect({ case Matched(t, s, e) => (t, (s, e)) })
    case nonEmpty =>
      val head = nonEmpty.head
      val dominant: MachineState[T] = initialStates.getOrElse(head, SortedSet.empty[Seq[T]](lengthOrdering))
        .foldRight(inProgress)((ts: Seq[T], dominant: MachineState[T]) => dominant.dominate(Matching(ts, 0, pos)))
      val updated: MachineState[T] = dominant.nextToken(head, pos)
      matchEach(nonEmpty.tail, pos + 1, updated)
  }

  def matches(tokens: Seq[T]): Seq[(Seq[T], (Int, Int))] = {
    matchEach(tokens, 0, NonMatch[T]())
  }

}

sealed trait MachineState[T] {
  def nextToken(token: T, offset: Int): MachineState[T]

  def complete: Seq[MachineState[T]]

  def dominate(subject: MachineState[T]): MachineState[T] = (this, subject) match {
    case (NonMatch(), s) => s
    case (t, NonMatch()) => t
    case (DominatingMatching(d, os), ns) => DominatingMatching(d, os.dominate(ns))
    case (t, s) => DominatingMatching(t, s)
  }
}

trait InProgress[T] extends MachineState[T]

case class DominatingMatching[T](dominant: MachineState[T], subjugated: MachineState[T]) extends InProgress[T] {

  override def nextToken(token: T, offset: Int): MachineState[T] = {
    val d = dominant.nextToken(token, offset)
    val subj = subjugated.nextToken(token, offset)
    d match {
      // If we don't match, just fallback to our subjugated match
      case NonMatch() => subj
      // If the dominant has matched, it overrides any subjects that have matched and have <= end.
      // We have to keep going, though, in cases where a subjugated match fails at a later point in time and returns
      // a match that we dominate.  So we just keep going until complete.
      case _ => DominatingMatching(d, subj)
    }
  }

  override def complete: Seq[MachineState[T]] = {
    val d = dominant.complete
    val subjs = subjugated.complete
    d match {
      // If the dominant has matched, it overrides any subjects that have matched and have <= end.
      // We have to keep going, though, in cases where a subjugated match fails at a later point in time and returns
      // a match that we dominate.  So we just keep going until complete.
      case Seq(x: Matched[T]) => x +: subjs.collect({ case s: Matched[T] if s.end > x.end => s })
      // If we haven't matched, just fallback to our subjugated match
      case _ => subjs
    }
  }
}

case class Matching[T](tokens: Seq[T], pos: Int, start: Int) extends InProgress[T] {
  override def nextToken(token: T, offset: Int): MachineState[T] = {
    val nextPos = pos + 1
    if (tokens(pos) == token) {
      if (nextPos == tokens.length)
        Matched(tokens, start, offset)
      else
        Matching(tokens, pos + 1, start)
    } else {
      NonMatch[T]()
    }
  }

  override def complete: Seq[MachineState[T]] = Seq.empty
}

case class Matched[T](tokens: Seq[T], start: Int, end: Int) extends MachineState[T] {
  override def nextToken(token: T, offset: Int): MachineState[T] = this

  override def complete: Seq[MachineState[T]] = Seq(this)
}

case class NonMatch[T]() extends MachineState[T] {
  override def nextToken(token: T, offset: Int): MachineState[T] = this

  override def complete: Seq[MachineState[T]] = Seq.empty
}
