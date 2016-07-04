name := "portfolio"

version := "1.0"

scalaVersion := "2.11.7"

mainClass in (Compile,run) := Some("portfolio.server")

libraryDependencies ++= Seq(
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "org.mariadb.jdbc" % "mariadb-java-client" % "1.2.2",
  "org.scalikejdbc" %% "scalikejdbc"  % "2.2.9",
  "ch.qos.logback"  %  "logback-classic"   % "1.1.3",
  "joda-time" % "joda-time" % "2.9",
  "org.apache.commons" % "commons-csv" % "1.2",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalatra" %% "scalatra" % "2.3.0",
  "org.scalatra" %% "scalatra-json" % "2.3.0",
  "org.eclipse.jetty" % "jetty-webapp" % "9.2.10.v20150310",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.json4s" %% "json4s-ext" % "3.2.9",
  "org.json4s"   %% "json4s-jackson" % "3.2.9",
  "org.json4s"   %% "json4s-native" % "3.2.9"
)
