// GPars (formerly GParallelizer)
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

package groovyx.gpars.samples.forkjoin;


import groovyx.gpars.AbstractForkJoinWorker

/**
 *
 * @author Vaclav Pech
 * Date: Feb 19, 2010
 * Time: 7:36:40 PM
 */
public class JForkJoinFib extends AbstractForkJoinWorker {

    int number;

    public JForkJoinFib(final int number) {
        this.number = number;
    }

    protected void computeTask() {
        if (number < 0) {
            throw new RuntimeException("No fib below 0!");
        }
        if (number <= 13) {
            setResult(seqfib(number));
            return;
        }
        forkOffChild(new JForkJoinFib(number - 1));
        forkOffChild(new JForkJoinFib(number - 2));
        List results = null;
        try {
            results = getChildrenResults();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        setResult((Integer) results.get(0) + (Integer) results.get(1));
    }

    static int seqfib(int n) {
        if (n <= 1) return n;
        else return seqfib(n - 1) + seqfib(n - 2);
    }
}
