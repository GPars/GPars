// GPars - Groovy Parallel Systems
//
// Copyright © 2008-10  The original author or authors
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

package groovyx.gpars.dataflow;

import groovyx.gpars.actor.impl.MessageStream;

import java.util.List;

/**
 * @author Vaclav Pech
 *         Date: 30th Sep 2010
 */
public class MessagingAltSelect<T> {

    private final AbstractAltSelect<T> altSelect = new AbstractAltSelect<T>();

    private final MessageStream messageStream;

    public MessagingAltSelect(final MessageStream messageStream) {
        this.messageStream = messageStream;
    }

    public void select() throws InterruptedException {
        select(-1, null);
    }

    public void select(final List<Boolean> mask) throws InterruptedException {
        select(-1, mask);
    }

    public void prioritySelect() throws InterruptedException {
        select(0, null);
    }

    public void prioritySelect(final List<Boolean> mask) throws InterruptedException {
        select(0, mask);
    }

    private void select(final int startIndex, final List<Boolean> mask) throws InterruptedException {
        altSelect.doSelect(startIndex, new MaskSelectRequest<T>(mask) {
            @Override
            public void valueFound(final int index, final T value) {
                messageStream.send(new SelectResult() {
                    @Override
                    public int getIndex() {
                        return index;
                    }

                    @Override
                    public T getValue() {
                        return value;
                    }
                }
                );
            }
        });
    }
}
