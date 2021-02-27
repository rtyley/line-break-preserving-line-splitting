package com.madgag.linesplitting

import com.madgag.linesplitting.LineBreakPreservingIterator.{MaxLBSize, MinBufferSize}

import java.io.Reader
import scala.annotation.tailrec

case class FillResult(filledToBufferEdge: Boolean, endOfStream: Boolean)

object LineBreakPreservingIterator {

  val MaxLBSize = 2 // CR+LF is the longest line-break sequence

  // has to be at least 1 more than the length of a line-break, other
  val MinBufferSize: Int = MaxLBSize + 1
}

/*
  https://github.com/google/guava/commit/a2c7f54378dc2585f8524f59d71e56353ac0a1ba
  Usually a line - multiple lines - will fit into the character buffer.
  Occasionally a line could be crazy long, and span multiple lengths of the buffer.
  LF, CR, CR LF (windows) - but disregard LF CR (Acorn)
  \n, \r, \r \n
   */
class LineBreakPreservingIterator(reader: Reader, bufferSize: Int = 0x800) extends Iterator[String] {

  require(bufferSize >= MinBufferSize,
    "Buffer size must be at least 1 character more than the length of the longest line-break (ie 2: CR+LF)")

  val buf = new Array[Char](bufferSize)

  var endOfStream: Boolean = false

  /**
   * Anything from `readPointer` onwards, up to `writePointer` exclusive,
   * can be read.
   *
   * After a line is read, `readPointer` will be pointing to the next character
   * immediately after the terminator of that line.
   */
  var readPointer: Int = 0

  /**
   * Anything from `writePointer` onwards, up to `readPointer` exclusive,
   * can be overwritten.
   */
  var writePointer: Int = 0

  private def hasReadableBufferedBytes = readPointer != writePointer // check assumptions here - do we use sentinel values?

  override def hasNext: Boolean = hasReadableBufferedBytes || !endOfStream

  private def numBytesWeCouldAcceptInOneRead: Int = {
    val firstUnwritableIndex = if (readPointer > writePointer) readPointer else bufferSize
    firstUnwritableIndex - writePointer
  }

  /*
   * Must repeatedly fill until it finds a newline or the endOfStream
   * // case A "LBX visible in readable bytes before buffer edge"
   */
  override def next(): String = findLBX().getOrElse {
    if (readPointer <= writePointer) caseB_readBeforeOrEqualToWritePointer() else caseC_writeBeforeReadPointer()
  }

  // case B "read is before or equal to write in the buffer, with no LBX visible in readable bytes"
  // @tailrec
  private def caseB_readBeforeOrEqualToWritePointer(): String = {
    val fillResult = fill()
    if (fillResult.endOfStream) grabUpTo(writePointer) else findLBX().getOrElse {
      if (fillResult.filledToBufferEdge) caseC_writeBeforeReadPointer() else caseB_readBeforeOrEqualToWritePointer()
    }
  }

  // case C "read is ahead of write in the buffer, with no LBX visible before buffer edge"
  private def caseC_writeBeforeReadPointer(): String = {
    val stringBuilder = new StringBuilder()
    rollOverBufferEndToReturnToBufferStartWith(stringBuilder)
    searchingForLBXWith(stringBuilder)
  }

  private def rollOverBufferEndToReturnToBufferStartWith(stringBuilder: StringBuilder): Unit = {
    val charsRemaining = bufferSize - readPointer
    val numCharsToClone = Math.min(MaxLBSize, charsRemaining)
    stringBuilder.appendAll(buf, readPointer, charsRemaining - numCharsToClone)
    Array.copy(buf, bufferSize - numCharsToClone, buf, 0, numCharsToClone)
    readPointer = 0
    writePointer = numCharsToClone
  }

  private def grabUpTo(startOfNextLine: Int): String = {
    val str = new String(buf, readPointer, startOfNextLine - readPointer)
    readPointer = startOfNextLine
    str
  }

  def findLBX(): Option[String] = findLBXStartingReadFrom(readPointer)

  private def findLBXWith(stringBuilder: StringBuilder): Option[String] =
    findLBXStartingReadFrom(0, x => stringBuilder.append(x).result())

  def findLBXStartingReadFrom(startI: Int, concludeLBXSearchWithFinalChunk: String => String = identity): Option[String] = {
    var i = startI // readPointer?
    val searchBoundary = (if (writePointer==0) bufferSize else writePointer) - 1
    val boundaryFor2CharLB = searchBoundary - 1

    while (i < searchBoundary) {
      val c = buf(i)
      if (c == '\r' && buf(i+1) == '\n') {
        return if (i < boundaryFor2CharLB) Some(concludeLBXSearchWithFinalChunk(grabUpTo(i + 2))) else None // Need to ensure there's a buffer byte...
      } else if (c == '\n' || c == '\r') {
        return Some(concludeLBXSearchWithFinalChunk(grabUpTo(i + 1)))
      }
      i += 1
    }
    None
  }

  // @tailrec
  private def searchingForLBXWith(stringBuilder: StringBuilder): String = {
    val fillResult = fill()
    if (fillResult.endOfStream) {
      stringBuilder.appendAll(buf, readPointer, writePointer - readPointer)
      readPointer = writePointer
      stringBuilder.result()
    } else findLBXWith(stringBuilder).getOrElse {
      if (fillResult.filledToBufferEdge) {
        rollOverBufferEndToReturnToBufferStartWith(stringBuilder)
      }
      searchingForLBXWith(stringBuilder)
    }
  }

  /*
  Fill needs to block/loop until it either reaches endOfStream or reads at least one byte
   */
  @tailrec
  private def fill(): FillResult = {
    val bytesRead = reader.read(buf, writePointer, numBytesWeCouldAcceptInOneRead)
    bytesRead match {
      case -1 =>
        endOfStream = true
        FillResult(filledToBufferEdge=false, endOfStream = true)
      case 0 =>
        fill()
      case _ =>
        writePointer = (writePointer + bytesRead) % bufferSize // if we filled the buf, writePointer goes back to zero
        FillResult(filledToBufferEdge = writePointer == 0, endOfStream = false)
    }
  }

}