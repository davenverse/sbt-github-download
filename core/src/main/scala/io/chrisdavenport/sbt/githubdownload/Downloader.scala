package io.chrisdavenport.sbt.githubdownload

import cats._
import cats.syntax.all._

import cats.effect._
import GithubDownloadPlugin.autoImport.GithubDownloadTarget
import org.http4s.ember.client.EmberClientBuilder
import io.chrisdavenport.github.OAuth
import io.chrisdavenport.github.endpoints.repositories.Content
import java.nio.file.{Paths, Path}
import io.chrisdavenport.github.data.Content.{Content => ContentD}
import org.http4s.client.Client
import java.util.Base64

object Downloader {

  private val b64 = Base64.getMimeDecoder()
  
  def runGithubDownload[F[_]: Concurrent: Timer: ContextShift](targets: List[GithubDownloadTarget], token: Option[String]): F[Unit] = {
    (Blocker[F], EmberClientBuilder.default[F].build).tupled.use{ case (blocker, client) => 
      targets.traverse_(downloadFile(blocker, client)(_, token))
    }
  }

  private def downloadFile[F[_]: Concurrent : ContextShift](blocker: Blocker, client: Client[F])(target: GithubDownloadTarget, token: Option[String]): F[Unit] = {
    for {
      content <- Content.contentsFor[F](target.org, target.repo, target.repoPath, None, token.map(OAuth(_))).run(client)
      fileContent <- content match {
        case ContentD.File(data) => data.pure[F]
        case ContentD.Directory(data) => new Throwable("Cannot Download Directory").raiseError
      }
      data = new String(b64.decode(fileContent.content))
      wd <- Sync[F].delay(System.getProperty("user.dir"))
      path = Paths.get(wd, target.localPath)
      _ <- 
        fs2.Stream(data)
          .through(fs2.text.utf8Encode)
          .through(
            fs2.io.file.writeAll(path, blocker)
          ).compile.drain
    } yield ()
  }
}