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

package groovyx.gpars.samples.collections

import static groovyx.gpars.GParsPool.*;
import groovy.transform.TupleConstructor

/**
 * @author Mario Garcia, Vaclav Pech
 */

@TupleConstructor
class PricedCar implements Cloneable {
    String model
    Double price

    String toString(){
        "PricedCar $model"
    }

    boolean equals(final o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        final PricedCar car = (PricedCar) o

        if (model != car.model) return false

        return true
    }

    int hashCode() {
        return (model != null ? model.hashCode() : 0)
    }

    @Override
    protected Object clone() {
        return super.clone()
    }
}

def cars = [new PricedCar("F550",2342.223),new PricedCar("F550",234.234),new PricedCar("Da",2222.2)]

withPool{
    def result =
        cars.parallel.map{
            [it,it]
        }.combine(new PricedCar("", 0.0)){sum,value->
            sum.model = value.model
            sum.price += value.price
            sum
        }
    .values()

    println result
}