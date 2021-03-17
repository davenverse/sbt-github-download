package io.chrisdavenport.sbt.githubdownload


import sbt._
import Keys._
import cats.effect.{IO => CIO, ContextShift, Timer}

object GithubDownloadPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  implicit val CS : ContextShift[CIO] = CIO.contextShift(scala.concurrent.ExecutionContext.global)
  implicit val T: Timer[CIO] = CIO.timer(scala.concurrent.ExecutionContext.global)

  object autoImport {
    case class GithubDownloadTarget(
      org: String,
      repo: String,
      repoPath: String,
      localPath: String
    )

    val githubDownloadTargets: SettingKey[List[GithubDownloadTarget]] =
      settingKey[List[GithubDownloadTarget]](
        "Github Targets to download"
      )
    val githubDownloadToken: SettingKey[Option[String]] =
      settingKey[Option[String]]("Github token for downloading")
    

    val githubDownload: TaskKey[Unit] = taskKey[Unit]("Main task to download artifacts")
  }


  import autoImport._
  override lazy val projectSettings = Seq(
    githubDownloadTargets := List(),
    githubDownloadToken := None,
    githubDownload := {
      val token = githubDownloadToken.value
      val artifacts = githubDownloadTargets.value
      Downloader.runGithubDownload[CIO](artifacts, token).unsafeRunSync
    },
    (Compile / compile) := ((Compile / compile) dependsOn githubDownload).value
  )

  
}