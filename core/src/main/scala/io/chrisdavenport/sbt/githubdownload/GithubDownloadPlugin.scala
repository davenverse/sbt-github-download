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
      localPath: String,
      ref: Option[String] = None
    )

    val githubDownloadTargets: SettingKey[Seq[GithubDownloadTarget]] =
      settingKey[Seq[GithubDownloadTarget]](
        "Github Targets to download"
      )
    val githubDownloadToken: SettingKey[Option[String]] =
      settingKey[Option[String]]("Github token for downloading")
    

    val githubDownloadExecute: TaskKey[Unit] = taskKey[Unit]("Main task to download targets, overwriting current files")
    val githubDownloadCheck: TaskKey[Unit] = taskKey[Unit]("Checks local versions of files are correct with remote store")
  }


  import autoImport._
  override lazy val projectSettings = Seq(
    githubDownloadTargets := List(),
    githubDownloadToken := None,
    githubDownloadExecute := {
      val token = githubDownloadToken.value
      val artifacts = githubDownloadTargets.value
      Downloader.runGithubDownload[CIO](artifacts.toList, token).unsafeRunSync
    },
    githubDownloadCheck := {
      val token = githubDownloadToken.value
      val artifacts = githubDownloadTargets.value
      Downloader.runGithubDownloadCheck[CIO](artifacts.toList, token).unsafeRunSync
    }
  )

  
}