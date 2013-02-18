/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import static groovyx.gpars.extra166y.Ops.BinaryOp;
import static groovyx.gpars.extra166y.Ops.BinaryPredicate;
import static groovyx.gpars.extra166y.Ops.Generator;
import static groovyx.gpars.extra166y.Ops.IntAndObjectPredicate;
import static groovyx.gpars.extra166y.Ops.IntAndObjectToDouble;
import static groovyx.gpars.extra166y.Ops.IntAndObjectToLong;
import static groovyx.gpars.extra166y.Ops.IntAndObjectToObject;
import static groovyx.gpars.extra166y.Ops.IntToObject;
import static groovyx.gpars.extra166y.Ops.ObjectAndDoubleToDouble;
import static groovyx.gpars.extra166y.Ops.ObjectAndDoubleToLong;
import static groovyx.gpars.extra166y.Ops.ObjectAndDoubleToObject;
import static groovyx.gpars.extra166y.Ops.ObjectAndLongToDouble;
import static groovyx.gpars.extra166y.Ops.ObjectAndLongToLong;
import static groovyx.gpars.extra166y.Ops.ObjectAndLongToObject;
import static groovyx.gpars.extra166y.Ops.ObjectAndObjectToDouble;
import static groovyx.gpars.extra166y.Ops.ObjectAndObjectToLong;
import static groovyx.gpars.extra166y.Ops.ObjectToDouble;
import static groovyx.gpars.extra166y.Ops.ObjectToLong;
import static groovyx.gpars.extra166y.Ops.Op;
import static groovyx.gpars.extra166y.Ops.Predicate;
import static groovyx.gpars.extra166y.Ops.Procedure;
import static groovyx.gpars.extra166y.Ops.Reducer;

/**
 * An array supporting parallel operations.
 *
 * <p>A ParallelArray maintains a {@link ForkJoinPool} and an
 * array in order to provide parallel aggregate operations.  The main
 * operations are to <em>apply</em> some procedure to each element, to
 * <em>map</em> each element to a new element, to <em>replace</em>
 * each element, to <em>select</em> a subset of elements based on
 * matching a predicate or ranges of indices, and to <em>reduce</em>
 * all elements into a single value such as a sum.
 *
 * <p>A ParallelArray is constructed by allocating, using, or copying
 * an array, using one of the static factory methods {@link #create},
 * {@link #createEmpty}, {@link #createUsingHandoff} and {@link
 * #createFromCopy}. Upon construction, the encapsulated array managed
 * by the ParallelArray must not be shared between threads without
 * external synchronization. In particular, as is the case with any
 * array, access by another thread of an element of a ParallelArray
 * while another operation is in progress has undefined effects.
 *
 * <p>The ForkJoinPool used to construct a ParallelArray can be
 * shared safely by other threads (and used in other
 * ParallelArrays). To avoid the overhead associated with creating
 * multiple executors, it is often a good idea to use the {@link
 * #defaultExecutor()} across all ParallelArrays. However, you might
 * choose to use different ones for the sake of controlling processor
 * usage, isolating faults, and/or ensuring progress.
 *
 * <p>A ParallelArray is not a List. It relies on random access across
 * array elements to support efficient parallel operations.  However,
 * a ParallelArray can be viewed and manipulated as a List, via method
 * {@link ParallelArray#asList}. The {@code asList} view allows
 * incremental insertion and modification of elements while setting up
 * a ParallelArray, generally before using it for parallel
 * operations. Similarly, the list view may be useful when accessing
 * the results of computations in sequential contexts.  A
 * ParallelArray may also be created using the elements of any other
 * Collection, by constructing from the array returned by the
 * Collection's {@code toArray} method. The effects of mutative
 * {@code asList} operations may also be achieved directly using
 * method {@link #setLimit} along with element-by-element access
 * methods {@link #get}</tt> and {@link #set}.
 *
 * <p>While ParallelArrays can be based on any kind of an object
 * array, including "boxed" types such as Long, parallel operations on
 * scalar "unboxed" type are likely to be substantially more
 * efficient. For this reason, classes {@link ParallelLongArray} and
 * {@link ParallelDoubleArray} are also supplied, and designed to
 * smoothly interoperate with ParallelArrays. You should also use a
 * ParallelLongArray for processing other integral scalar data
 * ({@code int}, {@code short}, etc).  And similarly use a
 * ParallelDoubleArray for {@code float} data.  (Further
 * specializations for these other types would add clutter without
 * significantly improving performance beyond that of the Long and
 * Double versions.)
 *
 * <p>Most usages of ParallelArray involve sets of operations prefixed
 * with range bounds, filters, and mappings (including mappings that
 * combine elements from other ParallelArrays), using
 * {@code withBounds}, {@code withFilter}, and {@code withMapping},
 * respectively. For example,
 * {@code aParallelArray.withFilter(aPredicate).all()} creates a new
 * ParallelArray containing only those elements matching the
 * predicate. And for ParallelLongArrays a, b, and c,
 * {@code a.withMapping(CommonOps.longAdder(),b).withMapping(CommonOps.longAdder(),c).min()}
 * returns the minimum value of a[i]+b[i]+c[i] for all i.  As
 * illustrated below, a <em>mapping</em> often represents accessing
 * some field or invoking some method of an element.  These versions
 * are typically more efficient than performing selections, then
 * mappings, then other operations in multiple (parallel) steps. The
 * basic ideas and usages of filtering and mapping are similar to
 * those in database query systems such as SQL, but take a more
 * restrictive form.  Series of filter and mapping prefixes may each
 * be cascaded, but all filter prefixes must precede all mapping
 * prefixes, to ensure efficient execution in a single parallel step.
 * In cases of combined mapping expressions, this rule is only
 * dynamically enforced. For example, {@code pa.withMapping(op,
 * pb.withFilter(f))} will compile but throw an exception upon
 * execution because the filter precedes the mapping.
 *
 * <p>While series of filters and mappings are allowed, it is
 * usually more efficient to combine them into single filters or
 * mappings when possible. For example
 * {@code pa.withMapping(addOne).withMapping(addOne)} is generally
 * less efficient than {@code pa.withMapping(addTwo)}. Methods
 * {@code withIndexedFilter} and {@code withIndexedMapping} may be
 * useful when combining such expressions.
 *
 * <p>This class includes some reductions, such as {@code min}, that
 * are commonly useful for most element types, as well as a combined
 * version, {@code summary}, that computes all of them in a single
 * parallel step, which is normally more efficient than computing each
 * in turn.
 *
 * <p>The methods in this class are designed to perform efficiently
 * with both large and small pools, even with single-thread pools on
 * uniprocessors.  However, there is some overhead in parallelizing
 * operations, so short computations on small arrays might not execute
 * faster than sequential versions, and might even be slower.
 *
 * <p><b>Sample usages</b>.
 *
 * The main difference between programming with plain arrays and
 * programming with aggregates is that you must separately define each
 * of the component functions on elements. For example, the following
 * returns the maximum Grade Point Average across all senior students,
 * given a (fictional) {@code Student} class:
 *
 * <pre>
 * import static Ops.*;
 * class StudentStatistics {
 *   ParallelArray&lt;Student&gt; students = ...
 *   // ...
 *   public double getMaxSeniorGpa() {
 *     return students.withFilter(isSenior).withMapping(gpaField).max();
 *   }
 *
 *   // helpers:
 *   static final class IsSenior implements Predicate&lt;Student&gt; {
 *     public boolean op(Student s) { return s.credits &gt; 90; }
 *   }
 *   static final IsSenior isSenior = new IsSenior();
 *   static final class GpaField implements ObjectToDouble&lt;Student&gt {
 *     public double op(Student s) { return s.gpa; }
 *   }
 *   static final GpaField gpaField = new GpaField();
 * }
 * </pre>
 */
public class ParallelArray<T> extends AbstractParallelAnyArray.OUPap<T> implements Iterable<T> {
    /*
     * See classes PAS and AbstractParallelAnyArray for most of the underlying parallel execution
     * code and explanation.
     */

    /**
     * Returns a common default executor for use in ParallelArrays.
     * This executor arranges enough parallelism to use most, but not
     * necessarily all, of the available processors on this system.
     * @return the executor
     */
    public static ForkJoinPool defaultExecutor() {
        return PAS.defaultExecutor();
    }

    /** Lazily constructed list view */
    AsList listView;

    /**
     * Constructor for use by subclasses to create a new ParallelArray
     * using the given executor, and initially using the supplied
     * array, with effective size bound by the given limit. This
     * constructor is designed to enable extensions via
     * subclassing. To create a ParallelArray, use {@link #create},
     * {@link #createEmpty}, {@link #createUsingHandoff} or {@link
     * #createFromCopy}.
     * @param executor the executor
     * @param array the array
     * @param limit the upper bound limit
     */
    protected ParallelArray(ForkJoinPool executor, T[] array, int limit) {
        super(executor, 0, limit, array);
        if (executor == null || array == null)
            throw new NullPointerException();
        if (limit < 0 || limit > array.length)
            throw new IllegalArgumentException();
    }

    /**
     * Trusted internal version of protected constructor.
     */
    ParallelArray(ForkJoinPool executor, T[] array) {
        super(executor, 0, array.length, array);
    }

    /**
     * Creates a new ParallelArray using the given executor and
     * an array of the given size constructed using the
     * indicated base element type.
     * @param size the array size
     * @param elementType the type of the elements
     * @param executor the executor
     */
    public static <T> ParallelArray<T> create
        (int size, Class<? super T> elementType,
         ForkJoinPool executor) {
        T[] array = (T[])Array.newInstance(elementType, size);
        return new ParallelArray<T>(executor, array, size);
    }

    /**
     * Creates a new ParallelArray initially using the given array and
     * executor. In general, the handed off array should not be used
     * for other purposes once constructing this ParallelArray.  The
     * given array may be internally replaced by another array in the
     * course of methods that add or remove elements.
     * @param handoff the array
     * @param executor the executor
     */
    public static <T> ParallelArray<T> createUsingHandoff
        (T[] handoff, ForkJoinPool executor) {
        return new ParallelArray<T>(executor, handoff, handoff.length);
    }

    /**
     * Creates a new ParallelArray using the given executor and
     * initially holding copies of the given
     * source elements.
     * @param source the source of initial elements
     * @param executor the executor
     */
    public static <T> ParallelArray<T> createFromCopy
        (T[] source, ForkJoinPool executor) {
        // For now, avoid copyOf so people can compile with Java5
        int size = source.length;
        T[] array = (T[])Array.newInstance
            (source.getClass().getComponentType(), size);
        System.arraycopy(source, 0, array, 0, size);
        return new ParallelArray<T>(executor, array, size);
    }

    /**
     * Creates a new ParallelArray using an array of the given size,
     * initially holding copies of the given source truncated or
     * padded with nulls to obtain the specified length.
     * @param source the source of initial elements
     * @param size the array size
     * @param executor the executor
     */
    public static <T> ParallelArray<T> createFromCopy
        (int size, T[] source, ForkJoinPool executor) {
        // For now, avoid copyOf so people can compile with Java5
        T[] array = (T[])Array.newInstance
            (source.getClass().getComponentType(), size);
        System.arraycopy(source, 0, array, 0,
                         Math.min(source.length, size));
        return new ParallelArray<T>(executor, array, size);
    }

    /**
     * Creates a new ParallelArray using the given executor and an
     * array of the given size constructed using the indicated base
     * element type, but with an initial effective size of zero,
     * enabling incremental insertion via {@link ParallelArray#asList}
     * operations.
     * @param size the array size
     * @param elementType the type of the elements
     * @param executor the executor
     */
    public static <T> ParallelArray<T> createEmpty
        (int size, Class<? super T> elementType,
         ForkJoinPool executor) {
        T[] array = (T[])Array.newInstance(elementType, size);
        return new ParallelArray<T>(executor, array, 0);
    }

    /**
     * Summary statistics for a possibly bounded, filtered, and/or
     * mapped ParallelArray.
     */
    public static interface SummaryStatistics<T> {
        /** Returns the number of elements */
        public int size();
        /** Returns the minimum element, or null if empty */
        public T min();
        /** Returns the maximum element, or null if empty */
        public T max();
        /** Returns the index of the minimum element, or -1 if empty */
        public int indexOfMin();
        /** Returns the index of the maximum element, or -1 if empty */
        public int indexOfMax();
    }

    /**
     * Returns the executor used for computations.
     * @return the executor
     */
    public ForkJoinPool getExecutor() { return ex; }

    /**
     * Applies the given procedure to elements.
     * @param procedure the procedure
     */
    public void apply(Procedure<? super T> procedure) {
        super.apply(procedure);
    }

    /**
     * Returns reduction of elements.
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return reduction
     */
    public T reduce(Reducer<T> reducer, T base) {
        return super.reduce(reducer, base);
    }

    /**
     * Returns a new ParallelArray holding all elements.
     * @return a new ParallelArray holding all elements
     */
    public ParallelArray<T> all() {
        return super.all();
    }

    /**
     * Returns a new ParallelArray with the given element type holding
     * all elements.
     * @param elementType the type of the elements
     * @return a new ParallelArray holding all elements
     */
    public ParallelArray<T> all(Class<? super T> elementType) {
        return super.all(elementType);
    }

    /**
     * Replaces elements with the results of applying the given transform
     * to their current values.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> replaceWithMapping
        (Op<? super T, ? extends T> op) {
        super.replaceWithMapping(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * mapping to their indices.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> replaceWithMappedIndex
        (IntToObject<? extends T> op) {
        super.replaceWithMappedIndex(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * mapping to each index and current element value.
     * @param op the op
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> replaceWithMappedIndex
        (IntAndObjectToObject<? super T, ? extends T> op) {
        super.replaceWithMappedIndex(op);
        return this;
    }

    /**
     * Replaces elements with the results of applying the given
     * generator.
     * @param generator the generator
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> replaceWithGeneratedValue
        (Generator<? extends T> generator) {
        super.replaceWithGeneratedValue(generator);
        return this;
    }

    /**
     * Replaces elements with the given value.
     * @param value the value
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> replaceWithValue(T value) {
        super.replaceWithValue(value);
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public <V,W> ParallelArray<T> replaceWithMapping
        (BinaryOp<? super T, ? super V, ? extends T> combiner,
         ParallelArrayWithMapping<W,V> other) {
        super.replaceWithMapping(combiner, other);
        return this;
    }

    /**
     * Replaces elements with results of applying
     * {@code op(thisElement, otherElement)}.
     * @param other the other array
     * @param combiner the combiner
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> replaceWithMapping
        (BinaryOp<T,T,T> combiner, T[] other) {
        super.replaceWithMapping(combiner, other);
        return this;
    }

    /**
     * Returns the index of some element equal to given target,
     * or -1 if not present.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public int indexOf(T target) {
        return super.indexOf(target);
    }

    /**
     * Assuming this array is sorted, returns the index of an element
     * equal to given target, or -1 if not present. If the array
     * is not sorted, the results are undefined.
     * @param target the element to search for
     * @return the index or -1 if not present
     */
    public int binarySearch(T target) {
        return super.binarySearch(target);
    }

    /**
     * Assuming this array is sorted with respect to the given
     * comparator, returns the index of an element equal to given
     * target, or -1 if not present. If the array is not sorted, the
     * results are undefined.
     * @param target the element to search for
     * @param comparator the comparator
     * @return the index or -1 if not present
     */
    public int binarySearch(T target, Comparator<? super T> comparator) {
        return super.binarySearch(target, comparator);
    }

    /**
     * Returns summary statistics, using the given comparator
     * to locate minimum and maximum elements.
     * @param comparator the comparator to use for
     * locating minimum and maximum elements
     * @return the summary
     */
    public ParallelArray.SummaryStatistics<T> summary
        (Comparator<? super T> comparator) {
        return super.summary(comparator);
    }

    /**
     * Returns summary statistics, assuming that all elements are
     * Comparables.
     * @return the summary
     */
    public ParallelArray.SummaryStatistics<T> summary() {
        return super.summary();
    }

    /**
     * Returns the minimum element, or null if empty.
     * @param comparator the comparator
     * @return minimum element, or null if empty
     */
    public T min(Comparator<? super T> comparator) {
        return super.min(comparator);
    }

    /**
     * Returns the minimum element, or null if empty,
     * assuming that all elements are Comparables.
     * @return minimum element, or null if empty
     * @throws ClassCastException if any element is not Comparable
     */
    public T min() {
        return super.min();
    }

    /**
     * Returns the maximum element, or null if empty.
     * @param comparator the comparator
     * @return maximum element, or null if empty
     */
    public T max(Comparator<? super T> comparator) {
        return super.max(comparator);
    }

    /**
     * Returns the maximum element, or null if empty,
     * assuming that all elements are Comparables.
     * @return maximum element, or null if empty
     * @throws ClassCastException if any element is not Comparable
     */
    public T max() {
        return super.max();
    }

    /**
     * Replaces each element with the running cumulation of applying
     * the given reducer. For example, if the contents are the numbers
     * {@code 1, 2, 3}, and the reducer operation adds numbers, then
     * after invocation of this method, the contents would be {@code 1,
     * 3, 6} (that is, {@code 1, 1+2, 1+2+3}).
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> cumulate(Reducer<T> reducer, T base) {
        super.cumulate(reducer, base);
        return this;
    }

    /**
     * Replaces each element with the cumulation of applying the given
     * reducer to all previous values, and returns the total
     * reduction. For example, if the contents are the numbers {@code 1,
     * 2, 3}, and the reducer operation adds numbers, then after
     * invocation of this method, the contents would be {@code 0, 1,
     * 3} (that is, {@code 0, 0+1, 0+1+2}, and the return value
     * would be 6 (that is, {@code  1+2+3}).
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return the total reduction
     */
    public T precumulate(Reducer<T> reducer, T base) {
        return super.precumulate(reducer, base);
    }

    /**
     * Sorts the array. Unlike Arrays.sort, this sort does
     * not guarantee that elements with equal keys maintain their
     * relative position in the array.
     * @param comparator the comparator to use
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> sort(Comparator<? super T> comparator) {
        super.sort(comparator);
        return this;
    }

    /**
     * Sorts the array, assuming all elements are Comparable. Unlike
     * Arrays.sort, this sort does not guarantee that elements
     * with equal keys maintain their relative position in the array.
     * @throws ClassCastException if any element is not Comparable
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> sort() {
        super.sort();
        return this;
    }

    /**
     * Returns a new ParallelArray containing only the non-null unique
     * elements of this array (that is, without any duplicates), using
     * each element's {@code equals} method to test for duplication.
     * @return the new ParallelArray
     */
    public ParallelArray<T> allUniqueElements() {
        return super.allUniqueElements();
    }

    /**
     * Returns a new ParallelArray containing only the non-null unique
     * elements of this array (that is, without any duplicates), using
     * reference identity to test for duplication.
     * @return the new ParallelArray
     */
    public ParallelArray<T> allNonidenticalElements() {
        return super.allNonidenticalElements();
    }

    /**
     * Removes from the array all elements for which the given
     * selector holds.
     * @param selector the selector
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> removeAll(Predicate<? super T> selector) {
        OFPap<T> v = new OFPap<T>(ex, 0, fence, array, selector);
        PAS.FJRemoveAllDriver f = new PAS.FJRemoveAllDriver(v, 0, fence);
        ex.invoke(f);
        removeSlotsAt(f.offset, fence);
        return this;
    }

    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are equal.
     * @param other the other array
     * @return true if equal
     */
    public <U,V> boolean hasAllEqualElements
        (ParallelArrayWithMapping<U,V> other) {
        return super.hasAllEqualElements(other);
    }

    /**
     * Returns true if all elements at the same relative positions
     * of this and other array are identical.
     * @param other the other array
     * @return true if equal
     */
    public <U,V> boolean hasAllIdenticalElements
        (ParallelArrayWithMapping<U,V> other) {
        return super.hasAllIdenticalElements(other);
    }

    /**
     * Removes consecutive elements that are equal (or null),
     * shifting others leftward, and possibly decreasing size.  This
     * method may be used after sorting to ensure that this
     * ParallelArray contains a set of unique elements.
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> removeConsecutiveDuplicates() {
        // Sequential implementation for now
        int k = 0;
        int n = fence;
        Object[] arr = this.array;
        Object last = null;
        for (int i = k; i < n; ++i) {
            Object x = arr[i];
            if (x != null && (last == null || !last.equals(x)))
                arr[k++] = last = x;
        }
        removeSlotsAt(k, n);
        return this;
    }

    /**
     * Removes null elements, shifting others leftward, and possibly
     * decreasing size.
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> removeNulls() {
        // Sequential implementation for now
        int k = 0;
        int n = fence;
        Object[] arr = this.array;
        for (int i = k; i < n; ++i) {
            Object x = arr[i];
            if (x != null)
                arr[k++] = x;
        }
        removeSlotsAt(k, n);
        return this;
    }

    /**
     * Equivalent to {@code asList().addAll} but specialized for array
     * arguments and likely to be more efficient.
     * @param other the elements to add
     * @return this (to simplify use in expressions)
     */
    public ParallelArray<T> addAll(T[] other) {
        int csize = other.length;
        int end = fence;
        insertSlotsAt(end, csize);
        System.arraycopy(other, 0, array, end, csize);
        return this;
    }

    /**
     * Appends all (possibly bounded, filtered, or mapped) elements of
     * the given ParallelArray, resizing and/or reallocating this
     * array if necessary.
     * @param other the elements to add
     * @return this (to simplify use in expressions)
     */
    public <V> ParallelArray<T> addAll
        (ParallelArrayWithMapping<V,T> other) {
        int end = fence;
        if (other.hasFilter()) {
            PAS.FJOAppendAllDriver r = new PAS.FJOAppendAllDriver
                (other, end, array);
            ex.invoke(r);
            array = (T[])(r.results);
            fence = end + r.resultSize;
        }
        else {
            int csize = other.size();
            insertSlotsAt(end, csize);
            if (other.hasMap())
                ex.invoke(new PAS.FJOMap(other, other.origin, other.fence,
                                         null, array, end - other.origin));
            else
                System.arraycopy(other.array, 0, array, end, csize);
        }
        return this;
    }

    /**
     * Returns an operation prefix that causes a method to
     * operate only on the elements of the array between
     * firstIndex (inclusive) and upperBound (exclusive).
     * @param firstIndex the lower bound (inclusive)
     * @param upperBound the upper bound (exclusive)
     * @return operation prefix
     */
    public ParallelArrayWithBounds<T> withBounds
        (int firstIndex, int upperBound) {
        return super.withBounds(firstIndex, upperBound);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on the elements of the array for which the given selector
     * returns true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelArrayWithFilter<T> withFilter
        (Predicate<? super T> selector) {
        return super.withFilter(selector);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the given binary selector returns
     * true.
     * @param selector the selector
     * @return operation prefix
     */
    public <V,W> ParallelArrayWithFilter<T> withFilter
        (BinaryPredicate<? super T, ? super V> selector,
         ParallelArrayWithMapping<W,V> other) {
        return super.withFilter(selector, other);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on elements for which the given indexed selector returns
     * true.
     * @param selector the selector
     * @return operation prefix
     */
    public ParallelArrayWithFilter<T> withIndexedFilter
        (IntAndObjectPredicate<? super T> selector) {
        return super.withIndexedFilter(selector);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public <U> ParallelArrayWithMapping<T,U> withMapping
        (Op<? super T, ? extends U> op) {
        return super.withMapping(op);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public ParallelArrayWithDoubleMapping<T> withMapping
        (ObjectToDouble<? super T> op) {
        return super.withMapping(op);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given op.
     * @param op the op
     * @return operation prefix
     */
    public ParallelArrayWithLongMapping<T> withMapping
        (ObjectToLong<? super T> op) {
        return super.withMapping(op);
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
    public <U,V,W> ParallelArrayWithMapping<T,V> withMapping
        (BinaryOp<? super T, ? super U, ? extends V> combiner,
         ParallelArrayWithMapping<W,U> other) {
        return super.withMapping(combiner, other);
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
        (ObjectAndDoubleToObject<? super T, ? extends V> combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        return super.withMapping(combiner, other);
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
        (ObjectAndLongToObject<? super T, ? extends V> combiner,
         ParallelLongArrayWithLongMapping other) {
        return super.withMapping(combiner, other);
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
    public <U,W> ParallelArrayWithDoubleMapping<T> withMapping
        (ObjectAndObjectToDouble<? super T, ? super U> combiner,
         ParallelArrayWithMapping<W,U> other) {
        return super.withMapping(combiner, other);
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
        (ObjectAndDoubleToDouble<? super T> combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        return super.withMapping(combiner, other);
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
        (ObjectAndLongToDouble<? super T> combiner,
         ParallelLongArrayWithLongMapping other) {
        return super.withMapping(combiner, other);
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
    public <U,W> ParallelArrayWithLongMapping<T> withMapping
        (ObjectAndObjectToLong<? super T, ? super U> combiner,
         ParallelArrayWithMapping<W,U> other) {
        return super.withMapping(combiner, other);
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
        (ObjectAndDoubleToLong<? super T> combiner,
         ParallelDoubleArrayWithDoubleMapping other) {
        return super.withMapping(combiner, other);
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
        (ObjectAndLongToLong<? super T> combiner,
         ParallelLongArrayWithLongMapping other) {
        return super.withMapping(combiner, other);
    }

    /**
     * Returns an operation prefix that causes a method to operate on
     * mappings of this array using the given mapper that accepts as
     * arguments an element's current index and value, and produces a
     * new value. Index-based mappings allow parallel computation of
     * many common array operations. For example, you could create
     * function to average the values at the same index of multiple
     * arrays and apply it using this method.
     * @param mapper the mapper
     * @return operation prefix
     */
    public <U> ParallelArrayWithMapping<T,U> withIndexedMapping
        (IntAndObjectToObject<? super T, ? extends U> mapper) {
        return super.withIndexedMapping(mapper);
    }

    /**
     * Returns an operation prefix that causes a method to operate on
     * mappings of this array using the given mapper that accepts as
     * arguments an element's current index and value, and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public ParallelArrayWithDoubleMapping<T> withIndexedMapping
        (IntAndObjectToDouble<? super T> mapper) {
        return super.withIndexedMapping(mapper);
    }

    /**
     * Returns an operation prefix that causes a method to operate on
     * mappings of this array using the given mapper that accepts as
     * arguments an element's current index and value, and produces a
     * new value.
     * @param mapper the mapper
     * @return operation prefix
     */
    public ParallelArrayWithLongMapping<T> withIndexedMapping
        (IntAndObjectToLong<? super T> mapper) {
        return super.withIndexedMapping(mapper);
    }

    /**
     * Returns an iterator stepping through each element of the array
     * up to the current limit. This iterator does <em>not</em>
     * support the remove operation. However, a full
     * {@code ListIterator} supporting add, remove, and set
     * operations is available via {@link #asList}.
     * @return an iterator stepping through each element
     */
    public Iterator<T> iterator() {
        return new ParallelArrayIterator<T>(array, fence);
    }

    static final class ParallelArrayIterator<T> implements Iterator<T> {
        int cursor;
        final T[] arr;
        final int hi;
        ParallelArrayIterator(T[] a, int limit) { arr = a; hi = limit; }
        public boolean hasNext() { return cursor < hi; }
        public T next() {
            if (cursor >= hi)
                throw new NoSuchElementException();
            return arr[cursor++];
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // List support

    /**
     * Returns a view of this ParallelArray as a List. This List has
     * the same structural and performance characteristics as {@link
     * ArrayList}, and may be used to modify, replace or extend the
     * bounds of the array underlying this ParallelArray.  The methods
     * supported by this list view are <em>not</em> in general
     * implemented as parallel operations. This list is also not
     * itself thread-safe.  In particular, performing list updates
     * while other parallel operations are in progress has undefined
     * (and surely undesired) effects.
     * @return a list view
     */
    public List<T> asList() {
        AsList lv = listView;
        if (lv == null)
            listView = lv = new AsList();
        return lv;
    }

    /**
     * Returns the effective size of the underlying array. The
     * effective size is the current limit, if used (see {@link
     * #setLimit}), or the length of the array otherwise.
     * @return the effective size of array
     */
    public int size() { return fence; }

    /**
     * Returns the element of the array at the given index.
     * @param i the index
     * @return the element of the array at the given index
     */
    public T get(int i) { return array[i]; }

    /**
     * Sets the element of the array at the given index to the given value.
     * @param i the index
     * @param x the value
     */
    public void set(int i, T x) { array[i] = x; }

    /**
     * Returns the underlying array used for computations.
     * @return the array
     */
    public T[] getArray() { return array; }

    /**
     * Equivalent to {@code asList().toString()}.
     * @return a string representation
     */
    public String toString() {
        return asList().toString();
    }


    /**
     * Ensures that the underlying array can be accessed up to the
     * given upper bound, reallocating and copying the underlying
     * array to expand if necessary. Or, if the given limit is less
     * than the length of the underlying array, causes computations to
     * ignore elements past the given limit.
     * @param newLimit the new upper bound
     * @throws IllegalArgumentException if newLimit less than zero
     */
    public final void setLimit(int newLimit) {
        if (newLimit < 0)
            throw new IllegalArgumentException();
        int cap = array.length;
        if (newLimit > cap)
            resizeArray(newLimit);
        fence = newLimit;
    }

    final void replaceElementsWith(T[] a) {
        System.arraycopy(a, 0, array, 0, a.length);
        fence = a.length;
    }

    final void resizeArray(int newCap) {
        int cap = array.length;
        if (newCap > cap) {
            Class elementType = array.getClass().getComponentType();
            T[] a =(T[])Array.newInstance(elementType, newCap);
            System.arraycopy(array, 0, a, 0, cap);
            array = a;
        }
    }

    final void insertElementAt(int index, T e) {
        int hi = fence++;
        if (hi >= array.length)
            resizeArray((hi * 3)/2 + 1);
        if (hi > index)
            System.arraycopy(array, index, array, index+1, hi - index);
        array[index] = e;
    }

    final void appendElement(T e) {
        int hi = fence++;
        if (hi >= array.length)
            resizeArray((hi * 3)/2 + 1);
        array[hi] = e;
    }

    /**
     * Makes len slots available at index.
     */
    final void insertSlotsAt(int index, int len) {
        if (len <= 0)
            return;
        int cap = array.length;
        int newSize = fence + len;
        if (cap < newSize) {
            cap = (cap * 3)/2 + 1;
            if (cap < newSize)
                cap = newSize;
            resizeArray(cap);
        }
        if (index < fence)
            System.arraycopy(array, index, array, index + len, fence - index);
        fence = newSize;
    }

    final void removeSlotAt(int index) {
        System.arraycopy(array, index + 1, array, index, fence - index - 1);
        array[--fence] = null;
    }

    final void removeSlotsAt(int fromIndex, int toIndex) {
        if (fromIndex < toIndex) {
            int size = fence;
            System.arraycopy(array, toIndex, array, fromIndex, size - toIndex);
            int newSize = size - (toIndex - fromIndex);
            fence = newSize;
            while (size > newSize)
                array[--size] = null;
        }
    }

    final int seqIndexOf(Object target) {
        T[] arr = array;
        int end = fence;
        if (target == null) {
            for (int i = 0; i < end; i++)
                if (arr[i] == null)
                    return i;
        } else {
            for (int i = 0; i < end; i++)
                if (target.equals(arr[i]))
                    return i;
        }
        return -1;
    }

    final int seqLastIndexOf(Object target) {
        T[] arr = array;
        int last = fence - 1;
        if (target == null) {
            for (int i = last; i >= 0; i--)
                if (arr[i] == null)
                    return i;
        } else {
            for (int i = last; i >= 0; i--)
                if (target.equals(arr[i]))
                    return i;
        }
        return -1;
    }

    final class ListIter implements ListIterator<T> {
        int cursor;
        int lastRet;
        T[] arr; // cache array and bound
        int hi;
        ListIter(int lo) {
            this.cursor = lo;
            this.lastRet = -1;
            this.arr = ParallelArray.this.array;
            this.hi = ParallelArray.this.fence;
        }

        public boolean hasNext() {
            return cursor < hi;
        }

        public T next() {
            int i = cursor;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            T next = arr[i];
            lastRet = i;
            cursor = i + 1;
            return next;
        }

        public void remove() {
            int k = lastRet;
            if (k < 0)
                throw new IllegalStateException();
            ParallelArray.this.removeSlotAt(k);
            hi = ParallelArray.this.fence;
            if (lastRet < cursor)
                cursor--;
            lastRet = -1;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public T previous() {
            int i = cursor - 1;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            T previous = arr[i];
            lastRet = cursor = i;
            return previous;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public void set(T e) {
            int i = lastRet;
            if (i < 0 || i >= hi)
                throw new NoSuchElementException();
            arr[i] = e;
        }

        public void add(T e) {
            int i = cursor;
            ParallelArray.this.insertElementAt(i, e);
            arr = ParallelArray.this.array;
            hi = ParallelArray.this.fence;
            lastRet = -1;
            cursor = i + 1;
        }
    }

    final class AsList extends AbstractList<T> implements RandomAccess {
        public T get(int i) {
            if (i >= fence)
                throw new IndexOutOfBoundsException();
            return array[i];
        }

        public T set(int i, T x) {
            if (i >= fence)
                throw new IndexOutOfBoundsException();
            T[] arr = array;
            T t = arr[i];
            arr[i] = x;
            return t;
        }

        public boolean isEmpty() {
            return fence == 0;
        }

        public int size() {
            return fence;
        }

        public Iterator<T> iterator() {
            return new ListIter(0);
        }

        public ListIterator<T> listIterator() {
            return new ListIter(0);
        }

        public ListIterator<T> listIterator(int index) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            return new ListIter(index);
        }

        public boolean add(T e) {
            appendElement(e);
            return true;
        }

        public void add(int index, T e) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            insertElementAt(index, e);
        }

        public boolean addAll(Collection<? extends T> c) {
            int csize = c.size();
            if (csize == 0)
                return false;
            int hi = fence;
            setLimit(hi + csize);
            T[] arr = array;
            for (T e : c)
                arr[hi++] = e;
            return true;
        }

        public boolean addAll(int index, Collection<? extends T> c) {
            if (index < 0 || index > fence)
                throw new IndexOutOfBoundsException();
            int csize = c.size();
            if (csize == 0)
                return false;
            insertSlotsAt(index, csize);
            T[] arr = array;
            for (T e : c)
                arr[index++] = e;
            return true;
        }

        public void clear() {
            T[] arr = array;
            for (int i = 0; i < fence; ++i)
                arr[i] = null;
            fence = 0;
        }

        public boolean remove(Object o) {
            int idx = seqIndexOf(o);
            if (idx < 0)
                return false;
            removeSlotAt(idx);
            return true;
        }

        public T remove(int index) {
            T oldValue = get(index);
            removeSlotAt(index);
            return oldValue;
        }

        public void removeRange(int fromIndex, int toIndex) {
            removeSlotsAt(fromIndex, toIndex);
        }

        public boolean contains(Object o) {
            return seqIndexOf(o) >= 0;
        }

        public int indexOf(Object o) {
            return seqIndexOf(o);
        }

        public int lastIndexOf(Object o) {
            return seqLastIndexOf(o);
        }

        public Object[] toArray() {
            int len = fence;
            Object[] a = new Object[len];
            System.arraycopy(array, 0, a, 0, len);
            return a;
        }

        public <V> V[] toArray(V a[]) {
            int len = fence;
            if (a.length < len) {
                Class elementType = a.getClass().getComponentType();
                a =(V[])Array.newInstance(elementType, len);
            }
            System.arraycopy(array, 0, a, 0, len);
            if (a.length > len)
                a[len] = null;
            return a;
        }
    }

}
