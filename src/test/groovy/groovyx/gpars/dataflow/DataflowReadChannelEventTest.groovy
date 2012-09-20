// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.dataflow

public class DataflowReadChannelEventTest extends GroovyTestCase {

    public void testVariable() {
        int counter = 0
        final DataflowVariable dfv = new DataflowVariable()
        dfv.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=1
            }
        })
        dfv.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=10
            }
        })
        assert counter==0
        dfv << 100
        assert counter==11

        dfv.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=100
            }
        })
        assert counter==11
    }

    public void testQueue() {
        int counter = 0
        final DataflowQueue queue = new DataflowQueue()
        queue.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=1
            }
        })
        queue.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=10
            }
        })
        assert counter==0
        queue << 100
        assert counter==11
        queue << 100
        assert counter==22

        queue.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=100
            }
        })
        queue << 100
        assert counter==133
    }

    public void testBroadcast() {
        int counter = 0
        final DataflowBroadcast broadcast = new DataflowBroadcast()
        final subscription1 = broadcast.createReadChannel()
        final subscription2 = broadcast.createReadChannel()

        subscription1.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=1
            }
        })
        subscription2.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=100
            }
        })
        subscription1.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=10
            }
        })
        assert counter==0
        broadcast << 100
        assert counter==111
        broadcast << 100
        assert counter==222

        final subscription3 = broadcast.createReadChannel()
        subscription3.eventManager.addDataflowChannelListener(new DataflowChannelListener<Integer>() {
            @Override
            void onMessage(final Integer message) {
                counter+=1000
            }
        })
        assert counter==222
        broadcast << 100
        assert counter==1333
    }
}
