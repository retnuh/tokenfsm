package com.github.retnuh.tokenfsm

import org.scalatest.Matchers

class TokenFSMSpecs extends org.scalatest.FlatSpec with Matchers {

  private val ws = """\s+""".r
  private def split(s: String) = ws.pattern.split(s).to[List]

  "A TokenFSM" should "match the tokens and offsets from a seq of tokens" in {
    val sentence = split("Der schnelle braune Fuchs sprang über den faulen Hund")
    val fsm = TokenFSM(Seq(Seq("schnelle", "braune"), Seq("faulen", "Hund"), Seq("der", "schnelle", "scheitern")))
    val matches = fsm.matches(sentence)
    matches.length shouldBe 2
    matches(0) shouldBe (Seq("schnelle", "braune"), (1, 2))
    matches(1) shouldBe (Seq("faulen", "Hund"), (7, 8))
  }

  it should "allow overlapping matches" in {
    val fsm = TokenFSM(Seq(split("flauschig rosa"), split("rosa Häschen"), split("rosa")))
    val matches = fsm.matches(split("Das flauschig rosa Häschen ist genial"))
    matches.length shouldBe 2
    matches(0) shouldBe (split("flauschig rosa"), (1, 2))
    matches(1) shouldBe (split("rosa Häschen"), (2, 3))
  }

  it should "not match smaller token phrases that are contained in larger matches" in {
    val fsm = TokenFSM(Seq(split("Gold"), split("weißes Gold"), split("gelbes")))
    val matches = fsm.matches(split("gelbes und weißes Gold"))
    matches.length shouldBe 2
    matches(0) shouldBe (split("gelbes"), (0, 0))
    matches(1) shouldBe (split("weißes Gold"), (2, 3))
  }

  it should "have smaller phrases match if a larger phrase does not complete by the end of the sentence" in {
    val fsm = TokenFSM(Seq(split("a"), split("b c"), split("b c d"), split("a b c d e f")))
    val matches = fsm.matches(split("a b c d e"))
    matches.length shouldBe 2
    matches(0) shouldBe (split("a"), (0, 0))
    matches(1) shouldBe (split("b c d"), (1, 3))
  }

  it should "remove duplicate matches" in {
    val fsm = TokenFSM(Seq(split("a"), split("a b x"), split("a b y"), split("a b z")))
    val matches = fsm.matches(split("a b c"))
    matches.length shouldBe 1
    matches(0) shouldBe (split("a"), (0, 0))
  }

  it should "not include smaller matches even when one dominant match fails but other matches" in {

    val text = "gelbes rosa und blau-grünes Armband mit Diamanten Cartier, Preis auf Anfrage. Spike Cuff, Giles & Brother von Philip Crangi, $ 88."
    val fsm = TokenFSM(Seq(split("gelbes"), split("rosa"), split("blau-grünes"), split("weiße perlen"), split("goldpüree"),
      split("blau"), split("grünes"), split("Diamanten")))

    val matches = fsm.matches(split(text))
    matches shouldNot contain((Seq("blau"), (3, 3)))
    matches shouldNot contain((Seq("grünes"), (5, 5)))
    matches should contain((split("blau-grünes"), (3, 3)))
    matches.length shouldBe 4
  }

}

