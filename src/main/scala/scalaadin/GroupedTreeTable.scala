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

trait TabTitleUpdated {
  this: Component =>
  val title: String
  def dataUpdated(size: Option[Int]) = size match {
    case None | Some(0) =>
      this.tabCaption = title
    case Some(1) =>
      this.tabCaption = title + " (1 item)"
    case Some(n) =>
      this.tabCaption = title + " (" + n + " items)"
  }
}
case class GroupedTreeTable[T](title: String, columns: Seq[Column[T]], groups: List[Group[T]]) 
extends TreeTable
   with TabTitleUpdated 
{ 
  setFooterVisible(true)
  setSelectable(true)
  setMultiSelect(true)
  setImmediate(true)
  
  private var dataItems = Seq[T]()
  def data: Seq[T] = dataItems
  def data_=(dataItems: Seq[T]): Unit = {
    this.dataItems = if (dataItems == null) Seq() else dataItems
    setContainerDataSource(new TreeDataSource(this.dataItems, columns, groups))
      
    columns.map(c => {
      val v = c.get(this.dataItems)
      setColumnFooter(c.name, if (v == null) "" else v.toString)
    })
    dataUp
  }
  private def dataUp = dataUpdated(Option(dataItems).map(_.size))
  
  override def attach: Unit = {
    super.attach
    dataUp
  }
  
  data = Seq()
}

