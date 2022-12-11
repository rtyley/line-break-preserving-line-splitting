# line-break-preserving-line-splitting

[![line-break-preserving-line-splitting artifacts](https://index.scala-lang.org/rtyley/line-break-preserving-line-splitting/line-break-preserving-line-splitting/latest-by-scala-version.svg)](https://index.scala-lang.org/rtyley/line-break-preserving-line-splitting/line-break-preserving-line-splitting/)
[![CI](https://github.com/rtyley/line-break-preserving-line-splitting/actions/workflows/ci.yml/badge.svg)](https://github.com/rtyley/line-break-preserving-line-splitting/actions/workflows/ci.yml)

### Features

* Streams from a `java.io.Reader`, so **can process very large files**
* Unlike `java.io.BufferedReader.readLine()`, **retains the line-break at the end of
  the line**, whether it's UNIX, Windows, etc
* Implements the `scala.collection.Iterator` trait. Lines are not retained in memory
  after being returned by `next()`.
* Limits memory consumption to a single ring-buffer the lives the lifetime of the
  iterator, and, for lines that loop round the ring-buffer, temporary `StringBuilder`
  instances that are garbage-collected after the line is returned.
* Reading a line using `next()` will return as soon as the `java.io.Reader` has
  returned enough characters - will at most only attempt to fill the ring-buffer.
  
For my purposes, the first two requirements were the most important. If I hadn't
wanted to retain the line-breaks, I'd probably have used
`java.io.BufferedReader.readLine()`.

### Installation

```scala
libraryDependencies += "com.madgag" %% "line-break-preserving-line-splitting" % "[version]"
```

### Example usage

```scala
import com.madgag.linesplitting._

val splitLines = {
  val reader: java.io.Reader = new java.io.StringReader("Foo\nBar\rBoo")

  val iter = new LineBreakPreservingIterator(reader)
  
  iter.toSeq
}.toSeq
// splitLines: Seq[String] = List(
//   """Foo
// """,
//   """Bar
""",
//   "Boo"
// )

splitLines.map(_.replace("\n", "[LF]").replace("\r", "[CR]"))
// res0: Seq[String] = List("Foo[LF]", "Bar[CR]", "Boo")
```
