package scalaadin

import scala.collection.JavaConversions._

//import com.vaadin.Application
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

case class FiltrableTable[T <: AnyRef](
  title: String, 
  extraBoolFilters: Seq[(String, Boolean, String)] = Seq(),
  filterProperty: String, 
  hideFilterColumn: Boolean,
  t: ClassManifest[T])
extends VerticalLayout 
   with TabTitleUpdated
{
  val table = newTable().fillSize
  val searchField = newTextField().withPrompt("Filter " + title)
  
  var fieldInfos = Map[String, TableColumn]()
  for (method <- t.erasure.getMethods) {
    val n = method.getName
    if (n.startsWith("get") && n.size != 3) {
      val fieldName = {
        val f = method.getName.substring(3)
        f.take(1).toLowerCase + f.drop(1)
      }
      fieldInfos = fieldInfos + (fieldName -> method.getAnnotation(classOf[TableColumn]))
    }
  }
  //println("fieldInfos = " + fieldInfos)
  
  protected var hiddenCols: Set[String] = 
    if (filterProperty != null && hideFilterColumn) 
      Set(filterProperty) 
    else 
      Set()
    
      /*
  def hideCols(names: String*) = {
    hiddenCols = hiddenCols ++ names
    doHideCols
    this
  }*/
  def sortedBy(name: String) = {
    table.sortedBy(name)
    this
  }
  /*
  private def doHideCols = {
    val cols = (fieldInfos.filter(_._2.hiddenByDefault).map(_._1) ++ hiddenCols).distinct
    table.hideCols(cols:_*)
  }*/
  
  def onItemClick(action: (ItemClickEvent, T) => Unit) = {
    table.onItemClick(action)
    this
  }
  def selection = table.selection[T]
  def onSingleSel(action: T => Unit) = {
    table.onSingleSel[T](action)
    this
  }
  def onMultiSel(action: Set[T] => Unit) = {
    table.onMultiSel[T](action)
    this
  }
  
  private var columnsOrderArr: Array[String] = null
  
  def columnsOrder(names: String*) = {
    columnsOrderArr = names.toArray
    updateColumnsOrder
    this
  }
  
  def hideCols(names: String*) = {
    table.hideCols(names:_*)
    updateColumnsOrder
    this
  }
  
  private def updateColumnsOrder = {
    //println("columnsOrderArr = " + Option(columnsOrderArr).map(_.toSeq))
    //println("table.getVisibleColumns = " + table.getVisibleColumns.toSeq)
    if (table.getContainerDataSource != null && columnsOrderArr != null) {
      val cols = columnsOrderArr.toSeq
      val vis = table.getVisibleColumns.map(_.toString).toSeq
      val arr = cols.filter(vis.contains(_)) ++ vis.filter(c => !cols.contains(c))
      
      //println("arr = " + arr)
      table.setColumnReorderingAllowed(true)
      table.setVisibleColumns(arr.toArray)
    }
  }
  
  private var dataItems = Iterable[T]()
  def data = dataItems
  def data_=(data: Iterable[T]): Unit = {
    val dat = if (data == null) IndexedSeq() else data.toIndexedSeq
    dataItems = dat
    if (dat.isEmpty) {
      table.setContainerDataSource(null)
    } else {
      table.setContainerDataSource(new BeanItemContainer(dat))
      for ((fieldName, tableColumn) <- fieldInfos.toSeq.sortBy(_._1)) {
        if (tableColumn != null) {
          if (tableColumn.hiddenByDefault()) {
            table.setColumnCollapsingAllowed(true);
            table.setColumnCollapsed(fieldName, true)
          }
          table.setColumnHeader(fieldName, tableColumn.value())
          table.setColumnAlignment(fieldName, tableColumn.alignment())
        }
      }
      actSearch
      //doHideCols
    }
    dataUp
    updateColumnsOrder
  }
  
  private var actualFooterData: T = _
  def footerData = actualFooterData
  def footerData_=(v: T) = {
    if (v == null) {
      table.setFooterVisible(false)
    } else {
      table.setFooterVisible(true)
      val bi = new BeanItem[T](v)
      for ((fieldName, tableColumn) <- fieldInfos.filter(_._2.hasFooter)) {
        table.setColumnFooter(fieldName, Option(bi.getItemProperty(fieldName).getValue).map(_.toString).orNull)
      }
    }
  }
  /*
  val footerData = Var[T]() listen (v => {
    if (v == null) {
      table.setFooterVisible(false)
    } else {
      table.setFooterVisible(true)
      val bi = new BeanItem[T](v)
      for ((fieldName, tableColumn) <- fieldInfos.filter(_._2.hasFooter)) {
        table.setColumnFooter(fieldName, Option(bi.getItemProperty(fieldName).getValue).map(_.toString).orNull)
      }
    }
  })*/
  
  private var renameTab = false
  def withTabRenaming = {
    renameTab = true
    dataUp
    this
  }
  private def dataUp = if (renameTab) dataUpdated(Option(dataItems).map(_.size))
  
  override def attach: Unit = {
    super.attach
    dataUp
  }
  
  //searchField.setDebugId("filt-" + title.replace(' ', '_'))
  
  val showAll = newButton("Show All") { searchField.setValue("") }
  showAll.setEnabled(false)
  
  val extraCheckboxes = extraBoolFilters.map { case (caption, checked, property) =>
    val cb = new CheckBox(caption, checked)
    cb.setImmediate(true)
    (property, cb)
  }.toMap
  
  def actSearch(): Unit = {
    val txt = searchField.getValue.toString
    showAll.setEnabled(txt.trim != "")
    
    val cont = table.getContainerDataSource.asInstanceOf[Container.Filterable]
    cont.removeAllContainerFilters
    for (word <- txt.split(' '))
      cont.addContainerFilter(filterProperty, word, true, false)
    for ((property, cb) <- extraCheckboxes)
      if (cb.booleanValue)
        cont.addContainerFilter(property, "true", true, false)
  }
  
  extraCheckboxes.map(_._2.onClick(actSearch))
  searchField.onEdit(_ => actSearch)
  val hor = newHorizontal(
    List[CompWithRatio](
      (searchField.fillWidth, 1f)
    ) ++ 
    extraCheckboxes.map(_._2: CompWithRatio).toSeq ++
    List[CompWithRatio](
      newButton("Search")(actSearch),
      showAll
    ):_*
  )
  extraCheckboxes.map(cb => hor.setComponentAlignment(cb._2, Alignment.MIDDLE_LEFT))
  
  table.addListener(new Container.PropertySetChangeListener {
    override def containerPropertySetChange(event: Container.PropertySetChangeEvent) = 
      actSearch
  })
  actSearch
  
  add(
    this,
    (table, 1f),
    hor.fillWidth
  )
}



case class RichTable(table: Table) {

  def data: Traversable[Object] = table.getContainerDataSource.asInstanceOf[BeanItemContainer[Object]].getItemIds
  def data_=(collection: Traversable[Object]): Unit = {
    val col = if (collection == null) IndexedSeq() else collection.toIndexedSeq
    if (col.isEmpty)
      table.setContainerDataSource(null)
    else
      table.setContainerDataSource(new BeanItemContainer(col))
  }
  private[scalaadin] def hideCols(names: String*) = {
    table.setColumnCollapsingAllowed(true);
    if (table.getContainerDataSource != null)
      for (name <- names)
        table.setColumnCollapsed(name, true)
        
    table
  }
  
  def sortedBy(name: String) = {
    table.setSortContainerPropertyId(name)
    table
  }

  def onItemClick[I](action: (ItemClickEvent, I) => Unit) = {
    table.setSelectable(true)
    table.addListener(new ItemClickEvent.ItemClickListener {
      override def itemClick(e: ItemClickEvent) = action(e, e.getItem.asInstanceOf[BeanItem[I]].getBean)
    })
    table
  }
  
  def selection[I] = if (table.isMultiSelect)
    table.getValue.asInstanceOf[java.util.Set[I]].toList
  else
    List(table.getValue.asInstanceOf[I])
  
  def onSingleSel[I](action: I => Unit) = {
    table.setSelectable(true)
    table.setMultiSelect(false)
    table.addListener(new ValueChangeListener {
        override def valueChange(event: ValueChangeEvent) {
          action(event.getProperty.getValue.asInstanceOf[I])
        }
      });
    table
  }
  def onMultiSel[I](action: Set[I] => Unit) = {
    table.setSelectable(true)
    table.setMultiSelect(true)
    table.addListener(new ValueChangeListener {
        override def valueChange(event: ValueChangeEvent) {
          val set = event.getProperty.getValue.asInstanceOf[java.util.Set[I]]
          action(set.toSet)
        }
      });
    table
  }
}
