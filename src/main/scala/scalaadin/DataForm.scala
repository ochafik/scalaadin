package scalaadin

import com.vaadin.data.util._
import com.vaadin.data._
import com.vaadin.event._
import com.vaadin.data.Property
import com.vaadin.data.Property.ValueChangeListener
import com.vaadin.data.Property.ValueChangeEvent
import com.vaadin.addon.beanvalidation.BeanValidationForm
import com.vaadin.data.Validator.InvalidValueException
import com.vaadin.ui._
import org.vaadin._

class DataForm[D <: AnyRef](visibleFields: String*) extends Form {
  private var _data: D = null.asInstanceOf[D]
  private var _fields: Seq[String] = null
  def fields = _fields
  def fields_=(fields: Seq[String]) = {
    this._fields = fields
    if (fields != null)
      setVisibleItemProperties(fields.map(_.asInstanceOf[Object]).toArray)
  }
    
  def data = _data 
  def data_=(data: D) = {
    this._data = data
    if (data == null)
      setItemDataSource(null)
    else {
      val item = new BeanItem[D](data)
      setItemDataSource(item)
      fields = fields
    }
  }
  
  this.showNullValuesAsEmptyStrings
  
  setImmediate(false)
  setWriteThrough(false)
  setInvalidCommitted(false)
  
  fields = visibleFields.toSeq
}
