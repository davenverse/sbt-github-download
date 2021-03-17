package io.chrisdavenport.sbt.githubdownload

import cats._
import cats.syntax.all._

import cats.effect._
import GithubDownloadPlugin.autoImport.GithubDownloadTarget
import org.http4s.ember.client.EmberClientBuilder
import io.chrisdavenport.github.OAuth
import io.chrisdavenport.github.endpoints.repositories.Content
import java.nio.file.{Paths, Path}
import io.chrisdavenport.github.data.Content.{Content => ContentD, ContentFileData}
import org.http4s.client.Client
import java.util.Base64
import io.chrisdavenport.github
import java.nio.file.StandardOpenOption

object Downloader {
  
  def runGithubDownload[F[_]: Concurrent: Timer: ContextShift](targets: List[GithubDownloadTarget], token: Option[String]): F[Unit] = {
    if (targets.nonEmpty) {
      (Blocker[F], EmberClientBuilder.default[F].build).tupled.use{ case (blocker, client) => 
        targets.traverse_(target => 
          downloadFile(blocker, client, token)(target).flatMap{data => 
            writeFileContent(blocker)(target, data)
          }
        )
      }
    } else Applicative[F].unit
  }

  def runGithubDownloadCheck[F[_]: Concurrent: Timer: ContextShift](targets: List[GithubDownloadTarget], token: Option[String]): F[Unit] = {
    if (targets.nonEmpty) {
      (Blocker[F], EmberClientBuilder.default[F].build).tupled.use{ case (blocker, client) => 
        targets.traverse_(target => 
          downloadFile(blocker, client, token)(target).flatMap{data => 
            checkFileContent(blocker)(target, data)
          }
        )
      }
    } else Applicative[F].unit
  }

  private def downloadFile[F[_]: Concurrent : ContextShift](blocker: Blocker, client: Client[F], token: Option[String])(target: GithubDownloadTarget): F[ContentFileData] = {
    for {
      content <- Content.contentsFor[F](target.org, target.repo, target.repoPath, target.ref, token.map(OAuth(_))).run(client)
      fileContent <- content match {
        case ContentD.File(data) => data.pure[F]
        case ContentD.Directory(data) => new Throwable("Cannot Download Directory").raiseError
      }

    } yield fileContent
  }

  private def checkFileContent[F[_]: Concurrent : ContextShift](blocker: Blocker)(target: GithubDownloadTarget, fileContent: ContentFileData): F[Unit] = {
    for {
      wd <- Sync[F].delay(System.getProperty("user.dir"))
      currentFilePath <- Sync[F].delay(Paths.get(wd, target.localPath))
      _ <- fs2.io.file.exists(blocker, currentFilePath).ifM(
        Applicative[F].unit,
        new Throwable(s"Local Version of File $currentFilePath does not exist").raiseError
      )
      currentFileContent <- 
        fs2.io.file.readAll(currentFilePath,blocker, 4096) // 4kb
          .through(fs2.text.utf8Decode)
          .compile
          .string
      currentSha <- Sync[F].delay{
        // https://stackoverflow.com/questions/55678890/compute-github-api-file-sha
        val shaString: Array[Byte] = {
          s"blob ${currentFileContent.size}".getBytes() ++
          Array(0x00.toByte) ++
          currentFileContent.getBytes()
        }

        val md = java.security.MessageDigest.getInstance("SHA-1")
        val sha1Array = md.digest(shaString)
        scodec.bits.ByteVector(sha1Array).toHex
      }
      _ <- if (currentSha === fileContent.info.sha) {
        Applicative[F].unit 
      } else {
        new Throwable(s"Github file is different than locally stored file: $target").raiseError
      }
    } yield ()
  }

  def writeFileContent[F[_]: Concurrent : ContextShift](blocker: Blocker)(target: GithubDownloadTarget, fileContent: ContentFileData): F[Unit] = {
    val b64 = Base64.getMimeDecoder()
    val data = new String(b64.decode(fileContent.content))
    for {
      wd <- Sync[F].delay(System.getProperty("user.dir"))
      path <- Sync[F].delay(Paths.get(wd, target.localPath))
      _ <- fs2.io.file.exists(blocker, path.getParent()).ifM(
        Applicative[F].unit,
        fs2.io.file.createDirectories(blocker, path.getParent()).void
      )
      _ <- fs2.Stream(data)
          .through(fs2.text.utf8Encode)
          .through(
            fs2.io.file.writeAll(path, blocker, 
              List(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
            )
          ).compile.drain
    } yield ()
  }

}