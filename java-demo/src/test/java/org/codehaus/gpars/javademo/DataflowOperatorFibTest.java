// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package org.codehaus.gpars.javademo;

import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.Dataflow;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.operator.DataflowProcessor;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Vaclav Pech
 */

@SuppressWarnings({"rawtypes", "RawUseOfParameterizedType", "MagicNumber"})
public class DataflowOperatorFibTest {
    @Test
    public void testFibonacci() throws Exception {
        final DataflowQueue<BigInteger> ch1 = new DataflowQueue<BigInteger>();
        final DataflowQueue<BigInteger> ch2 = new DataflowQueue<BigInteger>();
        final DataflowQueue<BigInteger> ch3 = new DataflowQueue<BigInteger>();

        final long startTime = System.currentTimeMillis();
        ch1.bind(BigInteger.ONE);
        ch2.bind(BigInteger.ZERO);
        ch2.bind(BigInteger.ZERO);


        final List<DataflowQueue<BigInteger>> calculationInputList = asList(ch1, ch2);
        final List<DataflowQueue<BigInteger>> calculationOutputList = asList(ch3, ch1, ch2);
        final DataflowProcessor op = Dataflow.operator(calculationInputList, calculationOutputList, new DataflowMessagingRunnable(2) {
            long counter = 0L;

            @Override
            protected void doRun(final Object... arguments) {
                DataflowProcessor owner = getOwningProcessor();
                BigInteger a = (BigInteger) arguments[0];
                BigInteger b = (BigInteger) arguments[1];
                counter++;
                final BigInteger sum = a.add(b);
                owner.bindOutput(1, sum);
                owner.bindOutput(2, sum);
                if (counter == 1000000L) {
                    owner.bindOutput(0, sum);
                    owner.terminateAfterNextRun();
                }
            }
        });


        System.out.println(ch3.getVal());

        op.join();
        System.out.println("time: " + (System.currentTimeMillis() - startTime));
    }
}

