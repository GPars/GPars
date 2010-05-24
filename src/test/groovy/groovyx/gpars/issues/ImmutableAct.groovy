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

package groovyx.gpars.issues

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Sep 21, 2009
 * Time: 10:56:05 PM
 * To change this template use File | Settings | File Templates.
 */

class FailingA {
    protected void act() {
        throw new UnsupportedOperationException()
    }
}

//@Immutable
final class FailingB extends FailingA {
    public void act() {

    }
}

final def b = new FailingB()
