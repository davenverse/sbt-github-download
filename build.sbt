ThisBuild / crossScalaVersions := Seq("2.12.18")
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / githubWorkflowArtifactUpload := false


ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test")),
)

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")

// currently only publishing tags
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / githubWorkflowPublishPreamble ++=
  WorkflowStep.Use("olafurpg", "setup-gpg", "v3") :: Nil

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    name = Some("Publish artifacts to Sonatype"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"))
)

val http4sV = "0.21.15"

// Projects
lazy val `sbt-github-download` = project.in(file("."))
  .disablePlugins(MimaPlugin)
  .enablePlugins(NoPublishPlugin)
  .aggregate(core)

lazy val core = project.in(file("core"))
  .enablePlugins(SbtPlugin)
  .settings(commonSettings)
  .settings(
    name := "sbt-github-download",

    scalacOptions += "-Ypartial-unification",

    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full),


    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    test := {
      (Test / test).value
      scripted.toTask("").value
    }
  )

// General Settings
lazy val commonSettings = Seq(
  

  libraryDependencies ++= Seq(
    "io.chrisdavenport"           %% "github"                     % "0.1.0",

    "org.http4s"                  %% "http4s-ember-client"        % http4sV,
    "org.http4s"                  %% "http4s-circe"               % http4sV,
    
  )
)

// General Settings
inThisBuild(List(
  organization := "io.chrisdavenport",
  developers := List(
    Developer("ChristopherDavenport", "Christopher Davenport", "chris@christopherdavenport.tech", url("https://github.com/ChristopherDavenport"))
  ),

  homepage := Some(url("https://github.com/ChristopherDavenport/sbt-github-download")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

  pomIncludeRepository := { _ => false},
  scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/ChristopherDavenport/sbt-github-download/blob/v" + version.value + "€{FILE_PATH}.scala"
  )
))
