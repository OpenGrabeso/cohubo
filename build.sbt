import sbt.Def
import sbt.Keys.scalacOptions
// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


val udashVersion = "0.8.2"

val bootstrapVersion = "4.3.1"

val udashJQueryVersion = "3.0.1"

lazy val commonSettings = Seq(
  organization := "com.github.ondrejspanel",
  version := "0.0.1-alpha",
  scalaVersion := "2.12.10",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),

  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.1.0" % "test",

  libraryDependencies += "io.udash" %%% "udash-core" % udashVersion,
  libraryDependencies += "io.udash" %%% "udash-rest" % udashVersion,
  libraryDependencies += "io.udash" %%% "udash-rpc" % udashVersion,
  libraryDependencies += "io.udash" %%% "udash-css" % udashVersion
)

lazy val jsCommonSettings = Seq(
  scalacOptions ++= Seq("-P:scalajs:sjsDefinedByDefault")
)

lazy val jsLibs = libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.7",
  "org.querki" %%% "jquery-facade" % "1.2",

  "io.udash" %%% "udash-bootstrap4" % udashVersion,
  "io.udash" %%% "udash-charts" % udashVersion,
  "io.udash" %%% "udash-jquery" % udashJQueryVersion,

  "com.zoepepper" %%% "scalajs-jsjoda" % "1.1.1",
  "com.zoepepper" %%% "scalajs-jsjoda-as-java-time" % "1.1.1"
)

lazy val sharedJs = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure).in(file("shared-js"))
  .settings(commonSettings)
  .jvmSettings(

  ).jsSettings(
    jsCommonSettings,
    jsLibs,
    // "jquery.js" is provided by "udash-jquery" dependency
    jsDependencies += "org.webjars" % "bootstrap" % bootstrapVersion / "bootstrap.bundle.js" minified "bootstrap.bundle.min.js" dependsOn "jquery.js",
    jsDependencies += "org.webjars.npm" % "js-joda" % "1.10.1" / "dist/js-joda.js" minified "dist/js-joda.min.js"
  )

lazy val sharedJs_JVM = sharedJs.jvm
lazy val sharedJs_JS = sharedJs.js

def generateIndexTask(index: String, suffix: String) = Def.task {
  val source = baseDirectory.value / "index.html"
  val jsTarget = (Compile / fastOptJS / crossTarget).value / index
  val log = streams.value.log
  IO.writeLines(jsTarget,
    IO.readLines(source).map {
      line => line.replace("{{target-js}}", s"cohubo-$suffix.js")
    }
  )

  log.info(s"Generate $index with suffix: $suffix")
}

val generateCssTask = taskKey[Unit]("Copy CSS and JS files to the output.")

generateCssTask := Def.task {
  val log = streams.value.log
  import Path._
  // we need fastOptJS to execute first
  val dep = (frontend / Compile / fastOptJS).value
  val depCSS = (root / Compile / compile).value
  val srcJS = (frontend / Compile / fastOptJS / crossTarget).value
  val src = (Compile / fastOptJS / target).value / "styles"
  // https://stackoverflow.com/a/57994298/16673
  val cssFiles: Seq[File] = (src ** "*.css").get()
  val jsFiles: Seq[File] = (srcJS ** "*.js").get() ++ (srcJS ** "*.js.map").get() ++ (srcJS ** "*.html").get()
  val tgt = (Compile / crossTarget).value
  val pairs = (cssFiles pair rebase(src, tgt)) ++ (jsFiles pair rebase(srcJS, tgt))
  log.info(s"CSS from $src to $tgt")
  log.info(s"JS from $srcJS to $tgt")
  // Copy files to source files to target
  IO.copy(pairs, CopyOptions.apply(overwrite = true, preserveLastModified = true, preserveExecutable = false))
}.value

lazy val frontend = project.settings(
    name := "Cohubo",
    commonSettings,
    jsCommonSettings,
    jsLibs,
    //scalaJSUseMainModuleInitializer := true,
    //mainClass in Compile := Some("com.github.opengrabeso.cohabo.MainJS"),

    (fastOptJS in Compile) := (fastOptJS in Compile).dependsOn(generateIndexTask("index-fast.html","fastOpt")).value,
    (fullOptJS in Compile) := (fullOptJS in Compile).dependsOn(generateIndexTask("index.html","opt")).value
  ).enablePlugins(ScalaJSPlugin)
    .dependsOn(sharedJs_JS)

lazy val backend = (project in file("backend"))
  .dependsOn(sharedJs_JVM)
  .settings(
    name := "CohuboJVMBuild",
    libraryDependencies += "commons-io" % "commons-io" % "2.1",
    commonSettings
  )

lazy val root = (project in file("."))
  .aggregate(frontend, backend)
  .settings(
    Compile / compile := Def.taskDyn {
      (frontend / Compile / fastOptJS).value // the CSS and JS need to be produced first
      val c = (Compile / compile).value
      val log = streams.value.log
      val dir = (Compile / fastOptJS / target).value / "styles"
      val path = dir.absolutePath
      log.info(s"Compile css in $dir($path)")
      dir.mkdirs()
      Def.task {
        (backend / Compile / runMain).toTask(s" com.github.opengrabeso.cohubo.CompileCss $path true").value
        c // return compile result
      }
    }.value,

    Compile / products := (Compile / products dependsOn generateCssTask).value
  )