/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import static groovyx.gpars.extra166y.Ops.DoubleComparator;
import static groovyx.gpars.extra166y.Ops.DoubleReducer;

/**
 * A prefix view of ParallelDoubleArray that causes operations to apply
 * only to elements within a given range.
 * Instances of this class may be constructed only via prefix
 * methods of ParallelDoubleArray or its other prefix classes.
 */
public abstract class ParallelDoubleArrayWithBounds extends ParallelDoubleArrayWithFilter {
    ParallelDoubleArrayWithBounds
        (ForkJoinPool ex, int origin, int fence, double[] array) {
        super(ex, origin, fence, array);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on the elements of the array between firstIndex
     * (inclusive) and upperBound (exclusive).  The bound
     * arguments are relative to the current bounds.  For example
     * {@code pa.withBounds(2, 8).withBounds(3, 5)} indexes the
     * 5th (= 2+3) and 6th elements of pa. However, indices
     * returned by methods such as {@code indexOf} are
     * with respect to the underlying ParallelDoubleArray.
     * @param firstIndex the lower bound (inclusive)
     * @param upperBound the upper bound (exclusive)
     * @return operation prefix
     */
    public abstract ParallelDoubleArrayWithBounds withBounds
        (int firstIndex, int upperBound);

    /**
     * Returns the index of some element equal to given target,
     * or -1 if not present.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public abstract int indexOf(double target);

    /**
     * Assuming this array is sorted, returns the index of an
     * element equal to given target, or -1 if not present. If the
     * array is not sorted, the results are undefined.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public abstract int binarySearch(double target);

    /**
     * Assuming this array is sorted with respect to the given
     * comparator, returns the index of an element equal to given
     * target, or -1 if not present. If the array is not sorted,
     * the results are undefined.
     * @param target the element to search for
     * @param comparator the comparator
     * @return the index or -1 if not present
     */
    public abstract int binarySearch(double target,
                                     DoubleComparator comparator);

    /**
     * Replaces each element with the running cumulation of applying
     * the given reducer.
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return this (to simplify use in expressions)
     */
    public abstract ParallelDoubleArrayWithBounds cumulate(DoubleReducer reducer, double base);

    /**
     * Replaces each element with the running sum.
     * @return this (to simplify use in expressions)
     */
    public abstract ParallelDoubleArrayWithBounds cumulateSum();

    /**
     * Replaces each element with the cumulation of applying the given
     * reducer to all previous values, and returns the total
     * reduction.
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return the total reduction
     */
    public abstract double precumulate(DoubleReducer reducer, double base);

    /**
     * Replaces each element with its prefix sum.
     * @return the total sum
     */
    public abstract double precumulateSum();

    /**
     * Sorts the elements.
     * Unlike Arrays.sort, this sort does
     * not guarantee that elements with equal keys maintain their
     * relative position in the array.
     * @param cmp the comparator to use
     * @return this (to simplify use in expressions)
     */
    public abstract ParallelDoubleArrayWithBounds sort(DoubleComparator cmp);

    /**
     * Sorts the elements, assuming all elements are
     * Comparable. Unlike Arrays.sort, this sort does not
     * guarantee that elements with equal keys maintain their relative
     * position in the array.
     * @throws ClassCastException if any element is not Comparable
     * @return this (to simplify use in expressions)
     */
    public abstract ParallelDoubleArrayWithBounds sort();
}
