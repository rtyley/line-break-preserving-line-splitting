import ReleaseTransformations.*
import sbtversionpolicy.withsbtrelease.ReleaseVersion

name := "line-splitting-root"

description := "Splits lines of text while preserving line breaks"

ThisBuild / scalaVersion := "2.13.16"

lazy val lineSplitting = project.in(file("line-splitting")).settings(
  name := "line-break-preserving-line-splitting",
  organization := "com.madgag",
  licenses := Seq(License.Apache2),
  scalacOptions ++= Seq("-deprecation", "-unchecked", "-release:11"),
  crossScalaVersions := Seq(scalaVersion.value, "3.3.4"),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    "com.madgag" %% "scala-collection-plus" % "0.11" % Test
  ),
  Test/testOptions += Tests.Argument(
    TestFrameworks.ScalaTest,
    "-u", s"test-results/scala-${scalaVersion.value}"
  )
)

lazy val docs = project.in(file("line-splitting-generated-docs")) // important: it must not be docs/
  .dependsOn(lineSplitting)
  .enablePlugins(MdocPlugin)
  .settings(
    mdocOut := (ThisBuild / baseDirectory).value
  )

lazy val lineSplittingRoot = (project in file(".")).aggregate(lineSplitting).settings(
  publish / skip := true,
  releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value,
  releaseCrossBuild := true, // true if you cross-build the project for multiple Scala versions
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion
  )
)
