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

package groovyx.gpars.remote.message;

import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

/**
 * Message sent by NetTransportProvider immediately after connection to another host is set up
 *
 * @author Alex Tkachman, Vaclav Pech
 */
public class HostIdMsg extends SerialMsg {

    /**
     * Construct message representing current state of the transport provider
     * @param id Local host id
     */
    public HostIdMsg(final UUID id) {
        super(id);
    }
}
