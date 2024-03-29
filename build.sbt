import sbt.Def
import sbt.Keys.scalacOptions
// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}


val udashVersion = "0.9.0"

val bootstrapVersion = "4.3.1"

val udashJQueryVersion = "3.0.4"

lazy val commonSettings = Seq(
  organization := "com.github.ondrejspanel",
  version := "0.1.0",
  scalaVersion := "2.13.10",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),

  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.2" % Test,
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.14.3" % Test,
  libraryDependencies += "org.scalatestplus" %%% "scalacheck-1-14" % "3.2.2.0" % Test,

  libraryDependencies += "io.udash" %%% "udash-core" % udashVersion,
  libraryDependencies += "io.udash" %%% "udash-rest" % udashVersion,
  libraryDependencies += "io.udash" %%% "udash-rpc" % udashVersion,
  libraryDependencies += "io.udash" %%% "udash-css" % udashVersion,

  libraryDependencies += "io.udash" %%% "udash-css" % udashVersion,

  libraryDependencies += "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.1.2"
)

lazy val jsCommonSettings = Seq(
  excludeDependencies += ExclusionRule(organization = "io.github.cquiroz") // workaround for https://github.com/cquiroz/scala-java-time/issues/257
)

lazy val jsLibs = libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  "org.querki" %%% "jquery-facade" % "2.1",

  "io.udash" %%% "udash-bootstrap4" % udashVersion,
  "io.udash" %%% "udash-jquery" % udashJQueryVersion,

  "com.zoepepper" %%% "scalajs-jsjoda" % "1.2.0",
  "com.zoepepper" %%% "scalajs-jsjoda-as-java-time" % "1.2.0"
)

lazy val sharedJs = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure).in(file("shared-js"))
  .jsConfigure(_.enablePlugins(JSDependenciesPlugin))
  .settings(commonSettings)
  .jvmSettings(
  ).jsSettings(
    jsCommonSettings,
    jsLibs,
    // "jquery.js" is provided by "udash-jquery" dependency
    jsDependencies += "org.webjars" % "bootstrap" % bootstrapVersion / "bootstrap.bundle.js" minified "bootstrap.bundle.min.js" dependsOn "jquery.js",
    jsDependencies += "org.webjars.npm" % "js-joda" % "1.10.1" / "dist/js-joda.js" minified "dist/js-joda.min.js",
    jsDependencies += "org.webjars.npm" % "js-joda-timezone" % "1.1.5" / "dist/js-joda-timezone.js" minified "dist/js-joda-timezone.min.js",
    jsDependencies += "org.webjars.npm" % "he" % "1.2.0" / "1.2.0/he.js"
  )

lazy val sharedJs_JVM = sharedJs.jvm
lazy val sharedJs_JS = sharedJs.js

def generateIndexTask(index: String, suffix: String) = Def.task {
  val source = baseDirectory.value / "index.html"
  val jsTarget = (Compile / fastOptJS / crossTarget).value / index
  val log = streams.value.log
  IO.writeLines(jsTarget,
    IO.readLines(source).map {
      line => line.replace("{{target-js}}", s"frontend-$suffix.js")
    }
  )

  log.info(s"Generate $source from $index with suffix: $suffix")
}

val generateCssTask = taskKey[Unit]("Copy CSS and JS files")

generateCssTask := Def.task {
  val log = streams.value.log
  import Path._
  // we need fastOptJS to execute first
  val dep = (frontend / Compile / fastOptJS).value
  val depCSS = (root / Compile / compile).value
  val src = (frontend / Compile / fastOptJS / crossTarget).value
  // https://stackoverflow.com/a/57994298/16673
  val jsFiles: Seq[File] = (src ** "*.css").get() ++ (src ** "*.js").get() ++ (src ** "*.js.map").get() ++ (src ** "*.html").get()
  val tgt = (Compile / crossTarget).value
  val pairs = jsFiles pair rebase(src, tgt)
  log.info(s"CSS/JS from $src to $tgt")
  // Copy files to source files to target
  IO.copy(pairs, CopyOptions.apply(overwrite = true, preserveLastModified = true, preserveExecutable = false))
}.value

def copyAssets() = Def.task {
  val log = streams.value.log
  import Path._
  val src = baseDirectory.value / "assets"
  val srcFiles: Seq[File] = (src ** "*").get()
  val tgt = (Compile / crossTarget).value
  val pairs = srcFiles pair rebase(src, tgt)
  log.info(s"Assets from $src to $tgt")
  // Copy files to source files to target
  IO.copy(pairs, CopyOptions.apply(overwrite = true, preserveLastModified = true, preserveExecutable = false))
}

lazy val frontend = project.settings(
    commonSettings,
    jsCommonSettings,
    jsLibs,
    //scalaJSUseMainModuleInitializer := true,
    //mainClass in Compile := Some("com.github.opengrabeso.cohabo.MainJS"),
    jsDependencies += ProvidedJS / "jQuery.resizableColumns.js" minified "jQuery.resizableColumns.min.js" dependsOn "jquery.js",

    (fastOptJS in Compile) := (fastOptJS in Compile).dependsOn(generateIndexTask("index-fast.html","fastopt"), copyAssets()).value,
    (fullOptJS in Compile) := (fullOptJS in Compile).dependsOn(generateIndexTask("index.html","opt"), copyAssets()).value
  ).enablePlugins(ScalaJSPlugin, JSDependenciesPlugin)
    .dependsOn(sharedJs_JS)

lazy val backend = (project in file("backend"))
  .dependsOn(sharedJs_JVM)
  .settings(
    libraryDependencies += "commons-io" % "commons-io" % "2.1",
    commonSettings
  )

lazy val root = (project in file("."))
  .aggregate(frontend, backend)
  .settings(
    name := "Cohubo",
    Compile / compile := Def.taskDyn {
      val doFastOptJS = (frontend / Compile / fastOptJS).value // the CSS and JS need to be produced first
      val c = (Compile / compile).value
      val log = streams.value.log
      val dir = (frontend / Compile / fastOptJS / crossTarget).value
      val path = dir.absolutePath
      log.info(s"Compile css in $dir")
      dir.mkdirs()
      Def.task {
        val doRunMain = (backend / Compile / runMain).toTask(s" com.github.opengrabeso.cohubo.CompileCss $path true").value
        c // return compile result
      }
    }.value,

    Compile / products := ((Compile / products).dependsOn(generateCssTask)).value
  )
