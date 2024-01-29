package com.madgag.linesplitting

import com.madgag.linesplitting.LineBreakPreservingIterator.{MaxLBSize, MinBufferSize}
import com.madgag.linesplitting.testkit.PathologicalStringReader
import com.madgag.scala.collection.decorators._
import org.scalatest.Inspectors.forAll
import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

import java.io.{Reader, StringReader}
import scala.util.matching.Regex

// text - optionally accompanied by an explicitly expected result

// for any given piece of text, we should be able to vary the following without it failing or changing the result!
// buffer size
// reader behaviour/implementation

case class TopCat(reader: Reader, bufferSize: Int) {
  def execute(): Seq[String] = new LineBreakPreservingIterator(reader, bufferSize).toSeq
}

class LineBreakPreservingIteratorTest extends AnyFlatSpec with should.Matchers with OptionValues {

  val lineBreak: Regex = "\\R".r

  def splittingLinesOf(text: String): Seq[String] = exhaustivelyExecuteAndFindConsensusResult(text)

  private def exhaustivelyExecuteAndFindConsensusResult(text: String): Seq[String] = {
    val maxBufferSize = text.length + MaxLBSize + 1
    val testCases = for {
      bufferSize <- MinBufferSize to maxBufferSize
      reader <- allReadersFor(text)
    } yield TopCat(reader, bufferSize)

    val consensusResult: Seq[String] = findConsensusResult(testCases)
    checkResultQuality(text, consensusResult)
    consensusResult
  }

  def findConsensusResult(topCats: Iterable[TopCat]): Seq[String] = {
    val testCasesByResult = topCats.groupUp(_.execute())(_.toSeq.sortBy(tc => tc.bufferSize))
    testCasesByResult.keySet should have size 1
    testCasesByResult.keys.head
  }

  def allReadersFor(text: String): Iterable[Reader] = {
    Iterable(new StringReader(text)) ++ PathologicalStringReader.allPossible(text)
  }

  private def checkResultQuality(text: String, lines: Seq[String]) = {
    lines.mkString shouldBe text

    forAll(lines.dropRight(1)) { line =>
      val lineBreakDistanceBeforeEndOfString = lineBreak.findAllMatchIn(line).map(m => line.length - m.end).toSeq
      assert(lineBreakDistanceBeforeEndOfString.forall(_ == 0), (Seq(
        s"'$line' should have at most 1 line break, and if it exists that line-break should be at the end of the string."
      ) ++ Option.when(lineBreakDistanceBeforeEndOfString.nonEmpty)(s"Line-breaks occur at ${lineBreakDistanceBeforeEndOfString.mkString(", ")} char before the end of the string")).mkString(" "))
    }
    lineBreak.findAllIn(lines.last).size should be <= 1
  }

  it should "handle the empty string" in {
    splittingLinesOf("") shouldBe Seq("")
  }

  it should "return 1 line for a simple string" in {
    splittingLinesOf("abc") shouldBe Seq("abc")
  }

  it should "return 1 line for a simple string with a large buffer" in {
    TopCat(new StringReader("abc"),4).execute() shouldBe Seq("abc")
  }

  it should "return 1 line for a simple string with a newline at it's end" in {
    splittingLinesOf("abc\n") shouldBe Seq("abc\n")
  }

  it should "split on Windows newlines" in splittingLinesOf("Ab\r\n\r\nCd")
  it should "split on UNIX newlines" in splittingLinesOf("dig\n\nhum")
  it should "split on Windows newlines at the end of the data" in splittingLinesOf("abc\r\n\r\n")
  it should "split on UNIX newlines at the end of the data" in splittingLinesOf("dig\n\n")
  it should "split on a reasonably long example" in splittingLinesOf("\n\r\nabc\r\nd\n\nef\n")

  it should "return 2 lines for a simple string with a newline in the middle of it" in {
    splittingLinesOf("abc\ndig") shouldBe Seq("abc\n", "dig")
  }

  it should "return 2 lines for two simple strings each ending with a newline" in {
    splittingLinesOf("abc\ndig\n") shouldBe Seq("abc\n","dig\n")
  }

  it should "handle weird border issues" in {
    splittingLinesOf("h\r\n\r\nmno") shouldBe Seq("h\r\n", "\r\n", "mno")
  }

  it should "handle weird border issues quicka" in {
    splittingLinesOf("mno") shouldBe Seq("mno")
  }

  it should "handle crazy thang" in {
    splittingLinesOf("abc") shouldBe Seq("abc")
  }

  it should "be cool, real cool" in {
    splittingLinesOf("F\r\n\r\nM") shouldBe Seq("F\r\n", "\r\n", "M")
  }

  it should "be cool, abcl" in {
    splittingLinesOf("F\n\nM") shouldBe Seq("F\n", "\n", "M")
  }

  it should "not mind if the characters for a 2-char line-break arrive one at a time" in {
    new LineBreakPreservingIterator(new PathologicalStringReader(Seq("\r", "\n"))).toSeq shouldBe Seq("\r\n")
  }


  it should "be sensible about how many separate lines you get" in {
    splittingLinesOf("\n") shouldBe Seq("notreally\n")
    splittingLinesOf("\n\n") shouldBe Seq("\n", "\n")
    splittingLinesOf("\r") shouldBe Seq("\r")
    splittingLinesOf("\r\r") shouldBe Seq("\r", "\r")
    splittingLinesOf("\r\n") shouldBe Seq("\r\n")
    splittingLinesOf("\r\n\r\n") shouldBe Seq("\r\n", "\r\n")
    splittingLinesOf("\r\r\n\r") shouldBe Seq("\r", "\r\n", "\r")
  }

}
