lazy val baseSettings = Seq(
  scalaVersion := "2.13.10",
  organization := "com.madgag",
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishTo := sonatypePublishToBundle.value,
  scmInfo := Some(ScmInfo(
    url("https://github.com/rtyley/line-break-preserving-line-splitting"),
    "scm:git:git@github.com:rtyley/line-break-preserving-line-splitting.git"
  )),
  scalacOptions ++= Seq("-deprecation", "-unchecked")
)

name := "line-splitting-root"

description := "A few odds and ends to replace mapViews"

ThisBuild / scalaVersion := "2.13.10"

lazy val lineSplitting = project.in(file("line-splitting")).settings(
  baseSettings,
  name := "line-break-preserving-line-splitting",
  crossScalaVersions := Seq(scalaVersion.value, "3.2.1"),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.14" % Test,
    "com.madgag" %% "scala-collection-plus" % "0.11" % Test
  )
)

lazy val docs = project.in(file("line-splitting-generated-docs")) // important: it must not be docs/
  .dependsOn(lineSplitting)
  .enablePlugins(MdocPlugin)
  .settings(
    mdocOut := (ThisBuild / baseDirectory).value
  )

import ReleaseTransformations._

lazy val lineSplittingRoot = (project in file("."))
  .aggregate(
    lineSplitting
  )
  .settings(baseSettings).settings(
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  releaseCrossBuild := true, // true if you cross-build the project for multiple Scala versions
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    // For non cross-build projects, use releaseStepCommand("publishSigned")
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )

)
