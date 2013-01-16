/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import static groovyx.gpars.extra166y.Ops.BinaryLongOp;
import static groovyx.gpars.extra166y.Ops.BinaryLongPredicate;
import static groovyx.gpars.extra166y.Ops.IntAndLongPredicate;
import static groovyx.gpars.extra166y.Ops.IntAndLongToLong;
import static groovyx.gpars.extra166y.Ops.IntToLong;
import static groovyx.gpars.extra166y.Ops.LongGenerator;
import static groovyx.gpars.extra166y.Ops.LongOp;
import static groovyx.gpars.extra166y.Ops.LongPredicate;


/**
 * A prefix view of ParallelLongArray that causes operations to apply
 * only to elements for which a selector returns true.  Instances of
 * this class may be constructed only via prefix methods of
 * ParallelLongArray or its other prefix classes.
 */
public abstract class ParallelLongArrayWithFilter extends ParallelLongArrayWithLongMapping {
    ParallelLongArrayWithFilter
        (ForkJoinPool ex, int origin, int fence, long[] array) {
        super(ex, origin, fence, array);
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their current values.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArrayWithFilter replaceWithMapping(LongOp op) {
        ex.invoke(new PAS.FJLTransform
                  (this, origin, fence, null, op));
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * op to their indices.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArrayWithFilter replaceWithMappedIndex(IntToLong op) {
        ex.invoke(new PAS.FJLIndexMap
                  (this, origin, fence, null, op));
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * mapping to each index and current element value.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArrayWithFilter replaceWithMappedIndex(IntAndLongToLong op) {
        ex.invoke(new PAS.FJLBinaryIndexMap
                  (this, origin, fence, null, op));
        return this;
    }

    /**
     * Replaces elements with results of applying the given
     * generator.
     * @param generator the generator
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArrayWithFilter replaceWithGeneratedValue(LongGenerator generator) {
        ex.invoke(new PAS.FJLGenerate
                  (this, origin, fence, null, generator));
        return this;
    }

    /**
     * Replaces elements with the given value.
     * @param value the value
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArrayWithFilter replaceWithValue(long value) {
        ex.invoke(new PAS.FJLFill
                  (this, origin, fence, null, value));
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public ParallelLongArrayWithFilter replaceWithMapping(BinaryLongOp combiner,
                                   ParallelLongArrayWithLongMapping other) {
        ex.invoke(new PAS.FJLPACombineInPlace
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
    public ParallelLongArrayWithFilter replaceWithMapping(BinaryLongOp combiner,
                                   long[] other) {
        ex.invoke(new PAS.FJLCombineInPlace
                  (this, origin, fence, null, other,
                   -origin, combiner));
        return this;
    }

    /**
     * Returns a new ParallelLongArray containing only unique
     * elements (that is, without any duplicates).
     * @return the new ParallelLongArray
     */
    public ParallelLongArray allUniqueElements() {
        PAS.UniquifierTable tab = new PAS.UniquifierTable
            (fence - origin, this, false);
        PAS.FJLUniquifier f = new PAS.FJLUniquifier
            (this, origin, fence, null, tab);
        ex.invoke(f);
        long[] res = tab.uniqueLongs(f.count);
        return new ParallelLongArray(ex, res);
    }


    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public abstract ParallelLongArrayWithFilter withFilter(LongPredicate selector);

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given binary selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelLongArrayWithFilter withFilter
        (BinaryLongPredicate selector,
         ParallelLongArrayWithLongMapping other) {
        return withIndexedFilter(AbstractParallelAnyArray.indexedSelector(selector, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the current selector (if
     * present) and the given indexed selector returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public abstract ParallelLongArrayWithFilter withIndexedFilter
        (IntAndLongPredicate selector);

    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are equal.
     * @param other the other array
     * @return true if equal
     */
    public boolean hasAllEqualElements(ParallelLongArrayWithLongMapping other) {
        return withFilter(CommonOps.longInequalityPredicate(),
                          other).anyIndex() < 0;
    }

    final void leafTransfer(int lo, int hi, long[] dest, int offset) {
        final long[] a = this.array;
        for (int i = lo; i < hi; ++i)
            dest[offset++] = (a[i]);
    }

    final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                   long[] dest, int offset) {
        final long[] a = this.array;
        for (int i = loIdx; i < hiIdx; ++i)
            dest[offset++] = (a[indices[i]]);
    }

    final long lget(int i) { return this.array[i]; }
}
