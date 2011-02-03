package scalaadin

import scala.collection.JavaConversions._

import com.vaadin.addon.treetable.Collapsible
import com.vaadin.data.Container.Indexed
import com.vaadin.data.Item
import com.vaadin.data.Property
import scala.collection.mutable.ArrayBuffer

trait NotImpl {
  protected def notImpl = error("not implemented")
}
case class Column[T](name: String, get: Seq[T] => Any, set: (T, Any) => Boolean, kind: Class[_]) {
  def this(name: String, get: Seq[T] => Any, set: (T, Any) => Boolean = null)(implicit t: ClassManifest[T]) =
    this(name, get, set, t.erasure)  
}
case class Group[T](name: String, groups: Seq[T] => Seq[(Any, Seq[T])])

class ValueItem[T](val parent: ValueItem[T], val data: Any, val values: Seq[T], val groups: List[Group[T]], columnsByName: Map[String, Column[T]]) 
extends Item 
   with NotImpl
{
  var collapsed = true
  
  override def addItemProperty(id: Object, property: Property) = notImpl
  override def getItemProperty(id: Object) = new ColumnProperty(columnsByName(id.asInstanceOf[String]), values)
  override def getItemPropertyIds = columnsByName.keys
  override def removeItemProperty(id: Object) = notImpl

  lazy val children: Seq[ValueItem[T]] = if (groups.isEmpty) {
    if (values.size <= 1)
      Nil
    else
      values.map(value => new ValueItem(this, null, Seq(value), Nil, columnsByName))
  } else {
    val ch = groups.head.groups(values)
    //println("ch = " + ch)
    ch.map { case (g, vs) => new ValueItem(this, g, vs, groups.tail, columnsByName) }
  }

  override def toString = data + " : " + values.mkString(", ")
  override def equals(o: Any) = o != null && o.isInstanceOf[ValueItem[T]] && o.asInstanceOf[ValueItem[T]].values.equals(values)
}
class ColumnProperty[T](column: Column[T], values: Seq[T]) 
extends Property
   with NotImpl 
{
  override def getType = column.kind
  override def getValue = column.get(values).asInstanceOf[AnyRef]
  override def isReadOnly = column.set != null
  override def setReadOnly(newStatus: Boolean) = notImpl
  override def setValue(newValue: Object) = values.foreach(value => column.set(value, newValue))
  override def toString = {
    val v = getValue
    if (v == null)
      ""
    else
      v.toString
  }
}

class TreeDataSource[T](data: => Seq[T], columns: Seq[Column[T]], groups: List[Group[T]])
extends Indexed
   with Collapsible
   with NotImpl
{  
  private val columnsByName = columns.map(c => (c.name, c)).toMap

  
  //private val itemsIds = data
  lazy val items = data
  private lazy val root = new ValueItem(null, null, items, groups, columnsByName)
  var filter: Seq[T] => Boolean = null//() => true

  /**
   * Tests if the specified Item in the container may have children. Since a
   * <code>FileSystemContainer</code> contains files and directories, this
   * method returns <code>true</code> for directory Items only.
   *
   * @param itemId
   *            the id of the item.
   * @return <code>true</code> if the specified Item is a directory,
   *         <code>false</code> otherwise.
   */
  override def areChildrenAllowed(itemId: Object) =
    hasChildren(itemId)
    //true//itemId.isInstanceOf[ValueItem] && (itemId.asInstanceOf[ValueItem].data == null || itemId.asInstanceOf[ValueItem].groups != Nil)

  /*
   * Gets the ID's of all Items who are children of the specified Item. Don't
   * add a JavaDoc comment here, we use the default documentation from
   * implemented interface.
   */
  override def getChildren(itemId: Object) =
    itemId.asInstanceOf[ValueItem[T]].children

  /*
   * Gets the parent item of the specified Item. Don't add a JavaDoc comment
   * here, we use the default documentation from implemented interface.
   */
  override def getParent(itemId: Object) =
    itemId.asInstanceOf[ValueItem[T]].parent

  /*
   * Tests if the specified Item has any children. Don't add a JavaDoc comment
   * here, we use the default documentation from implemented interface.
   */
  override def hasChildren(itemId: Object) =
    !itemId.asInstanceOf[ValueItem[T]].children.isEmpty

  /*
   * Tests if the specified Item is the root of the filesystem. Don't add a
   * JavaDoc comment here, we use the default documentation from implemented
   * interface.
   */
  override def isRoot(itemId: Object) =
    itemId.asInstanceOf[ValueItem[T]].parent == null

  /*
   * Gets the ID's of all root Items in the container. Don't add a JavaDoc
   * comment here, we use the default documentation from implemented
   * interface.
   */
  override def rootItemIds =
    root.children

  /**
   * Returns <code>false</code> when conversion from files to directories is
   * not supported.
   *
   * @param itemId
   *            the ID of the item.
   * @param areChildrenAllowed
   *            the boolean value specifying if the Item can have children or
   *            not.
   * @return <code>true</code> if the operaton is successful otherwise
   *         <code>false</code>.
   * @throws UnsupportedOperationException
   *             if the setChildrenAllowed is not supported.
   */
  override def setChildrenAllowed(itemId: Object, areChildrenAllowed: Boolean) = notImpl

  /**
   * Returns <code>false</code> when moving files around in the filesystem is
   * not supported.
   *
   * @param itemId
   *            the ID of the item.
   * @param newParentId
   *            the ID of the Item that's to be the new parent of the Item
   *            identified with itemId.
   * @return <code>true</code> if the operation is successful otherwise
   *         <code>false</code>.
   * @throws UnsupportedOperationException
   *             if the setParent is not supported.
   */
  override def setParent(itemId: Object, newParentId: Object) = notImpl

  /*
   * Tests if the filesystem contains the specified Item. Don't add a JavaDoc
   * comment here, we use the default documentation from implemented
   * interface.
   */
  override def containsId(itemId: Object) =
    itemId.isInstanceOf[ValueItem[T]] && (filter == null || filter(itemId.asInstanceOf[ValueItem[T]].values))

  /*
   * Gets the specified Item from the filesystem. Don't add a JavaDoc comment
   * here, we use the default documentation from implemented interface.
   */
  override def getItem(itemId: Object) =
    itemId.asInstanceOf[ValueItem[T]]

  /*
   * Gets the IDs of Items in the filesystem. Don't add a JavaDoc comment
   * here, we use the default documentation from implemented interface.
   */
  override def getItemIds = notImpl

  /**
   * Gets the specified property of the specified file Item. The available
   * file properties are "Name", "Size" and "Last Modified". If propertyId is
   * not one of those, <code>null</code> is returned.
   *
   * @param itemId
   *            the ID of the file whose property is requested.
   * @param propertyId
   *            the property's ID.
   * @return the requested property's value, or <code>null</code>
   */
  override def getContainerProperty(itemId: Object, propertyId: Object) =
    itemId.asInstanceOf[ValueItem[T]].getItemProperty(propertyId)

  override def addContainerProperty(itemId: Object, cl: Class[_], propertyId: Object) = notImpl

  /**
   * Gets the collection of available file properties.
   *
   * @return Unmodifiable collection containing all available file properties.
   */
  override def getContainerPropertyIds =
    columns.map(_.name)

  /**
   * Gets the specified property's data type. "Name" is a <code>String</code>,
   * "Size" is a <code>Long</code>, "Last Modified" is a <code>Date</code>. If
   * propertyId is not one of those, <code>null</code> is returned.
   *
   * @param propertyId
   *            the ID of the property whose type is requested.
   * @return data type of the requested property, or <code>null</code>
   */
  override def getType(propertyId: Object) =
    columnsByName(propertyId.asInstanceOf[String]).kind

  /**
   * Gets the number of Items in the container. In effect, this is the
   * combined amount of files and directories.
   *
   * @return Number of Items in the container.
   */
  override def size = getPreorder.size

  override def addItem = notImpl
  override def addItem(itemId: Object) = notImpl

  override def removeAllItems = notImpl

  /*
   * (non-Javadoc)
   *
   * @see com.vaadin.data.Container#removeItem(java.lang.Object)
   */
  override def removeItem(itemId: Object) = notImpl
  override def removeContainerProperty(propertyId: Object) = notImpl

  override def isCollapsed(itemId: Object) =
    !itemId.asInstanceOf[ValueItem[T]].collapsed

  override def setCollapsed(itemId: Object, visible: Boolean) = {
    val item = itemId.asInstanceOf[ValueItem[T]]
    item.collapsed = !visible//true//!item.collapsed
    preorder = null
  }

  override def addItemAfter(previousItemId: Object) = notImpl
  override def addItemAfter(previousItemId: Object, newItemId: Object) = notImpl

  override def firstItemId = root.children.head

  override def isFirstId(itemId: Object) = itemId.equals(firstItemId)

  override def isLastId(itemId: Object) = itemId.equals(lastItemId)

  def unroll(value: ValueItem[T], acc: ArrayBuffer[ValueItem[T]]): Unit = {
    value.children.foreach(c => {
      acc += c  
      if (!c.collapsed)
        unroll(c, acc)
    })
  }
  var preorder: IndexedSeq[ValueItem[T]] = _
  def getPreorder = {
    if (preorder == null) {
      val ret = new ArrayBuffer[ValueItem[T]]((items.size * 3) / 2)
      unroll(root, ret)
      preorder = ret.result
      //println("preorder = \n\t" + preorder.mkString("\n\t"))
    }
    preorder
  }

  override def lastItemId() =
    getPreorder.get(size - 1)

  override def nextItemId(itemId: Object) = {
    val indexOf = getPreorder.indexOf(itemId) + 1
    if (indexOf == size)
      null
    else
      getPreorder(indexOf)
  }

  override def prevItemId(itemId: Object) = {
    val indexOf = getPreorder.indexOf(itemId) - 1;
    if (indexOf < 0)
      null
    else
      getPreorder(indexOf)
  }

  override def addItemAt(index: Int) = notImpl

  override def addItemAt(index: Int, newItemId: Object) = notImpl

  override def getIdByIndex(index: Int) = getPreorder(index)

  override def indexOfId(itemId: Object) = getPreorder.indexOf(itemId)
}