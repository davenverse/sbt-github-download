# sbt-github-download - SBT Github Download [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/sbt-github-download_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.chrisdavenport/sbt-github-download_2.12) ![Code of Conduct](https://img.shields.io/badge/Code%20of%20Conduct-Scala-blue.svg)

Have a source of truth that's in another github repository? Maybe OpenAPI specifications, Binary Protocols, etc? Just a few settings and CI will guarantee correctness between repositories.

## Quick Start

To use sbt-github-download in an existing SBT project, add the following dependencies to your
`project/plugins.sbt` depending on your needs:

```scala
addSbtPlugin(
  "io.chrisdavenport" %% "sbt-github-download" % "<version>"
)
```


## Settings 

This has relatively simple settings

`githubDownloadTargets` which is a setting of Seq[GithubDownloadTarget], each file will be able to download
and directly map to a local file. The class it aligns to is below

```scala
case class GithubDownloadTarget(
  org: String,
  repo: String,
  repoPath: String,
  localPath: String,
  ref: Option[String] = None
)
```

`githubDownloadToken` is the token to use to download the file. It is optional, if it is not present, it
will attempt to retrieve the file unauthed which will only work against public github repositories.

## Tasks

`githubDownloadExecute` is the task that will download and create the file locally from the remote
github repo. It will overwrite whatever is presently at that file location with the content.

`githubDownloadCheck` is a task that will get the github file and compare it to the file saved locally
at that location. It checks the sha1 hash of the two files to confirm that they are exactly the same, if they are not the task fails and informs you which file was not correct.

## Example

So lets say you have 1 file that you want to keep up to date between repositories. This can be any file, but I'll use a simple file here. We'll also assume in this example that you check this file into version control, so you want reproducible builds. Add this to your projects `build.sbt`

```sbt
githubDownloadTargets ++= Seq(
  GithubDownloadTarget(
    "davenverse", // owner
    "mules",  // repository
    "LICENSE", // relative path from base of github repository
    "licenses/MULES_LICENSE", // relative path from base of local repository
    None // ref, when None will use the repository default branch
  )
),

// If you want to use auth
// for example if you wanted to talk to a private repository
// Can not set at all if you can access your files via github publicly.
githubDownloadToken := scala.sys.env.get("GITHUB_TOKEN")
```

If the file is not already in the repository or you'd like to update the file execute, `sbt githubDownloadExecute`

Then in whatever your CI is you'd want to run a check to confirm that your file is correct `sbt githubDownloadCheck` which will fail your CI if it is not up to date.