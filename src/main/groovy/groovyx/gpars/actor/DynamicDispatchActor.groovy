//  GPars (formerly GParallelizer)
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

package groovyx.gpars.actor

import groovyx.gpars.actor.impl.AbstractPooledActor

/**
 * A pooled actor allowing for an alternative structure of the message handling code.
 * In general DynamicDispatchActor repeatedly scans for messages and dispatches arrived messages to one
 * of the onMessage(message) methods defined on the actor.
 * <pre>
 * final class MyActor extends DynamicDispatchActor {*
 *     void onMessage(String message) {*         println 'Received string'
 *}*
 *     void onMessage(Integer message) {*         println 'Received integer'
 *}*
 *     void onMessage(Object message) {*         println 'Received object'
 *}*
 *     void onNullMessage() {*         println 'Received null'
 *}*}* </pre>
 *
 * Method when {...} provide alternative way to define message handlers
 *
 * @author Vaclav Pech, Alex Tkachman
 * Date: Jun 26, 2009
 */

public class DynamicDispatchActor extends AbstractPooledActor {

  def handlers = [:] as LinkedHashMap

    /**
     * Creates a new instance without any when handlers registered
     */
  DynamicDispatchActor() {
    this(null)
  }

    /**
     * Creates an instance, processing all when{} calls in the supplied closure
     * @param closure A closure to run against te actor, typically to register handlers
     */
  DynamicDispatchActor(Closure closure) {

    respondsTo("onMessage").each {MetaMethod method ->
      if (method.parameterTypes.length == 1) {
        handlers[method.parameterTypes[0].theClass] = method
      }
    }

    respondsTo("onNullMessage").each {MetaMethod method ->
      if (method.parameterTypes.length == 0) {
        handlers[null] = method
      }
    }

    if (closure) {
      Closure cloned = (Closure) closure.clone()
      cloned.resolveStrategy = Closure.DELEGATE_FIRST
      cloned.delegate = this
      cloned.call()
    }
  }

  /**
   * Loops reading messages using the react() method and dispatches to the corresponding onMessage() method.
   */
  final void act() {
    loop {
      react { msg ->
        def msgClass = msg?.class

        def handler = null

        if (!handler)
          handler = handlers[msgClass]

        if (!handler) {
          def handlerClass = handlers.keySet().find {Class handlerMsgClass -> handlerMsgClass.isAssignableFrom(msgClass)}
          handler = handlers[handlerClass]
        }

        if (!handler)
          throw new IllegalStateException("Unable to handle message $it");

        if (handler instanceof Closure) {
          msg ? handler.call(msg) : handler.call ()
        }
        else {
          ((MetaMethod) handler).invoke(delegate, (msg != null ? [msg] : []) as Object[])
        }
      }
    }
  }

  void when(Closure closure) {
    Closure cloned = (Closure) closure.clone()
    cloned.resolveStrategy = Closure.DELEGATE_FIRST
    cloned.delegate = this

    if (closure.maximumNumberOfParameters == 0) {
      handlers[null] = cloned
    }
    else {
      if (closure.maximumNumberOfParameters != 1)
        throw new IllegalStateException("'when' closure should have zero or one parameter");

      handlers[cloned.parameterTypes[0]] = cloned
    }
  }
}
