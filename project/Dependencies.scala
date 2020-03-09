import sbt._

object Dependencies {

  object Version {
    val silhouette = "7.0.0"
    val playVersion = play.core.PlayVersion.current
    val playMailerVersion = "8.0.0"
  }

  val resolvers = Seq(
    Resolver.jcenterRepo
  )

  val common = Seq(
    "com.mohiva" %% "play-silhouette" % Version.silhouette,
    "com.mohiva" %% "play-silhouette-password-bcrypt" % Version.silhouette,
    "com.mohiva" %% "play-silhouette-persistence" % Version.silhouette,
    "com.mohiva" %% "play-silhouette-crypto-jca" % Version.silhouette,
    "com.mohiva" %% "play-silhouette-testkit" % Version.silhouette,

    "com.typesafe.play" %% "play-mailer" % Version.playMailerVersion,
    "com.typesafe.play" %% "play-mailer-guice" % Version.playMailerVersion,
    "com.enragedginger" %% "akka-quartz-scheduler" % "1.8.3-akka-2.6.x",

    "com.iheart" %% "ficus" % "1.4.3",
    "net.codingwell" %% "scala-guice" % "4.2.0",

    "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3",

    "com.fijimf.deepfij" %% "fijbook-schedule-lib" % "1.1.1",


    "org.webjars" %% "webjars-play" % "2.7.0",
    "org.webjars" % "bootstrap" % "4.1.2",
    "org.webjars" % "jquery" % "3.3.1-1",
    "org.webjars" % "font-awesome" % "5.2.0",
    "org.webjars.npm" % "feather-icons" % "4.7.3",
    "org.webjars" % "popper.js" % "1.14.4"

  )
}