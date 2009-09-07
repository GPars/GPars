package org.gparallelizer.actors.pooledActors;

/**
 * Utility class to implement AbstractPooledActor backed by any Runnable (including Closure)
 *
 * @author
 */
class RunnableBackedPooledActor extends AbstractPooledActor {

  private Runnable action

  RunnableBackedPooledActor() {
  }

  RunnableBackedPooledActor(Runnable handler) {
    setAction(handler)
  }

  void setAction(Runnable handler) {
    if (handler == null) {
      action = null
    }
    else {
      if (handler instanceof Closure) {
        def cloned = (Closure)handler.clone ()
        if (handler.owner == handler.delegate) {
          // otherwise someone else already took care for setting delegate for the closure
          cloned.delegate = this
          cloned.resolveStrategy = Closure.DELEGATE_FIRST
        }
        else {
          cloned.delegate = new RunnableBackedPooledActorDelegate(handler.delegate, this)
        }
        action = cloned
      }
      else {
        action = handler
      }
    }
  }

  protected void act() {
    if (action != null)
      action.run()
  }
}

class RunnableBackedPooledActorDelegate {
  final def first, second

  RunnableBackedPooledActorDelegate (def f, def s) {
    first  = f
    second = s
  }

  Object invokeMethod(String name, Object args) {
    try {
      first.invokeMethod(name, args)
    }
    catch (MissingMethodException mme) {
      second.invokeMethod(name, args)
    }
  }

  Object getProperty(String propertyName) {
    try {
      first.getProperty(name)
    }
    catch (MissingPropertyException mpe) {
      second.getProperty(name)
    }
  }

  void setProperty(String propertyName, Object newValue) {
    try {
      first.setProperty(name, newValue)
    }
    catch (MissingPropertyException mpe) {
      second.setProperty(name, newValue)
    }
  }
}