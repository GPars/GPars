//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

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
