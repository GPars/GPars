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

import groovy.transform.ToString
import groovy.transform.TupleConstructor

import static groovyx.gpars.GParsPool.withPool

/**
 * @author Mario Garcia, Vaclav Pech
 */

@TupleConstructor @ToString
class PricedCar implements Cloneable {
    String model
    String color
    Double price

    boolean equals(final o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        final PricedCar pricedCar = (PricedCar) o

        if (color != pricedCar.color) return false
        if (model != pricedCar.model) return false

        return true
    }

    int hashCode() {
        int result
        result = (model != null ? model.hashCode() : 0)
        result = 31 * result + (color != null ? color.hashCode() : 0)
        return result
    }

    @Override
    public Object clone() {
        return super.clone()
    }
}

def cars = [new PricedCar('F550', 'blue', 2342.223),
        new PricedCar('F550', 'red', 234.234),
        new PricedCar('Da', 'white', 2222.2),
        new PricedCar('Da', 'white', 1111.1)]

withPool {
    //Combine by model
    def result =
        cars.parallel.map {
            [it.model, it]
        }.combine(new PricedCar('', 'N/A', 0.0)) {sum, value ->
            sum.model = value.model
            sum.price += value.price
            sum
        }.values()

    println result

    //Combine by model and color (the PricedCar's equals and hashCode))
    result =
        cars.parallel.map {
            [it, it]
        }.combine(new PricedCar('', 'N/A', 0.0)) {sum, value ->
            sum.model = value.model
            sum.color = value.color
            sum.price += value.price
            sum
        }.values()

    println result
}
