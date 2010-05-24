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

package groovyx.gpars.samples.csp.proposed

//process ResetNumbers ( ! outChannel, ? resetChannel, def initialValue = 0 ){
//
//    def a = Channel.createOne2One()
//    def b = Channel.createOne2One()
//    def c = Channel.createOne2One()
//
//    def testList = [ new ResetPrefix ( prefixValue: initialValue,
//                                       outChannel: a.out(),
//                                       inChannel: c.in(),
//                                       resetChannel: resetChannel ),
//                     new GPCopy ( inChannel: a.in(),
//                            	  outChannel0: outChannel,
//                            	  outChannel1: b.out() ),
//                     new GSuccessor ( inChannel: b.in(),
//                            		  outChannel: c.out())
//                   ]
//    new PAR ( testList ).run()
//}
//                         
