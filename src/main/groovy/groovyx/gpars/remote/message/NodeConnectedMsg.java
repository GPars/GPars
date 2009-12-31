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

import groovyx.gpars.actor.Actor;
import groovyx.gpars.remote.LocalNode;
import groovyx.gpars.remote.RemoteConnection;
import groovyx.gpars.serial.SerialMsg;

import java.util.UUID;

/**
 * Message sent when local node connected to remote host
 *
 * @author Alex Tkachman
 */
public class NodeConnectedMsg extends SerialMsg {
    private static final long serialVersionUID = -2198640646677794254L;

    /**
     * Id of node connected
     */
    public final UUID nodeId;

    public final Actor mainActor;

    public NodeConnectedMsg(LocalNode node) {
        super();
        nodeId = node.getId();
        mainActor = node.getMainActor();
    }

    @Override
    public void execute(RemoteConnection conn) {
        conn.getHost().getLocalHost().connectRemoteNode(nodeId, conn.getHost(), mainActor);
    }
}
