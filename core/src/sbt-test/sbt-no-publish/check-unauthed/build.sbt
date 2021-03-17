lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.13.0",
    organization := "askdfjakadjf",
    name := "fasdajhfdjah",
    githubDownloadTargets := List(
      GithubDownloadTarget("davenverse", "mapref", "README.md", "TESTREADME.md")
    )
  )