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

package org.gparallelizer.remote.messages;

import org.gparallelizer.remote.RemoteTransportProvider;

/**
 * Message sent by NetTransportProvider immediately after connection to another host set up set up
 *
 * @author Alex Tkachman
 */
public class HostIdMsg extends BaseMsg {

    /**
     * Construct message representing current state of the transport provider
     *
     * @param provider transport provider
     */
    public HostIdMsg(RemoteTransportProvider provider) {
        super(provider.getId());
    }
}
