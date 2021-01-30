package com.madgag.linesplitting.testkit

import java.io.Reader

object PathologicalStringReader {
  def allPossible(text: String): Iterable[PathologicalStringReader] = {
    require(text.length < Integer.SIZE)
    for (i <- 0 until (1 << (text.length-1))) yield {
      val breakIndicies: Seq[Int] = (0 to text.length).filter(bitIndex => (i & (1L << bitIndex)) != 0).map(_ + 1)

      val segmentBounds = (0 +: breakIndicies).zip(breakIndicies :+ text.length)
      val segments: Seq[String] =
        segmentBounds.map { case (start, end) => text.substring(start, end) }

      // println(s"breakIndicies=${breakIndicies.mkString(",")}\tsegmentBounds=${segmentBounds.mkString(",")}\tsegments=${segments.mkString(",")}")

      assert(segments.mkString == text)

      new PathologicalStringReader(segments)
    }
  }
}

class PathologicalStringReader(val segments: Seq[String]) extends Reader {
  var closed = false

  var currentSegmentNumber = 0
  var currentProgressWithinSegment = 0

  override def read(cbuf: Array[Char], off: Int, len: Int): Int = {
    require(!closed)
    if (currentSegmentNumber >= segments.length) -1 else {
      val segment = segments(currentSegmentNumber)
      val remainingSegment: String = segment.drop(currentProgressWithinSegment)
      val lenToCopy = Math.min(remainingSegment.length, len)
      val segmentToGive = remainingSegment.take(lenToCopy)
      Array.copy(segmentToGive.toCharArray, 0, cbuf, off, lenToCopy)
      currentProgressWithinSegment += lenToCopy
      if (currentProgressWithinSegment == segment.length) {
        currentSegmentNumber += 1
        currentProgressWithinSegment = 0
      }
      lenToCopy
    }
  }

  override def close(): Unit = {
    closed = true
  }

  override def toString: String = s"${getClass.getSimpleName}(${segments.mkString(",")})"
}

