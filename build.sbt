name := "portfolio"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.zaxxer" % "HikariCP" % "2.4.1",
  "org.mariadb.jdbc" % "mariadb-java-client" % "1.2.2",
  "org.scalikejdbc" %% "scalikejdbc"  % "2.2.9",
  "ch.qos.logback"  %  "logback-classic"   % "1.1.3",
  "joda-time" % "joda-time" % "2.9",
  "org.apache.commons" % "commons-csv" % "1.2",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)
