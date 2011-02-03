import sbt._
class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  //val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  //val sbtIdea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.1-SNAPSHOT"
  
  val sbtVaadin = "fi.jawsy" % "sbt-vaadin-plugin" % "0.1.4-SNAPSHOT" from "https://github.com/ochafik/sbt-vaadin-plugin/raw/master/sbt-vaadin-plugin-0.1.4-SNAPSHOT.jar"
  
  // https://github.com/nuttycom
  val proguard = "org.scala-tools.sbt" % "sbt-proguard-plugin" % "0.0.5"
}
