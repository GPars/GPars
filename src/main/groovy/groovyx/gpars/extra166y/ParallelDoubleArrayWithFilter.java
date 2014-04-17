/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import static groovyx.gpars.extra166y.Ops.BinaryDoubleOp;
import static groovyx.gpars.extra166y.Ops.BinaryDoublePredicate;
import static groovyx.gpars.extra166y.Ops.DoubleGenerator;
import static groovyx.gpars.extra166y.Ops.DoubleOp;
import static groovyx.gpars.extra166y.Ops.DoublePredicate;
import static groovyx.gpars.extra166y.Ops.IntAndDoublePredicate;
import static groovyx.gpars.extra166y.Ops.IntAndDoubleToDouble;
import static groovyx.gpars.extra166y.Ops.IntToDouble;

/**
 * A prefix view of ParallelDoubleArray that causes operations to apply
 * only to elements for which a selector returns true.
 * Instances of this class may be constructed only via prefix
 * methods of ParallelDoubleArray or its other prefix classes.
 */
public abstract class ParallelDoubleArrayWithFilter extends ParallelDoubleArrayWithDoubleMapping {
    ParallelDoubleArrayWithFilter
        (ForkJoinPool ex, int origin, int fence, double[] array) {
        super(ex, origin, fence, array);
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their current values.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArrayWithFilter replaceWithMapping(DoubleOp op) {
        ex.invoke(new PAS.FJDTransform(this, origin,
                                       fence, null, op));
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their indices.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArrayWithFilter replaceWithMappedIndex(IntToDouble op) {
        ex.invoke(new PAS.FJDIndexMap(this, origin, fence,
                                      null, op));
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * mapping to each index and current element value.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArrayWithFilter replaceWithMappedIndex(IntAndDoubleToDouble op) {
        ex.invoke(new PAS.FJDBinaryIndexMap
                  (this, origin, fence, null, op));
        return this;
    }

    /**
     * Replaces elements with results of applying the given generator.
     * @param generator the generator
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArrayWithFilter replaceWithGeneratedValue(DoubleGenerator generator) {
        ex.invoke(new PAS.FJDGenerate
                  (this, origin, fence, null, generator));
        return this;
    }

    /**
     * Replaces elements with the given value.
     * @param value the value
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArrayWithFilter replaceWithValue(double value) {
        ex.invoke(new PAS.FJDFill(this, origin, fence,
                                  null, value));
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArrayWithFilter replaceWithMapping(BinaryDoubleOp combiner,
                                   ParallelDoubleArrayWithDoubleMapping other) {
        ex.invoke(new PAS.FJDPACombineInPlace
                  (this, origin, fence, null,
                   other, other.origin - origin, combiner));
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public ParallelDoubleArrayWithFilter replaceWithMapping
        (BinaryDoubleOp combiner,
         double[] other) {
        ex.invoke(new PAS.FJDCombineInPlace
                  (this, origin, fence, null, other,
                   -origin, combiner));
        return this;
    }

    /**
     * Returns a new ParallelDoubleArray containing only unique
     * elements (that is, without any duplicates).
     * @return the new ParallelDoubleArray
     */
    public ParallelDoubleArray allUniqueElements() {
        PAS.UniquifierTable tab = new PAS.UniquifierTable
            (fence - origin, this, false);
        PAS.FJDUniquifier f = new PAS.FJDUniquifier
            (this, origin, fence, null, tab);
        ex.invoke(f);
        double[] res = tab.uniqueDoubles(f.count);
        return new ParallelDoubleArray(ex, res);
    }


    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public abstract ParallelDoubleArrayWithFilter withFilter
        (DoublePredicate selector);

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given binary selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelDoubleArrayWithFilter withFilter
        (BinaryDoublePredicate selector,
         ParallelDoubleArrayWithDoubleMapping other) {
        return withIndexedFilter(AbstractParallelAnyArray.indexedSelector(selector, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given indexed selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public abstract ParallelDoubleArrayWithFilter withIndexedFilter
        (IntAndDoublePredicate selector);

    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are equal.
     * @param other the other array
     * @return true if equal
     */
    public boolean hasAllEqualElements
        (ParallelDoubleArrayWithDoubleMapping other) {
        return withFilter(CommonOps.doubleInequalityPredicate(),
                          other).anyIndex() < 0;
    }

    final void leafTransfer(int lo, int hi, double[] dest, int offset) {
        final double[] a = this.array;
        for (int i = lo; i < hi; ++i)
            dest[offset++] = (a[i]);
    }

    final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                   double[] dest, int offset) {
        final double[] a = this.array;
        for (int i = loIdx; i < hiIdx; ++i)
            dest[offset++] = (a[indices[i]]);
    }

    final double dget(int i) { return this.array[i]; }

}
