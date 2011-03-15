
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

//import supplix.vaadin._

package scalaadin {
  case class CompWithRatio(component: Component, ratio: Option[Float], alignment: Option[Alignment])

  trait JobIds {
    private var lastJobId = 0
    private var lastJobThread: Thread = _
    
    def newAsyncJob[V](work: => V, finish: V => Unit) = this synchronized {
      if (lastJobThread != null)
        lastJobThread.interrupt()
        
      val jobId = lastJobId + 1
      lastJobId = jobId
      lastJobThread = new Thread { override def run {
        val v = work
        this synchronized {
          if (jobId == lastJobId) {
            lastJobThread = null
            finish(v)
          }
        }
      }}
      lastJobThread.start
    }
  }
  class Wrapper[T <: Component] extends VerticalLayout with JobIds {
    private var component: T = _
    private var lastJobId = -1
    private def indet {
      removeAllComponents
      add(this, (newIndeterminate, 1f))
    }
    indet
    def apply() = component
    def update(component: => T) = {
      indet
      newAsyncJob(component, (c: T) => if (c != null) {
        removeAllComponents
        this.component = c
        add(this, (c.fillSize, 1f))//, newXHTMLLabel("hehe"))
      })
    }
  }

}
package object scalaadin {  

  def wrapper(c: => Component) = {
    val w = new Wrapper[Component]
    w() = c
    w
  }
  implicit def AbstractFieldExtensions(f: AbstractField) = new {
    def onValueChange(action: Any => Unit) = {
      f.setImmediate(true)
      f.addListener(new ValueChangeListener {
          override def valueChange(e: ValueChangeEvent) = {
            action(e.getProperty.getValue)
          }
        })
      f
    }
  }
  
  implicit def TextFieldExtensions(f: TextField) = new {
    def withSecret = {
      f.setSecret(true)
      f
    }
    def withSize(columns: Int, rows: Int = 1) = {
      if (columns > 0)
        f.setColumns(columns)
        
      if (rows != 1)
        f.setRows(rows)
      
      f
    }
    
    def withPrompt(txt: String) = {
      f.setInputPrompt(txt)
      f
    }
    def onEdit(action: String => Unit) = {
      f.onValueChange(v => action(v.toString))
    }
  }
  
  implicit def ComponentExtensions[C <: Component](c: C) = new {
    
    def withStyle(name: String) = {
      c.addStyleName(name)
      c
    }
    def withWidth(width: String) = {
      c.setWidth(width)
      c
    }
    def withHeight(height: String) = {
      c.setHeight(height)
      c
    }
    
    def onClick(action: => Unit): C = onClick(_ => action)
    def onClick(action: MouseEvents.ClickEvent => Unit): C = {
      c.addListener(new MouseEvents.ClickListener {
        override def click(event: MouseEvents.ClickEvent) = {
          action(event)
        }
      }.asInstanceOf[Component.Listener])
      c
    }
    def disabled = {
      c.setEnabled(false)
      c
    }
    def withCaption(txt: String): C = {
      c.setCaption(txt)
      c
    }
    
    def fillSize = {
      c.setSizeFull
      c
    }
    def fillWidth = {
      c.setWidth("100%")
      c
    }
    def fillHeight = {
      c.setHeight("100%")
      c
    }
    def tab: TabSheet.Tab = {
      var comp: Component = c
      while (comp != null) {
        var par = comp.getParent
        if (par.isInstanceOf[TabSheet]) {
          return par.asInstanceOf[TabSheet].getTab(comp)
        }
        comp = par
      }
      null
    }
    
    def tabCaption = {
      val t = tab
      if (t != null)
        t.getCaption
      else
        null
    }
    def tabCaption_=(caption: String) = {
      val t = tab
      if (t != null)
        t.setCaption(caption)
    }
    
    def tabIcon = tab.getIcon
    def tabIcon_=(icon: Resource) = tab.setIcon(icon)
  }
  implicit def LabelExtensions[L <: Label](l: L) = new {
    def xhtml = l.getValue.toString
    def xhtml_=(txt: String) = {
      l.setContentMode(Label.CONTENT_XHTML)
      l.setValue(txt)
    }
  }
  implicit def LayoutExtensions[C <: Layout](c: C) = new {
    def withMargin = { 
      c.setMargin(true)
      c
    }
    def withHMargin = { 
      c.setMargin(false, true, false, true)
      c
    }
    def withVMargin = { 
      c.setMargin(true, false, true, false)
      c
    }
    def withRightMargin = { 
      c.setMargin(false, true, false, false)
      c
    }
    def withLeftMargin = { 
      c.setMargin(false, false, false, true)
      c
    }
  }
  implicit def Table2RichTable(t: Table) = new RichTable(t)
  
  def newTextField(caption: String = null, value: String = null, columns: Int = -1, rows: Int = 1) = {
    new TextField(caption, if (value == null) "" else value).withSize(columns, rows)
  }
  def newTable(data: Traversable[Object] = null) = {
    val table = new Table
    table.setImmediate(true); // react at once when something is selected
    table.setColumnReorderingAllowed(true);
    //table.setSizeFull
    if (data != null)
        table.data = data
    table                    
  }

  def openInNewWindow(parentWindow: Window, title: String, component: Component, width: Int, height: Int) = {
    lazy val window: Window = new Window(title) {
      addComponent(component)
      component.setSizeFull
      addListener(new Window.CloseListener() {
        override def windowClose(e: Window#CloseEvent) =
          parentWindow.getApplication.removeWindow(window)
      })
    }
    parentWindow.getApplication.addWindow(window)

    // Get the URL for the window, and open that in a new
    // browser window, in this case in a small window.
    parentWindow.open(
        new ExternalResource(window.getURL), // URL
        "_blank",
        width, 
        height,
        Window.BORDER_NONE
    )
    window
  }
  
  implicit def ButtonExtensions[B <: Button](b: B) = new {
    import com.vaadin.event.ShortcutAction.{KeyCode, ModifierKey}
    
    def withPrimaryEnterShortcut = {
      withClickShortcut(KeyCode.ENTER)
      withPrimaryStyle
      b
    }
    def withPrimaryStyle = {
      b.withStyle("primary")
      b
    }
    def withClickShortcut(keyCode: Int, modifiers: Int*) = {
      b.setClickShortcut(keyCode, modifiers.toArray:_*)
      b
    }
    
    def onClick(action: => Unit) = {
      b.addListener(new Button.ClickListener {
        override def buttonClick(e: Button#ClickEvent) = action
      })
      b
    }
  }
  
  implicit def c2cw2(c: Component) = new CompWithRatio(c, None, None)
  implicit def cf2cw2(cf: (Component, Float)) = new CompWithRatio(cf._1, Some(cf._2), None)
  implicit def cfa2cw2(cf: (Component, Float, Alignment)) = new CompWithRatio(cf._1, Some(cf._2), Some(cf._3))
  implicit def ca2cw2(cf: (Component, Alignment)) = new CompWithRatio(cf._1, None, Some(cf._2))
  
  def add(cont: AbstractOrderedLayout,components: CompWithRatio*) = {
    for (CompWithRatio(c, r, a) <- components.filter(_ != null)) {
      if (c != null) {
        cont.addComponent(c)
        if (r != None)
          cont.setExpandRatio(c, r.get)
        if (a != None)
          cont.setComponentAlignment(c, a.get)
      }
    }
  }
  def newVertical(components: CompWithRatio*) = {
    val l = new VerticalLayout()
    l.setSpacing(true)
    //l.setSizeFull
    add(l, components:_*)
    l
  }
  def newHorizontal(components: CompWithRatio*) = {
    val l = new HorizontalLayout()
    l.setSpacing(true)
    //l.setSizeFull
    add(l, components:_*)
    l
  }
  def newVerticalSplitPanel(a: Component, b: Component, ratio: Double = 0.5) = {
    val l = new VerticalSplitPanel
    l.addComponent(a)
    l.addComponent(b)
    a.setSizeFull
    b.setSizeFull
    l.setSplitPosition((ratio * 100).toInt) // percent
    l
  }
  def newHorizontalSplitPanel(a: Component, b: Component, ratio: Double = 0.5) = {
    val l = new HorizontalSplitPanel
    l.addComponent(a)
    l.addComponent(b)
    a.setSizeFull
    b.setSizeFull
    l.setSplitPosition((ratio * 100).toInt) // percent
    l
    /*val l = newVerticalSplitPanel(a, b, ratio)
    l.setOrientation(SplitPanel.ORIENTATION_HORIZONTAL)
    l*/
  }
  
  def newLink(url: URL, caption: String = null) =
    new Link(if (caption == null) url.toString else caption, new ExternalResource(url.toString)) 
  
  def newLinkButton(caption: String)(action: => Unit) =
    newButton(caption)(action).withStyle("link")
    
  def newButton(caption: String)(action: => Unit) =
    new Button(caption).onClick(action).withStyle("v-button-handcursor")

  def newCheckBox(caption: String, checked: Boolean = false)(action: Boolean => Unit) = {
    val cb = new CheckBox(caption)
    cb.setImmediate(true)
    cb.onClick(action(cb.booleanValue))
  }

  def newLazyPanel(contentByName: => Component) = new VerticalLayout {
    println("Creating a lazy panel")
    lazy val content = {
      println("Adding real content to lazy panel")
      val c = contentByName
      removeAllComponents
      addComponent(c)
      setExpandRatio(c, 1f)
      c
    }
    private def realize: Unit = content
    
    addListener(new Component.Listener {
      override def componentEvent(event: Component.Event) = {
        println("LazyPanel receive event = " + event)
        realize
      }
    })
    addComponent(newButton("Load") { realize })
  }
  
  def newModalWindow(caption: String, init: Window => Unit = null) = {
    val w = new Window(caption)
    w.addListener(
      new Window.CloseListener {
        override def windowClose(e: Window#CloseEvent) = {
          if (w.getParent != null)
            w.getParent.asInstanceOf[Window].removeWindow(w)
        }
      }
    )
    w.setModal(true)
    if (init != null)
      init(w)
    w
  }
  def newXHTMLLabel(content: String) = {
    val l = new Label(content)
    l.setContentMode(Label.CONTENT_XHTML)
    l
  }
  def newProgress = 
    new ProgressIndicator
  
  def newIndeterminate: ProgressIndicator = {
    val p = newProgress
    p.setIndeterminate(true)
    p
  }
  
  //implicit def FiltrableTable2Table[T](ft: FiltrableTable[T]): Table = ft.table
  implicit def FiltrableTable2RichTable[T <: AnyRef](ft: FiltrableTable[T]): RichTable = RichTable(ft.table)
  
  def newTreeTable[T <: AnyRef](title: String, columns: Seq[Column[T]], groups: List[Group[T]]) =
    GroupedTreeTable[T](title, columns, groups)
    
  def newFiltrableTable[T <: AnyRef](
    title: String, 
    //dataItems: Seq[T] = null,
    extraBoolFilters: Seq[(String, Boolean, String)] = Seq(), 
    filterProperty: String = "filter", 
    hideFilterColumn: Boolean = true
  )(implicit t: ClassManifest[T]): FiltrableTable[T] = 
    new FiltrableTable[T](title, extraBoolFilters, filterProperty, hideFilterColumn, t)
  
  def newFiltrableTableData[T <: AnyRef](title: String, dataItems: Seq[T])(implicit t: ClassManifest[T]): FiltrableTable[T] = {
    val tab = newFiltrableTable[T](title)(t)
    tab.data = dataItems
    tab
  }
  
  def newTabSheet(tabs: (String, Component)*) = {
    val t = new TabSheet
    for ((title, c) <- tabs.filter(_ != null)) {
        c.setSizeFull
        t.addTab(c, title, null)
    }
    t
  }
  def newPropertiesItem(properties: Map[String, String]) = {
    val props = new java.util.Properties
    props.putAll(properties)
    /*props.setProperty("Name", "John Doe")
    props.setProperty("Age(int)", "39")
    props.setProperty("DOA(boolean)", "false")
    props.setProperty("Arrived(date)", "")
    props.setProperty("Notes", "")*/
    val item = new org.vaadin.data.PropertiesItem(props)
        
    val f = new Form()
    f.setItemDataSource(item)
    f
  }
  
  def newLazyTabSheet(tabs: (String, () => Component)*) = {
    val t = new LazyTabSheet
    for ((title, compCreator) <- tabs.filter(_ != null))
      t.addLazyTab(title, compCreator())
    t
  }
}
