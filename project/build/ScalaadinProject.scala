import sbt._

class SupplixProject(info: ProjectInfo) 
extends DefaultProject(info)
{
  // If you're using JRebel for Lift development, uncomment this line
  // override def scanDirectories = Nil

  val javaNet = "java.net" at "http://download.java.net/maven/2/"
  val jbossRepo = "jboss.org" at "https://repository.jboss.org/nexus/content/groups/public-jboss/"
  val vaadinSnapshots = "vaadin-snapshots" at "http://oss.sonatype.org/content/repositories/vaadin-snapshots/"
  val vaadinReleases = "vaadin-releases" at "http://oss.sonatype.org/content/repositories/vaadin-releases/"
  val vaadinAddons = "vaadin-addons" at "http://maven.vaadin.com/vaadin-addons"

  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  
  val junitDeps = Set(
    "junit" % "junit" % "4.7" % "test->default",
    "com.novocode" % "junit-interface" % "0.4" % "test->default"
  )
  
  val vaadinVersion = "6.5.3"
  val gwtVersion = "2.1.0"

  override def libraryDependencies = 
    Set(
      "com.google.gwt" % "gwt-dev" % gwtVersion,
      "com.google.gwt" % "gwt-user" % gwtVersion,
      "com.google.gwt.google-apis" % "gwt-visualization" % "1.1.0" 
          from "jar:http://gwt-google-apis.googlecode.com/files/gwt-visualization-1.1.0.zip!/gwt-visualization-1.1.0/gwt-visualization.jar",
      "com.google.gwt.google-apis" % "gwt-maps" % "1.1.0" 
          from "jar:http://gwt-google-apis.googlecode.com/files/gwt-maps-1.1.0.zip!/gwt-maps-1.1.0/gwt-maps.jar",
      
      "com.vaadin" % "vaadin" % vaadinVersion % "compile->default",
      
      "com.vaadin.addon" % "beanvalidation-addon" % "0.6.1",
      "org.hibernate" % "hibernate-validator" % "4.2.0.Beta2",
      
      //"org.vaadin.addons" % "confirmdialog" % "1.0.1",
      "org.vaadin.addons" % "visualizationsforvaadin" % "1.1.1",
      "org.vaadin.addons" % "vaadin-sqlcontainer" % "0.8",
      "org.vaadin.addons" % "propertiesitem" % "0.9.1",
      "org.vaadin.addons" % "vaadin-treetable" % "1.0.0",
      "org.vaadin.addons" % "collectioncontainer" % "0.9.2",
      "org.vaadin.addons" % "ckeditor-wrapper-for-vaadin" % "0.7",
      "org.vaadin.addons" % "maskedtextfield" % "0.1.1",
      "org.vaadin.addons" % "lazyloadwrapper" % "1.0",
      "org.vaadin.addons" % "activelink" % "1.0",
      "org.vaadin.addons" % "ffselect" % "1.0.1",
      "org.vaadin.addons" % "lazy-query-container" % "1.1.8", //http://vaadin.com/directory#addon/lazy-query-container
      "org.vaadin.addons" % "delayedbutton" % "1.0.1",
      "org.vaadin.addons" % "copytoclipboard" % "0.5.0",
      "org.vaadin.addons" % "tokenfield" % "1.0"
      //"org.vaadin.addons" % "canvaswidget" % "1.0.3", //http://vaadin.com/addon/canvaswidget
      //"org.vaadin.addons" % "processingsvg" % "1.0.4", //http://vaadin.com/addon/processingsvg
    ) ++ 
    junitDeps ++
    super.libraryDependencies
  
  override val artifactID = "scalaadin"
}

