/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import static groovyx.gpars.extra166y.Ops.BinaryLongOp;
import static groovyx.gpars.extra166y.Ops.IntAndLongToDouble;
import static groovyx.gpars.extra166y.Ops.IntAndLongToLong;
import static groovyx.gpars.extra166y.Ops.IntAndLongToObject;
import static groovyx.gpars.extra166y.Ops.LongAndDoubleToDouble;
import static groovyx.gpars.extra166y.Ops.LongAndDoubleToLong;
import static groovyx.gpars.extra166y.Ops.LongAndDoubleToObject;
import static groovyx.gpars.extra166y.Ops.LongAndLongToDouble;
import static groovyx.gpars.extra166y.Ops.LongAndLongToObject;
import static groovyx.gpars.extra166y.Ops.LongAndObjectToDouble;
import static groovyx.gpars.extra166y.Ops.LongAndObjectToLong;
import static groovyx.gpars.extra166y.Ops.LongAndObjectToObject;
import static groovyx.gpars.extra166y.Ops.LongComparator;
import static groovyx.gpars.extra166y.Ops.LongOp;
import static groovyx.gpars.extra166y.Ops.LongProcedure;
import static groovyx.gpars.extra166y.Ops.LongReducer;
import static groovyx.gpars.extra166y.Ops.LongToDouble;
import static groovyx.gpars.extra166y.Ops.LongToObject;

/**
 * A prefix view of ParallelArray that causes operations to apply
 * to mappings of elements, not to the elements themselves.
 * Instances of this class may be constructed only via prefix
 * methods of ParallelArray or its other prefix classes.
 */
public abstract class ParallelArrayWithLongMapping<T> extends AbstractParallelAnyArray.OPap<T> {
    ParallelArrayWithLongMapping
        (ForkJoinPool ex, int origin, int fence, T[] array) {
        super(ex, origin, fence, array);
    }

    /**
     * Applies the given procedure.
     * @param procedure the procedure
     */
    public void apply(LongProcedure procedure) {
        ex.invoke(new PAS.FJLApply(this, origin, fence, null, procedure));
    }

    /**
     * Returns reduction of mapped elements.
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return reduction
     */
    public long reduce(LongReducer reducer, long base) {
        PAS.FJLReduce f = new PAS.FJLReduce
            (this, origin, fence, null, reducer, base);
        ex.invoke(f);
        return f.result;
    }

    /**
     * Returns the minimum element, or Long.MAX_VALUE if empty.
     * @return minimum element, or Long.MAX_VALUE if empty
     */
    public long min() {
        return reduce(CommonOps.naturalLongMinReducer(), Long.MAX_VALUE);
    }

    /**
     * Returns the minimum element, or Long.MAX_VALUE if empty.
     * @param comparator the comparator
     * @return minimum element, or Long.MAX_VALUE if empty
     */
    public long min(LongComparator comparator) {
        return reduce(CommonOps.longMinReducer(comparator), Long.MAX_VALUE);
    }

    /**
     * Returns the maximum element, or Long.MIN_VALUE if empty.
     * @return maximum element, or Long.MIN_VALUE if empty
     */
    public long max() {
        return reduce(CommonOps.naturalLongMaxReducer(), Long.MIN_VALUE);
    }

    /**
     * Returns the maximum element, or Long.MIN_VALUE if empty.
     * @param comparator the comparator
     * @return maximum element, or Long.MIN_VALUE if empty
     */
    public long max(LongComparator comparator) {
        return reduce(CommonOps.longMaxReducer(comparator), Long.MIN_VALUE);
    }

    /**
     * Returns the sum of elements.
     * @return the sum of elements
     */
    public long sum() {
        return reduce(CommonOps.longAdder(), 0L);
    }

    /**
     * Returns summary statistics.
     * @param comparator the comparator to use for
     * locating minimum and maximum elements
     * @return the summary
     */
    public ParallelLongArray.SummaryStatistics summary
        (LongComparator comparator) {
        PAS.FJLStats f = new PAS.FJLStats
            (this, origin, fence, null, comparator);
        ex.invoke(f);
        return f;
    }

    /**
     * Returns summary statistics, using natural comparator.
     * @return the summary
     */
    public ParallelLongArray.SummaryStatistics summary() {
        return summary(CommonOps.naturalLongComparator());
    }

    /**
     * Returns a new ParallelLongArray holding mappings.
     * @return a new ParallelLongArray holding mappings
     */
    public ParallelLongArray all() {
        return new ParallelLongArray(ex, allLongs());
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public abstract ParallelArrayWithDoubleMapping<T> withMapping(LongToDouble op);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public abstract ParallelArrayWithLongMapping<T> withMapping(LongOp op);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public abstract <U> ParallelArrayWithMapping<T,U> withMapping
        (LongToObject<? extends U> op);

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V,W,X> ParallelArrayWithMapping<T,W> withMapping
        (LongAndObjectToObject<? super V, ? extends W> combiner,
         ParallelArrayWithMapping<X,V> other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V> ParallelArrayWithMapping<T,V> withMapping
        (LongAndDoubleToObject<? extends V> combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V> ParallelArrayWithMapping<T,V> withMapping
        (LongAndLongToObject<? extends V> combiner,
         ParallelLongArrayWithLongMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V,W> ParallelArrayWithDoubleMapping<T> withMapping
        (LongAndObjectToDouble<? super V> combiner,
         ParallelArrayWithMapping<W,V> other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelArrayWithDoubleMapping<T> withMapping
        (LongAndDoubleToDouble combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }


    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelArrayWithDoubleMapping<T> withMapping
        (LongAndLongToDouble combiner,
         ParallelLongArrayWithLongMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public <V,W> ParallelArrayWithLongMapping<T> withMapping
        (LongAndObjectToLong<? super V> combiner,
         ParallelArrayWithMapping<W,V> other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }


    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelArrayWithLongMapping<T> withMapping
        (LongAndDoubleToLong combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }


    /**
     * Returns an operation prefix that causes a method to operate
     * on binary mappings of this array and the other array.
     * @param combiner the combiner
     * @param other the other array
     * @return operation prefix
     * @throws IllegalArgumentException if other array is a
     * filtered view (all filters must precede all mappings)
     */
    public ParallelArrayWithLongMapping<T> withMapping
        (BinaryLongOp combiner,
         ParallelLongArrayWithLongMapping other) {
        if (other.hasFilter()) throw new IllegalArgumentException();
        return withIndexedMapping
            (AbstractParallelAnyArray.indexedMapper(combiner, other, origin));
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mappings of this array using the given mapper that
     * accepts as arguments an element's current index and value
     * (as mapped by preceding mappings, if any), and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public abstract <V> ParallelArrayWithMapping<T,V> withIndexedMapping
        (IntAndLongToObject<? extends V> mapper);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mappings of this array using the given mapper that
     * accepts as arguments an element's current index and value
     * (as mapped by preceding mappings, if any), and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public abstract ParallelArrayWithDoubleMapping<T> withIndexedMapping
        (IntAndLongToDouble mapper);

    /**
     * Returns an operation prefix that causes a method to operate
     * on mappings of this array using the given mapper that
     * accepts as arguments an element's current index and value
     * (as mapped by preceding mappings, if any), and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public abstract ParallelArrayWithLongMapping<T> withIndexedMapping
        (IntAndLongToLong mapper);

    /**
     * Returns an Iterable view to sequentially step through mapped
     * elements also obeying bound and filter constraints, without
     * performing computations to evaluate them in parallel.
     * @return the Iterable view
     */
    public Iterable<Long> sequentially() {
        return new SequentiallyAsLong();
    }
}
