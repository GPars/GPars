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

package org.gparallelizer.actors

/**
 * ThreadedActors are active objects, which have their own thread processing repeatedly messages submitted to them.
 * The Actor super-interface provides means to send messages to the object, start and stop the background thread as well as
 * check its status.
 *
 * @author Vaclav Pech
 * Date: Feb 20, 2009
 */
public interface ThreadedActor extends Actor {}
