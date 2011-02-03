package scalaadin

case class Event() {
  private var listeners = scala.collection.mutable.ArrayBuffer[() => Unit]()
  def listen(block: => Unit) = this synchronized {
    listeners += (() => block)
  }
  def apply(block: => Unit) = listen(block)
  
  def ! = this synchronized {
    for (listener <- listeners)
      try { 
        listener() 
      } catch { 
        case ex =>
          ex.printStackTrace
      }
  }
}

class Task(action: => Unit) {
  private var lastThread: Thread = _
  
  def run: Unit = this !
  
  def ! = this synchronized {
    if (lastThread != null) {
      lastThread.interrupt
    }
    lastThread = new Thread { override def run { action } }
    lastThread.start
  }
}

trait ValueProxy[/* @specialized(Int, Double) */ T] {
  //protected var proxiedValue: T
  var value: T
  def +=(valueToAdd: T)(implicit n: Numeric[T]) = this synchronized {
      value = n.plus(value, valueToAdd)
      value
  }
  def ++(implicit n: Numeric[T]) = this += n.one
  def -=(valueToAdd: T)(implicit n: Numeric[T]) = this synchronized {
      value = n.minus(value, valueToAdd)
      value
  }
  def --(implicit n: Numeric[T]) = this -= n.one
  def *=(valueToAdd: T)(implicit n: Numeric[T]) = this synchronized {
      value = n.times(value, valueToAdd)
      value
  }
  def <(valueToAdd: T)(implicit n: Numeric[T]) = this synchronized {
      n.lt(value, valueToAdd)
  }
  def <=(valueToAdd: T)(implicit n: Numeric[T]) = this synchronized {
      n.lteq(value, valueToAdd)
  }
  def >=(valueToAdd: T)(implicit n: Numeric[T]) = this synchronized {
      n.gteq(value, valueToAdd)
  }
  def >(valueToAdd: T)(implicit n: Numeric[T]) = this synchronized {
      n.gt(value, valueToAdd)
  }
}
class Var[S <: AnyRef, /* @specialized(Int, Double) */ T](source: S = null) extends ValueProxy[T] {
  protected var proxiedValue: T = _
  private type This = Var[S, T]
  
  class Listeners[R] {
      type Sourced = (S, T, T) => R
      type Unsourced = (T, T) => R
      type JustValue = T => R
      
      import scala.collection.mutable.ArrayBuffer
      private val sourcedListeners = new ArrayBuffer[Sourced]
      private val unsourcedListeners = new ArrayBuffer[Unsourced]
      private val justValueListeners = new ArrayBuffer[JustValue]
      def +=(listener: Sourced) = Var.this synchronized {
          sourcedListeners += listener
          Var.this
      }
      def -=(listener: Sourced) = Var.this synchronized {
          sourcedListeners -= listener
          Var.this
      }
      def +=(listener: Unsourced) = Var.this synchronized {
          unsourcedListeners += listener
          Var.this
      }
      def -=(listener: Unsourced) = Var.this synchronized {
          unsourcedListeners -= listener
          Var.this
      }
      
      def +=(listener: JustValue) = Var.this synchronized {
          justValueListeners += listener
          Var.this
      }
      def -=(listener: JustValue) = Var.this synchronized {
          justValueListeners -= listener
          Var.this
      }
      def listeners = 
        sourcedListeners ++ 
        unsourcedListeners.map(f => (source: S, oldValue: T, newValue: T) => f(oldValue, newValue)) ++
        justValueListeners.map(f => (source: S, oldValue: T, newValue: T) => f(newValue))
  }
  
  def listen(listener: (S, T, T) => Unit): This = {
    listeners += listener
    this
  }
  def listen(listener: (T, T) => Unit): This = {
    listeners += listener
    this
  }
  def listen(listener: T => Unit): This = {
    listeners += listener
    this
  }
  def listen(eventBlock: => Unit): This = {
    listeners += { v => eventBlock }
    this
  }
  
  def veto(listener: (S, T, T) => Boolean): This = {
    vetoers += listener
    this
  }
  def veto(listener: (T, T) => Boolean): This = {
    vetoers += listener
    this
  }
  def veto(listener: T => Boolean): This = {
    vetoers += listener
    this
  }
  
  import scala.collection.mutable.ArrayBuffer
  protected val listeners = new Listeners[Unit]
  protected val vetoers = new Listeners[Boolean]
  
  override def value: T = apply
  override def value_=(newValue: T): Unit = update(newValue)
  
  def apply(): T = proxiedValue
  def update(newValue: T): Boolean = this synchronized {
    val oldValue = proxiedValue
    if (oldValue == newValue || !vetoers.listeners.forall(_(source, oldValue, newValue)))
        false
    else {
        proxiedValue = newValue
        listeners.listeners.foreach(_(source, oldValue, newValue))
        true
    }
  }
  override def toString = if (value == null) "null" else value.toString
  override def hashCode = if (value == null) 0 else value.hashCode
  /*override def equals(o: AnyRef) = {
      if (o.isInstanceOf[Var])
          value.equals(o.asInstanceOf[Var].value) // TODO
      
  }*/
}
object Var {
    def apply[T]() = {
        val v: Var[AnyRef, T] = new Var
        v
    }
    
    def apply[/* @specialized(Int, Double) */ T](initialValue: T) = {
        val v: Var[AnyRef, T] = new Var
        v.value = initialValue
        v
    }
    def apply[S <: AnyRef, /* @specialized(Int, Double) */ T](source: S, initialValue: T) = {
        val v = new Var[S, T](source)
        v.value = initialValue
    }
    implicit def Var2Value[S <: AnyRef, /* @specialized(Int, Double) */ T](v: Var[S, T]) = v()
}

/*
object TestVar extends Application {
    val v = Var(10)
    val w = Var("hello")
    v.listeners += { vv => println("Listener 1: " + vv) }
    v listen {
        println("Listener block " + v)
    }
    v() = 11
    v += 10
    v++
    val i: Int = v()
    val ii: Int = v
    
    
}
*/
