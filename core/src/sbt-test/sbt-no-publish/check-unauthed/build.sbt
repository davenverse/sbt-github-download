lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.13.0",
    organization := "askdfjakadjf",
    name := "fasdajhfdjah",
    githubDownloadTargets := List(
      GithubDownloadTarget("http4s", "http4s", "README.md", "TESTREADME.md")
    )
  )