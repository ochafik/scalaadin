package scalaadin

import scala.collection.JavaConversions._

import com.vaadin.data.util._
import com.vaadin.data._
import com.vaadin.terminal._
import com.vaadin.event._
import com.vaadin.data.Property
import com.vaadin.data.Property.ValueChangeListener
import com.vaadin.data.Property.ValueChangeEvent
import com.vaadin.ui._
import org.vaadin._
import com.vaadin.addon.treetable._
    
import scala.reflect._
import java.net.URL

class LazyTabSheet extends TabSheet {
  class LazyTab(val initialTitle: String, _content: => Component, val icon: Resource) extends VerticalLayout {
    lazy val content = {
      val c = _content
      addComponent(c)
      c.setSizeFull
      setExpandRatio(c, 1f)
      c
    }
  }
  private var lazyTabs = Seq[LazyTab]()
  /*def lazyTabs_=(ts: Seq[LazyTab]): Unit = {
    _lazyTabs.clear
    removeAllComponents
    for (tab <- ts)
      addLazyTab(tab)
  }*/
  def addLazyTab(tab: LazyTab): LazyTab = {
    if (lazyTabs.isEmpty)
      tab.content // realize content
      
    tab.setSizeFull
    addTab(tab, tab.initialTitle, tab.icon)
    lazyTabs = lazyTabs ++ Seq(tab)
    tab
  }
  def addLazyTab(initialTitle: String, content: => Component, icon: Resource = null): LazyTab = 
    addLazyTab(new LazyTab(initialTitle, content, icon))
  
  addListener(new TabSheet.SelectedTabChangeListener {
    override def selectedTabChange(event: TabSheet#SelectedTabChangeEvent) =
      Option(event.getTabSheet.getSelectedTab) collect {
        case t: LazyTab => t.content // force content to be realized and installed
      }
  })
}

