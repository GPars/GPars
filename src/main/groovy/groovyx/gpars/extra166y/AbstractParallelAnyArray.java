/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ForkJoinPool;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static groovyx.gpars.extra166y.Ops.*;

/**
 * Abstract class serving as the basis of parallel
 * array classes across types.
 */
public abstract class AbstractParallelAnyArray {
    /*
     * This class and its subclasses (most of which are defined here
     * as nested static classes) maintain the execution parameters for
     * ParallelArray, ParallelDoubleArray, and ParallelLongArray
     * tasks.  Pap instances hold the non-operation-specific control
     * and data accessors needed for a task as a whole (as opposed to
     * subtasks), and also house some of the leaf methods that perform
     * the actual array processing. The leaf methods are for the most
     * part just plain array operations. They are boringly repetitive
     * in order to flatten out and minimize inner-loop overhead, as
     * well as to minimize call-chain depth. This makes it more likely
     * that dynamic compilers can go the rest of the way, and hoist
     * per-element method call dispatch, so we have a good chance to
     * speed up processing via parallelism rather than lose due to
     * dispatch and indirection overhead. The dispatching from Pap to
     * FJ and back is otherwise Visitor-pattern-like, allowing the
     * basic parallelism control for most FJ tasks to be centralized.
     *
     * Note the extensive use of raw types. Arrays and generics do not
     * work together very well. It is more manageable to avoid them here,
     * and let the public classes perform casts in and out to the
     * processing here. Also note that the majority of code in concrete
     * classes is just for managing the various flavors created using
     * with* methods.
     *
     * Internal concrete classes are named using an
     * abbreviation scheme to avoid mile-long class names:
     * O, D, L for Object, Double, Long, for underlying Parallel array
     * U - unfiltered
     * F - filtered
     * R - relation-filtered (aka index-filtered)
     * OM, DM, LM - Mapped
     * OC, DC, LC - combiner-mapped (aka index-mapped)
     */

    final ForkJoinPool ex;
    final int origin;
    int fence;
    int threshold;

    AbstractParallelAnyArray(ForkJoinPool ex, int origin, int fence) {
        this.ex = ex;
        this.origin = origin;
        this.fence = fence;
    }

    // A few public methods exported across all subclasses

    /**
     * Returns the number of elements selected using bound or
     * filter restrictions. Note that this method must evaluate
     * all selectors to return its result.
     * @return the number of elements
     */
    public int size() {
        if (!hasFilter())
            return fence - origin;
        PAS.FJCountSelected f = new PAS.FJCountSelected
            (this, origin, fence, null);
        ex.invoke(f);
        return f.count;
    }

    /**
     * Returns the index of some element matching bound and filter
     * constraints, or -1 if none.
     * @return index of matching element, or -1 if none
     */
    public int anyIndex() {
        if (!hasFilter())
            return (origin < fence) ? origin : -1;
        AtomicInteger result = new AtomicInteger(-1);
        PAS.FJSelectAny f = new PAS.FJSelectAny
            (this, origin, fence, null, result);
        ex.invoke(f);
        return result.get();
    }

    /**
     * Returns true if there are no elements.
     * @return true if there are no elements
     */
    public boolean isEmpty() {
        return anyIndex() < 0;
    }


    /**
     * Returns size threshold for splitting into subtask.  By
     * default, uses about 8 times as many tasks as threads
     */
    final int computeThreshold() {
        int n = fence - origin;
        int p = ex.getParallelism();
        return threshold = (p > 1) ? (1 + n / (p << 3)) : n;
    }

    /**
     * Returns lazily computed threshold.
     */
    final int getThreshold() {
        int t = threshold;
        if (t == 0)
            t = computeThreshold();
        return t;
    }

    /**
     * Access methods for ref, double, long. Checking for
     * null/false return is used as a sort of type test.  These
     * are used to avoid duplication in non-performance-critical
     * aspects of control, as well as to provide a simple default
     * mechanism for extensions.
     */
    Object[] ogetArray() { return null; }
    double[] dgetArray() { return null; }
    long[]  lgetArray() { return null; }
    abstract Object oget(int index);
    abstract double dget(int index);
    abstract long lget(int index);
    boolean hasMap() { return false; }
    boolean hasFilter() { return false; }
    boolean isSelected(int index) { return true; }

    /*
     * Leaf methods for FJ tasks. Default versions use isSelected,
     * oget, dget, etc. But most are overridden in most concrete
     * classes to avoid per-element dispatching.
     */

    void leafApply(int lo, int hi, Procedure procedure) {
        for (int i = lo; i < hi; ++i)
            if (isSelected(i))
                procedure.op(oget(i));
    }

    void leafApply(int lo, int hi, DoubleProcedure procedure) {
        for (int i = lo; i < hi; ++i)
            if (isSelected(i))
                procedure.op(dget(i));
    }

    void leafApply(int lo, int hi, LongProcedure procedure) {
        for (int i = lo; i < hi; ++i)
            if (isSelected(i))
                procedure.op(lget(i));
    }

    Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
        boolean gotFirst = false;
        Object r = base;
        for (int i = lo; i < hi; ++i) {
            if (isSelected(i)) {
                Object x = oget(i);
                if (!gotFirst) {
                    gotFirst = true;
                    r = x;
                }
                else
                    r = reducer.op(r, x);
            }
        }
        return r;
    }

    double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
        boolean gotFirst = false;
        double r = base;
        for (int i = lo; i < hi; ++i) {
            if (isSelected(i)) {
                double x = dget(i);
                if (!gotFirst) {
                    gotFirst = true;
                    r = x;
                }
                else
                    r = reducer.op(r, x);
            }
        }
        return r;
    }

    long leafReduce(int lo, int hi, LongReducer reducer, long base) {
        boolean gotFirst = false;
        long r = base;
        for (int i = lo; i < hi; ++i) {
            if (isSelected(i)) {
                long x = lget(i);
                if (!gotFirst) {
                    gotFirst = true;
                    r = x;
                }
                else
                    r = reducer.op(r, x);
            }
        }
        return r;
    }

    // copy elements, ignoring selector, but applying mapping
    void leafTransfer(int lo, int hi, Object[] dest, int offset) {
        for (int i = lo; i < hi; ++i)
            dest[offset++] = oget(i);
    }

    void leafTransfer(int lo, int hi, double[] dest, int offset) {
        for (int i = lo; i < hi; ++i)
            dest[offset++] = dget(i);
    }

    void leafTransfer(int lo, int hi, long[] dest, int offset) {
        for (int i = lo; i < hi; ++i)
            dest[offset++] = lget(i);
    }

    // copy elements indexed in indices[loIdx..hiIdx], ignoring
    // selector, but applying mapping
    void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                             Object[] dest, int offset) {
        for (int i = loIdx; i < hiIdx; ++i)
            dest[offset++] = oget(indices[i]);
    }

    void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                             double[] dest, int offset) {
        for (int i = loIdx; i < hiIdx; ++i)
            dest[offset++] = dget(indices[i]);
    }

    void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                             long[] dest, int offset) {
        for (int i = loIdx; i < hiIdx; ++i)
            dest[offset++] = lget(indices[i]);
    }

    // add indices of selected elements to index array; return #added
    final int leafIndexSelected(int lo, int hi, boolean positive,
                                int[] indices) {
        int k = 0;
        for (int i = lo; i < hi; ++i) {
            if (isSelected(i) == positive)
                indices[lo + k++] = i;
        }
        return k;
    }

    // move selected elements to indices starting at offset,
    // return final offset
    abstract int leafMoveSelected(int lo, int hi, int offset,
                                  boolean positive);

    // move elements indexed by indices[loIdx...hiIdx] starting
    // at given offset
    abstract void leafMoveByIndex(int[] indices, int loIdx,
                                  int hiIdx, int offset);

    /**
     * Shared support for select/map all -- probe filter, map, and
     * type to start selection driver, or do parallel mapping, or
     * just copy.
     */
    final Object[] allObjects(Class elementType) {
        if (hasFilter()) {
            if (elementType == null) {
                if (!hasMap())
                    elementType = ogetArray().getClass().getComponentType();
                else
                    elementType = Object.class;
            }
            PAS.FJOSelectAllDriver r = new PAS.FJOSelectAllDriver
                (this, elementType);
            ex.invoke(r);
            return r.results;
        }
        else {
            int n = fence - origin;
            Object[] dest;
            if (hasMap()) {
                if (elementType == null)
                    dest = new Object[n];
                else
                    dest = (Object[])Array.newInstance(elementType, n);
                ex.invoke(new PAS.FJOMap(this, origin, fence,
                                         null, dest, -origin));
            }
            else {
                Object[] array = ogetArray();
                if (elementType == null)
                    elementType = array.getClass().getComponentType();
                dest = (Object[])Array.newInstance(elementType, n);
                System.arraycopy(array, origin, dest, 0, n);
            }
            return dest;
        }
    }

    final double[] allDoubles() {
        if (hasFilter()) {
            PAS.FJDSelectAllDriver r = new PAS.FJDSelectAllDriver(this);
            ex.invoke(r);
            return r.results;
        }
        else {
            int n = fence - origin;
            double[] dest = new double[n];
            if (hasMap()) {
                ex.invoke(new PAS.FJDMap(this, origin, fence,
                                         null, dest, -origin));
            }
            else {
                double[] array = dgetArray();
                System.arraycopy(array, origin, dest, 0, n);
            }
            return dest;
        }
    }

    final long[] allLongs() {
        if (hasFilter()) {
            PAS.FJLSelectAllDriver r = new PAS.FJLSelectAllDriver(this);
            ex.invoke(r);
            return r.results;
        }
        else {
            int n = fence - origin;
            long[] dest = new long[n];
            if (hasMap()) {
                ex.invoke(new PAS.FJLMap(this, origin, fence,
                                         null, dest, -origin));
            }
            else {
                long[] array = lgetArray();
                System.arraycopy(array, origin, dest, 0, n);
            }
            return dest;
        }
    }

    // Bounds check a range
    void boundsCheck(int lo, int hi) {
        if (lo > hi)
            throw new IllegalArgumentException(origin + " > " + fence);
        if (lo < 0)
            throw new ArrayIndexOutOfBoundsException(origin);
        if (hi - lo > this.fence - this.origin)
            throw new ArrayIndexOutOfBoundsException(fence);
    }

    /*
     * The following methods can be called only for classes
     * supporting in-place replacements (currently, those classes
     * without mappings).  They are declared as no-ops here, and
     * overridden only where applicable.
     */

    void leafTransform(int l, int h, Op op) {}
    void leafIndexMap(int l, int h, IntToObject op) {}
    void leafBinaryIndexMap(int l, int h, IntAndObjectToObject op) {}
    void leafGenerate(int l, int h, Generator generator) {}
    void leafFill(int l, int h, Object value) {}
    void leafCombineInPlace(int lo, int hi, Object[] other,
                            int otherOffset, BinaryOp combiner) {}
    void leafCombineInPlace(int lo, int hi, ParallelArrayWithMapping other,
                            int otherOffset, BinaryOp combiner) {}

    void leafTransform(int l, int h, DoubleOp op) {}
    void leafIndexMap(int l, int h, IntToDouble array) {}
    void leafBinaryIndexMap(int l, int h, IntAndDoubleToDouble op) {}
    void leafGenerate(int l, int h, DoubleGenerator generator) {}
    void leafFill(int l, int h, double value) {}
    void leafCombineInPlace(int lo, int hi, double[] other,
                            int otherOffset, BinaryDoubleOp combiner) {}
    void leafCombineInPlace(int lo, int hi,
                            ParallelDoubleArrayWithDoubleMapping other,
                            int otherOffset, BinaryDoubleOp combiner) {}

    void leafTransform(int l, int h, LongOp op) {}
    void leafIndexMap(int l, int h, IntToLong array) {}
    void leafBinaryIndexMap(int l, int h, IntAndLongToLong op) {}
    void leafGenerate(int l, int h, LongGenerator generator) {}
    void leafFill(int l, int h, long value) {}
    void leafCombineInPlace(int lo, int hi, long[] other,
                            int otherOffset, BinaryLongOp combiner) {}
    void leafCombineInPlace(int lo, int hi,
                            ParallelLongArrayWithLongMapping other,
                            int otherOffset, BinaryLongOp combiner) {}

    // Base of object ref array classes
    abstract static class OPap<T> extends AbstractParallelAnyArray {
        T[] array;
        OPap(ForkJoinPool ex, int origin, int fence, T[] array) {
            super(ex, origin, fence);
            this.array = array;
        }

        final Object[] ogetArray() { return this.array; }
        double dget(int i) { return ((Number)oget(i)).doubleValue(); }
        long lget(int i) { return ((Number)oget(i)).longValue(); }

        final void leafMoveByIndex(int[] indices, int loIdx,
                                   int hiIdx, int offset) {
            final Object[] array = this.array;
            for (int i = loIdx; i < hiIdx; ++i)
                array[offset++] = array[indices[i]];
        }

        final int leafMoveSelected(int lo, int hi, int offset,
                                   boolean positive) {
            final Object[] array = this.array;
            for (int i = lo; i < hi; ++i) {
                if (isSelected(i) == positive)
                    array[offset++] = array[i];
            }
            return offset;
        }
    }

    // Base of double array classes
    abstract static class DPap extends AbstractParallelAnyArray {
        double[] array;
        DPap(ForkJoinPool ex, int origin, int fence, double[] array) {
            super(ex, origin, fence);
            this.array = array;
        }

        final double[] dgetArray() { return this.array; }
        Object oget(int i) { return Double.valueOf(dget(i)); }
        long lget(int i) { return (long)(dget(i)); }

        final void leafMoveByIndex(int[] indices, int loIdx,
                                   int hiIdx, int offset) {
            final double[] array = this.array;
            for (int i = loIdx; i < hiIdx; ++i)
                array[offset++] = array[indices[i]];
        }

        final int leafMoveSelected(int lo, int hi, int offset,
                                   boolean positive) {
            final double[] array = this.array;
            for (int i = lo; i < hi; ++i) {
                if (isSelected(i) == positive)
                    array[offset++] = array[i];
            }
            return offset;
        }
    }

    // Base of long array classes
    abstract static class LPap extends AbstractParallelAnyArray {
        long[] array;
        LPap(ForkJoinPool ex, int origin, int fence, long[] array) {
            super(ex, origin, fence);
            this.array = array;
        }

        final long[] lgetArray() { return this.array; }
        Object oget(int i) { return Long.valueOf(lget(i)); }
        double dget(int i) { return (double)(lget(i)); }

        final void leafMoveByIndex(int[] indices, int loIdx,
                                   int hiIdx, int offset) {
            final long[] array = this.array;
            for (int i = loIdx; i < hiIdx; ++i)
                array[offset++] = array[indices[i]];
        }

        final int leafMoveSelected(int lo, int hi, int offset,
                                   boolean positive) {
            final long[] array = this.array;
            for (int i = lo; i < hi; ++i) {
                if (isSelected(i) == positive)
                    array[offset++] = array[i];
            }
            return offset;
        }
    }

    // Plain (unfiltered, unmapped) classes
    static class OUPap<T> extends ParallelArrayWithBounds<T> {
        OUPap(ForkJoinPool ex, int origin, int fence, T[] array) {
            super(ex, origin, fence, array);
        }

        public ParallelArrayWithBounds<T> withBounds(int lo, int hi) {
            boundsCheck(lo, hi);
            return new OUPap<T>(ex, origin + lo, origin + hi, array);
        }

        public ParallelArrayWithFilter<T> withFilter
            (Predicate<? super T> selector) {
            return new OFPap<T>(ex, origin, fence, array, selector);
        }

        public ParallelArrayWithFilter<T> withIndexedFilter
            (IntAndObjectPredicate<? super T> selector) {
            return new ORPap<T>(ex, origin, fence, array, selector);
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (Op<? super T, ? extends U> op) {
            return new OUOMPap<T,U>(ex, origin, fence, array, op);
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super T> op) {
            return new OUDMPap<T>(ex, origin, fence, array, op);
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super T> op) {
            return new OULMPap<T>(ex, origin, fence, array, op);
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super T, ? extends V> mapper) {
            return new OUOCPap<T,V>(ex, origin, fence, array, mapper);
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super T> mapper) {
            return new OUDCPap<T>(ex, origin, fence, array, mapper);
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super T> mapper) {
            return new OULCPap<T>(ex, origin, fence, array, mapper);
        }

        public int indexOf(T target) {
            AtomicInteger result = new AtomicInteger(-1);
            PAS.FJOIndexOf f = new PAS.FJOIndexOf
                (this, origin, fence, null, result, target);
            ex.invoke(f);
            return result.get();
        }

        public int binarySearch(T target) {
            final Object[] a = this.array;
            int lo = origin;
            int hi = fence - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int c = ((Comparable)target).compareTo((Comparable)a[mid]);
                if (c == 0)
                    return mid;
                else if (c < 0)
                    hi = mid - 1;
                else
                    lo = mid + 1;
            }
            return -1;
        }

        public int binarySearch(T target, Comparator<? super T> comparator) {
            Comparator cmp = comparator;
            final Object[] a = this.array;
            int lo = origin;
            int hi = fence - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int c = cmp.compare(target, a[mid]);
                if (c == 0)
                    return mid;
                else if (c < 0)
                    hi = mid - 1;
                else
                    lo = mid + 1;
            }
            return -1;
        }

        public ParallelArrayWithBounds<T> cumulate(Reducer<T> reducer, T base) {
            PAS.FJOCumulateOp op = new PAS.FJOCumulateOp(this, reducer, base);
            PAS.FJOScan r = new PAS.FJOScan(null, op, origin, fence);
            ex.invoke(r);
            return this;
        }

        public T precumulate(Reducer<T> reducer, T base) {
            PAS.FJOPrecumulateOp op = new PAS.FJOPrecumulateOp
                (this, reducer, base);
            PAS.FJOScan r = new PAS.FJOScan(null, op, origin, fence);
            ex.invoke(r);
            return (T)(r.out);
        }

        public ParallelArrayWithBounds<T> sort
            (Comparator<? super T> cmp) {
            final Object[] a = this.array;
            Class tc = array.getClass().getComponentType();
            T[] ws = (T[])Array.newInstance(tc, fence);
            ex.invoke(new PAS.FJOSorter
                      (cmp, array, ws, origin,
                       fence - origin, getThreshold()));
            return this;
        }

        public ParallelArrayWithBounds<T> sort() {
            final Object[] a = this.array;
            Class tc = array.getClass().getComponentType();
            if (!Comparable.class.isAssignableFrom(tc)) {
                sort(CommonOps.castedComparator());
            }
            else {
                Comparable[] ca = (Comparable[])array;
                Comparable[] ws = (Comparable[])Array.newInstance(tc, fence);
                ex.invoke(new PAS.FJOCSorter
                          (ca, ws, origin,
                           fence - origin, getThreshold()));
            }
            return this;
        }

        final void leafApply(int lo, int hi, Procedure procedure) {
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(a[i]);
        }

        final Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            if (lo >= hi)
                return base;
            final Object[] a = this.array;
            Object r = a[lo];
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, a[i]);
            return r;
        }

        final void leafTransform(int l, int h, Op op) {
            final Object[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(a[i]);
        }

        final void leafIndexMap(int l, int h, IntToObject op) {
            final Object[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(i);
        }

        final void leafBinaryIndexMap(int l, int h, IntAndObjectToObject op) {
            final Object[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(i, a[i]);
        }

        final void leafGenerate(int l, int h, Generator generator) {
            final Object[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = generator.op();
        }

        final void leafFill(int l, int h, Object value) {
            final Object[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = value;
        }

        final void leafCombineInPlace(int l, int h, Object[] other,
                                      int otherOffset, BinaryOp combiner) {
            final Object[] a = this.array;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i)
                a[i] = combiner.op(a[i], other[k++]);
        }

        final void leafCombineInPlace(int l, int h,
                                      ParallelArrayWithMapping other,
                                      int otherOffset, BinaryOp combiner) {
            final Object[] a = this.array;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    if (other.isSelected(k))
                        a[i] = combiner.op(a[i], other.oget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i)
                    a[i] = combiner.op(a[i], other.oget(k++));
            }
            else {
                Object[] b = other.array;
                for (int i = l; i < h; ++i)
                    a[i] = combiner.op(a[i], b[k++]);
            }
        }
    }

    static class DUPap extends ParallelDoubleArrayWithBounds {
        DUPap(ForkJoinPool ex, int origin, int fence, double[] array) {
            super(ex, origin, fence, array);
        }

        public ParallelDoubleArrayWithBounds withBounds(int lo, int hi) {
            boundsCheck(lo, hi);
            return new DUPap(ex, origin + lo, origin + hi, array);
        }

        public ParallelDoubleArrayWithFilter withFilter(DoublePredicate selector) {
            return new DFPap(ex, origin, fence, array, selector);
        }

        public ParallelDoubleArrayWithFilter withIndexedFilter
            (IntAndDoublePredicate selector) {
            return new DRPap(ex, origin, fence, array, selector);
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DUOMPap<U>(ex, origin, fence, array, op);
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DUDMPap(ex, origin, fence, array, op);
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DULMPap(ex, origin, fence, array, op);
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DUOCPap<V>(ex, origin, fence, array, mapper);
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DUDCPap(ex, origin, fence, array, mapper);
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DULCPap(ex, origin, fence, array, mapper);
        }

        public int indexOf(double target) {
            AtomicInteger result = new AtomicInteger(-1);
            PAS.FJDIndexOf f = new PAS.FJDIndexOf
                (this, origin, fence, null, result, target);
            ex.invoke(f);
            return result.get();
        }

        public int binarySearch(double target) {
            final double[] a = this.array;
            int lo = origin;
            int hi = fence - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                double m = a[mid];
                if (target == m)
                    return mid;
                else if (target < m)
                    hi = mid - 1;
                else
                    lo = mid + 1;
            }
            return -1;
        }

        public int binarySearch(double target, DoubleComparator comparator) {
            final double[] a = this.array;
            int lo = origin;
            int hi = fence - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int c = comparator.compare(target, a[mid]);
                if (c == 0)
                    return mid;
                else if (c < 0)
                    hi = mid - 1;
                else
                    lo = mid + 1;
            }
            return -1;
        }

        public ParallelDoubleArrayWithBounds cumulate(DoubleReducer reducer,
                                                      double base) {
            PAS.FJDCumulateOp op = new PAS.FJDCumulateOp(this, reducer, base);
            PAS.FJDScan r = new PAS.FJDScan(null, op, origin, fence);
            ex.invoke(r);
            return this;
        }

        public ParallelDoubleArrayWithBounds cumulateSum() {
            PAS.FJDCumulatePlusOp op = new PAS.FJDCumulatePlusOp(this);
            PAS.FJDScan r = new PAS.FJDScan(null, op, origin, fence);
            ex.invoke(r);
            return this;
        }

        public double precumulate(DoubleReducer reducer, double base) {
            PAS.FJDPrecumulateOp op = new PAS.FJDPrecumulateOp(this, reducer, base);
            PAS.FJDScan r = new PAS.FJDScan(null, op, origin, fence);
            ex.invoke(r);
            return r.out;
        }

        public double precumulateSum() {
            PAS.FJDPrecumulatePlusOp op = new PAS.FJDPrecumulatePlusOp(this);
            PAS.FJDScan r = new PAS.FJDScan(null, op, origin, fence);
            ex.invoke(r);
            return r.out;
        }

        public ParallelDoubleArrayWithBounds sort(DoubleComparator cmp) {
            ex.invoke(new PAS.FJDSorter
                      (cmp, this.array, new double[fence],
                       origin, fence - origin, getThreshold()));
            return this;
        }

        public ParallelDoubleArrayWithBounds sort() {
            ex.invoke(new PAS.FJDCSorter
                      (this.array, new double[fence],
                       origin, fence - origin, getThreshold()));
            return this;
        }

        final void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(a[i]);
        }

        final double leafReduce(int lo, int hi, DoubleReducer reducer,
                                double base) {
            if (lo >= hi)
                return base;
            final double[] a = this.array;
            double r = a[lo];
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, a[i]);
            return r;
        }

        final void leafTransform(int l, int h, DoubleOp op) {
            final double[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(a[i]);
        }

        final void leafIndexMap(int l, int h, IntToDouble op) {
            final double[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(i);
        }

        final void leafBinaryIndexMap(int l, int h, IntAndDoubleToDouble op) {
            final double[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(i, a[i]);
        }

        final void leafGenerate(int l, int h, DoubleGenerator generator) {
            final double[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = generator.op();
        }

        final void leafFill(int l, int h, double value) {
            final double[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = value;
        }

        final void leafCombineInPlace
            (int l, int h, double[] other,
             int otherOffset, BinaryDoubleOp combiner) {
            final double[] a = this.array;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i)
                a[i] = combiner.op(a[i], other[k++]);
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelDoubleArrayWithDoubleMapping other,
             int otherOffset, BinaryDoubleOp combiner) {
            final double[] a = this.array;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    if (other.isSelected(k))
                        a[i] = combiner.op(a[i], other.dget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i)
                    a[i] = combiner.op(a[i], other.dget(k++));
            }
            else {
                double[] b = other.array;
                for (int i = l; i < h; ++i)
                    a[i] = combiner.op(a[i], b[k++]);
            }

        }
    }

    static class LUPap extends ParallelLongArrayWithBounds {
        LUPap(ForkJoinPool ex, int origin, int fence,
              long[] array) {
            super(ex, origin, fence, array);
        }

        public ParallelLongArrayWithBounds withBounds(int lo, int hi) {
            boundsCheck(lo, hi);
            return new LUPap(ex, origin + lo, origin + hi, array);
        }

        public ParallelLongArrayWithFilter withFilter(LongPredicate selector) {
            return new LFPap(ex, origin, fence, array, selector);
        }

        public ParallelLongArrayWithFilter withIndexedFilter
            (IntAndLongPredicate selector) {
            return new LRPap(ex, origin, fence, array, selector);
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new LUOMPap<U>(ex, origin, fence, array, op);
        }

        public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
            return new LULMPap(ex, origin, fence, array, op);
        }

        public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
            return new LUDMPap(ex, origin, fence, array, op);
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LUOCPap<V>(ex, origin, fence, array, mapper);
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LUDCPap(ex, origin, fence, array, mapper);
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LULCPap(ex, origin, fence, array, mapper);
        }

        public int indexOf(long target) {
            AtomicInteger result = new AtomicInteger(-1);
            PAS.FJLIndexOf f = new PAS.FJLIndexOf
                (this, origin, fence, null, result, target);
            ex.invoke(f);
            return result.get();
        }

        public int binarySearch(long target) {
            final long[] a = this.array;
            int lo = origin;
            int hi = fence - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                long m = a[mid];
                if (target == m)
                    return mid;
                else if (target < m)
                    hi = mid - 1;
                else
                    lo = mid + 1;
            }
            return -1;
        }

        public int binarySearch(long target, LongComparator comparator) {
            final long[] a = this.array;
            int lo = origin;
            int hi = fence - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int c = comparator.compare(target, a[mid]);
                if (c == 0)
                    return mid;
                else if (c < 0)
                    hi = mid - 1;
                else
                    lo = mid + 1;
            }
            return -1;
        }

        public ParallelLongArrayWithBounds cumulate(LongReducer reducer, long base) {
            PAS.FJLCumulateOp op = new PAS.FJLCumulateOp(this, reducer, base);
            PAS.FJLScan r = new PAS.FJLScan(null, op, origin, fence);
            ex.invoke(r);
            return this;
        }

        public ParallelLongArrayWithBounds cumulateSum() {
            PAS.FJLCumulatePlusOp op = new PAS.FJLCumulatePlusOp(this);
            PAS.FJLScan r = new PAS.FJLScan(null, op, origin, fence);
            ex.invoke(r);
            return this;
        }

        public long precumulate(LongReducer reducer, long base) {
            PAS.FJLPrecumulateOp op = new PAS.FJLPrecumulateOp
                (this, reducer, base);
            PAS.FJLScan r = new PAS.FJLScan(null, op, origin, fence);
            ex.invoke(r);
            return r.out;
        }

        public long precumulateSum() {
            PAS.FJLPrecumulatePlusOp op = new PAS.FJLPrecumulatePlusOp(this);
            PAS.FJLScan r = new PAS.FJLScan(null, op, origin, fence);
            ex.invoke(r);
            return r.out;
        }

        public ParallelLongArrayWithBounds sort(LongComparator cmp) {
            ex.invoke(new PAS.FJLSorter
                      (cmp, this.array, new long[fence],
                       origin, fence - origin, getThreshold()));
            return this;
        }

        public ParallelLongArrayWithBounds sort() {
            ex.invoke(new PAS.FJLCSorter
                      (this.array, new long[fence],
                       origin, fence - origin, getThreshold()));
            return this;
        }

        final void leafApply(int lo, int hi, LongProcedure procedure) {
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(a[i]);
        }

        final long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            final long[] a = this.array;
            long r = a[lo];
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, a[i]);
            return r;
        }

        final void leafTransform(int l, int h, LongOp op) {
            final long[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(a[i]);
        }

        final void leafIndexMap(int l, int h, IntToLong op) {
            final long[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(i);
        }

        final void leafBinaryIndexMap(int l, int h, IntAndLongToLong op) {
            final long[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = op.op(i, a[i]);
        }

        final void leafGenerate(int l, int h, LongGenerator generator) {
            final long[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = generator.op();
        }

        final void leafFill(int l, int h, long value) {
            final long[] a = this.array;
            for (int i = l; i < h; ++i)
                a[i] = value;
        }

        final void leafCombineInPlace
            (int l, int h, long[] other,
             int otherOffset, BinaryLongOp combiner) {
            final long[] a = this.array;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i)
                a[i] = combiner.op(a[i], other[k++]);
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelLongArrayWithLongMapping other,
             int otherOffset, BinaryLongOp combiner) {
            final long[] a = this.array;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    if (other.isSelected(k))
                        a[i] = combiner.op(a[i], other.lget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i)
                    a[i] = combiner.op(a[i], other.lget(k++));
            }
            else {
                long[] b = other.array;
                for (int i = l; i < h; ++i)
                    a[i] = combiner.op(a[i], b[k++]);
            }
        }
    }

    static final class AndPredicate<T> implements Predicate<T> {
        final Predicate<? super T> first;
        final Predicate<? super T> second;
        AndPredicate(Predicate<? super T> first,
                     Predicate<? super T> second) {
            this.first = first; this.second = second;
        }
        public final boolean op(T x) { return first.op(x) && second.op(x); }
    }

    // Filtered (but unmapped) classes
    static final class OFPap<T> extends ParallelArrayWithFilter<T> {
        final Predicate<? super T> selector;
        OFPap(ForkJoinPool ex, int origin, int fence,
              T[] array,
              Predicate<? super T> selector) {
            super(ex, origin, fence, array);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelArrayWithFilter<T> withFilter
            (Predicate<? super T> selector) {
            return new OFPap<T>(ex, origin, fence, array,
                                new AndPredicate(this.selector, selector));
        }

        public ParallelArrayWithFilter<T> withIndexedFilter
            (IntAndObjectPredicate<? super T> selector) {
            return new ORPap<T>
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (Op<? super T, ? extends U> op) {
            return new OFOMPap<T,U>(ex, origin, fence, array, selector, op);
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super T> op) {
            return new OFDMPap<T>(ex, origin, fence, array, selector, op);
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super T> op) {
            return new OFLMPap<T>(ex, origin, fence, array, selector, op);
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super T, ? extends V> mapper) {
            return new OFOCPap<T,V>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super T> mapper) {
            return new OFDCPap<T>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super T> mapper) {
            return new OFLCPap<T>(ex, origin, fence, array, selector, mapper);
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final Predicate s = selector;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x))
                    procedure.op(x);
            }
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final Predicate s = selector;
            boolean gotFirst = false;
            Object r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x)) {
                    if (!gotFirst) {
                        gotFirst = true;
                        r = x;
                    }
                    else
                        r = reducer.op(r, x);
                }
            }
            return r;
        }

        final void leafTransform(int l, int h, Op op) {
            final Object[] a = this.array;
            final Predicate s = selector;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(x))
                    a[i] = op.op(x);
            }
        }

        final void leafIndexMap(int l, int h, IntToObject op) {
            final Object[] a = this.array;
            final Predicate s = selector;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(x))
                    a[i] = op.op(i);
            }
        }

        final void leafBinaryIndexMap(int l, int h, IntAndObjectToObject op) {
            final Object[] a = this.array;
            final Predicate s = selector;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(x))
                    a[i] = op.op(i, x);
            }
        }

        final void leafGenerate(int l, int h, Generator generator) {
            final Object[] a = this.array;
            final Predicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(a[i]))
                    a[i] = generator.op();
            }
        }

        final void leafFill(int l, int h, Object value) {
            final Object[] a = this.array;
            final Predicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(a[i]))
                    a[i] = value;
            }
        }

        final void leafCombineInPlace
            (int l, int h, Object[] other,
             int otherOffset, BinaryOp combiner) {
            final Object[] a = this.array;
            final Predicate s = selector;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(x))
                    a[i] = combiner.op(x, other[k]);
                k++;
            }
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelArrayWithMapping other,
             int otherOffset, BinaryOp combiner) {
            final Object[] a = this.array;
            final Predicate s = selector;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    Object x = a[i];
                    if (s.op(x) && other.isSelected(k))
                        a[i] = combiner.op(x, other.oget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i) {
                    Object x = a[i];
                    if (s.op(x))
                        a[i] = combiner.op(x, other.oget(k));
                    k++;
                }
            }
            else {
                Object[] b = other.array;
                for (int i = l; i < h; ++i) {
                    Object x = a[i];
                    if (s.op(x))
                        a[i] = combiner.op(x, b[k]);
                    k++;
                }
            }
        }
    }

    static final class DFPap extends ParallelDoubleArrayWithFilter {
        final DoublePredicate selector;
        DFPap(ForkJoinPool ex, int origin, int fence,
              double[] array,
              DoublePredicate selector) {
            super(ex, origin, fence, array);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelDoubleArrayWithFilter withFilter(DoublePredicate selector) {
            return new DFPap(ex, origin, fence, array,
                             CommonOps.andPredicate(this.selector, selector));
        }

        public ParallelDoubleArrayWithFilter withIndexedFilter
            (IntAndDoublePredicate selector) {
            return new DRPap
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DFOMPap<U>(ex, origin, fence, array, selector, op);
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DFDMPap(ex, origin, fence, array, selector, op);
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DFLMPap(ex, origin, fence, array, selector, op);
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DFOCPap<V>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DFDCPap(ex, origin, fence, array, selector, mapper);
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DFLCPap(ex, origin, fence, array, selector, mapper);
        }

        final void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final DoublePredicate s = selector;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x))
                    procedure.op(x);
            }
        }

        final double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final DoublePredicate s = selector;
            boolean gotFirst = false;
            double r = base;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x)) {
                    if (!gotFirst) {
                        gotFirst = true;
                        r = x;
                    }
                    else
                        r = reducer.op(r, x);
                }
            }
            return r;
        }

        final void leafTransform(int l, int h, DoubleOp op) {
            final double[] a = this.array;
            final DoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(x))
                    a[i] = op.op(x);
            }
        }

        final void leafIndexMap(int l, int h, IntToDouble op) {
            final double[] a = this.array;
            final DoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(x))
                    a[i] = op.op(i);
            }
        }

        final void leafBinaryIndexMap(int l, int h, IntAndDoubleToDouble op) {
            final double[] a = this.array;
            final DoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(x))
                    a[i] = op.op(i, x);
            }
        }

        final void leafGenerate(int l, int h, DoubleGenerator generator) {
            final double[] a = this.array;
            final DoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(a[i]))
                    a[i] = generator.op();
            }
        }

        final void leafFill(int l, int h, double value) {
            final double[] a = this.array;
            final DoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(a[i]))
                    a[i] = value;
            }
        }

        final void leafCombineInPlace
            (int l, int h, double[] other,
             int otherOffset, BinaryDoubleOp combiner) {
            final double[] a = this.array;
            final DoublePredicate s = selector;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(x))
                    a[i] = combiner.op(x, other[k]);
                k++;
            }
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelDoubleArrayWithDoubleMapping other,
             int otherOffset, BinaryDoubleOp combiner) {
            final double[] a = this.array;
            final DoublePredicate s = selector;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    double x = a[i];
                    if (s.op(x) && other.isSelected(k))
                        a[i] = combiner.op(x, other.dget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i) {
                    double x = a[i];
                    if (s.op(x))
                        a[i] = combiner.op(x, other.dget(k));
                    k++;
                }
            }
            else {
                double[] b = other.array;
                for (int i = l; i < h; ++i) {
                    double x = a[i];
                    if (s.op(x))
                        a[i] = combiner.op(x, b[k]);
                    k++;
                }
            }
        }
    }

    static final class LFPap extends ParallelLongArrayWithFilter {
        final LongPredicate selector;
        LFPap(ForkJoinPool ex, int origin, int fence,
              long[] array,
              LongPredicate selector) {
            super(ex, origin, fence, array);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelLongArrayWithFilter withFilter(LongPredicate selector) {
            return new LFPap(ex, origin, fence, array,
                             CommonOps.andPredicate(this.selector, selector));
        }

        public ParallelLongArrayWithFilter withIndexedFilter
            (IntAndLongPredicate selector) {
            return new LRPap
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new LFOMPap<U>(ex, origin, fence, array, selector, op);
        }

        public ParallelLongArrayWithLongMapping withMapping
            (LongOp op) {
            return new LFLMPap(ex, origin, fence, array, selector, op);
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new LFDMPap(ex, origin, fence, array, selector, op);
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LFOCPap<V>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LFDCPap(ex, origin, fence, array, selector, mapper);
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LFLCPap(ex, origin, fence, array, selector, mapper);
        }

        final void leafApply(int lo, int hi, LongProcedure procedure) {
            final LongPredicate s = selector;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x))
                    procedure.op(x);
            }
        }

        final long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final LongPredicate s = selector;
            boolean gotFirst = false;
            long r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x)) {
                    if (!gotFirst) {
                        gotFirst = true;
                        r = x;
                    }
                    else
                        r = reducer.op(r, x);
                }
            }
            return r;
        }

        final void leafTransform(int l, int h, LongOp op) {
            final long[] a = this.array;
            final LongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(x))
                    a[i] = op.op(x);
            }
        }

        final void leafIndexMap(int l, int h, IntToLong op) {
            final long[] a = this.array;
            final LongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(x))
                    a[i] = op.op(i);
            }
        }

        final void leafBinaryIndexMap(int l, int h, IntAndLongToLong op) {
            final long[] a = this.array;
            final LongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(x))
                    a[i] = op.op(i, x);
            }
        }

        final void leafGenerate(int l, int h, LongGenerator generator) {
            final long[] a = this.array;
            final LongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(a[i]))
                    a[i] = generator.op();
            }
        }

        final void leafFill(int l, int h, long value) {
            final long[] a = this.array;
            final LongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(a[i]))
                    a[i] = value;
            }
        }

        final void leafCombineInPlace
            (int l, int h, long[] other,
             int otherOffset, BinaryLongOp combiner) {
            final long[] a = this.array;
            final LongPredicate s = selector;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(x))
                    a[i] = combiner.op(x, other[k]);
                k++;
            }
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelLongArrayWithLongMapping other,
             int otherOffset, BinaryLongOp combiner) {
            final long[] a = this.array;
            final LongPredicate s = selector;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    long x = a[i];
                    if (s.op(x) && other.isSelected(k))
                        a[i] = combiner.op(x, other.lget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i) {
                    long x = a[i];
                    if (s.op(x))
                        a[i] = combiner.op(x, other.lget(k));
                    k++;
                }
            }
            else {
                long[] b = other.array;
                for (int i = l; i < h; ++i) {
                    long x = a[i];
                    if (s.op(x))
                        a[i] = combiner.op(x, b[k]);
                    k++;
                }
            }
        }
    }

    // Relationally Filtered (but unmapped) classes
    static final class ORPap<T> extends ParallelArrayWithFilter<T> {
        final IntAndObjectPredicate<? super T> selector;
        ORPap(ForkJoinPool ex, int origin, int fence,
              T[] array,
              IntAndObjectPredicate<? super T> selector) {
            super(ex, origin, fence, array);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelArrayWithFilter<T> withFilter
            (Predicate<? super T> selector) {
            return new ORPap<T>
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public ParallelArrayWithFilter<T> withIndexedFilter
            (IntAndObjectPredicate<? super T> selector) {
            return new ORPap<T>
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (Op<? super T, ? extends U> op) {
            return new OROMPap<T,U>(ex, origin, fence, array, selector, op);
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super T> op) {
            return new ORDMPap<T>(ex, origin, fence, array, selector, op);
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super T> op) {
            return new ORLMPap<T>(ex, origin, fence, array, selector, op);
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super T, ? extends V> mapper) {
            return new OROCPap<T,V>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super T> mapper) {
            return new ORDCPap<T>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super T> mapper) {
            return new ORLCPap<T>(ex, origin, fence, array, selector, mapper);
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    procedure.op(x);
            }
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final IntAndObjectPredicate s = selector;
            boolean gotFirst = false;
            Object r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x)) {
                    if (!gotFirst) {
                        gotFirst = true;
                        r = x;
                    }
                    else
                        r = reducer.op(r, x);
                }
            }
            return r;
        }

        final void leafTransform(int l, int h, Op op) {
            final Object[] a = this.array;
            final IntAndObjectPredicate s = selector;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(x);
            }
        }

        final void leafIndexMap(int l, int h, IntToObject op) {
            final Object[] a = this.array;
            final IntAndObjectPredicate s = selector;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(i);
            }
        }

        final void leafBinaryIndexMap(int l, int h, IntAndObjectToObject op) {
            final Object[] a = this.array;
            final IntAndObjectPredicate s = selector;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(i, x);
            }
        }

        final void leafGenerate(int l, int h, Generator generator) {
            final Object[] a = this.array;
            final IntAndObjectPredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(i, a[i]))
                    a[i] = generator.op();
            }
        }

        final void leafFill(int l, int h, Object value) {
            final Object[] a = this.array;
            final IntAndObjectPredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(i, a[i]))
                    a[i] = value;
            }
        }

        final void leafCombineInPlace
            (int l, int h, Object[] other,
             int otherOffset, BinaryOp combiner) {
            final Object[] a = this.array;
            final IntAndObjectPredicate s = selector;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    a[i] = combiner.op(x, other[k]);
                k++;
            }
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelArrayWithMapping other,
             int otherOffset, BinaryOp combiner) {
            final Object[] a = this.array;
            final IntAndObjectPredicate s = selector;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    Object x = a[i];
                    if (s.op(i, x) && other.isSelected(k))
                        a[i] = combiner.op(x, other.oget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i) {
                    Object x = a[i];
                    if (s.op(i, x))
                        a[i] = combiner.op(x, other.oget(k));
                    k++;
                }
            }
            else {
                Object[] b = other.array;
                for (int i = l; i < h; ++i) {
                    Object x = a[i];
                    if (s.op(i, x))
                        a[i] = combiner.op(x, b[k]);
                    k++;
                }
            }
        }
    }

    static final class DRPap extends ParallelDoubleArrayWithFilter {
        final IntAndDoublePredicate selector;
        DRPap(ForkJoinPool ex, int origin, int fence,
              double[] array,
              IntAndDoublePredicate selector) {
            super(ex, origin, fence, array);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelDoubleArrayWithFilter withFilter
            (DoublePredicate selector) {
            return new DRPap
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public ParallelDoubleArrayWithFilter withIndexedFilter
            (IntAndDoublePredicate selector) {
            return new DRPap
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DROMPap<U>(ex, origin, fence, array, selector, op);
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DRDMPap(ex, origin, fence, array, selector, op);
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DRLMPap(ex, origin, fence, array, selector, op);
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DROCPap<V>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DRDCPap(ex, origin, fence, array, selector, mapper);
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DRLCPap(ex, origin, fence, array, selector, mapper);
        }

        final void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndDoublePredicate s = selector;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    procedure.op(x);
            }
        }

        final double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final IntAndDoublePredicate s = selector;
            boolean gotFirst = false;
            double r = base;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x)) {
                    if (!gotFirst) {
                        gotFirst = true;
                        r = x;
                    }
                    else
                        r = reducer.op(r, x);
                }
            }
            return r;
        }

        final void leafTransform(int l, int h, DoubleOp op) {
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(x);
            }
        }

        final void leafIndexMap(int l, int h, IntToDouble op) {
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(i);
            }
        }

        final void leafBinaryIndexMap(int l, int h, IntAndDoubleToDouble op) {
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(i, x);
            }
        }

        final void leafGenerate(int l, int h, DoubleGenerator generator) {
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(i, a[i]))
                    a[i] = generator.op();
            }
        }

        final void leafFill(int l, int h, double value) {
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(i, a[i]))
                    a[i] = value;
            }
        }

        final void leafCombineInPlace
            (int l, int h, double[] other,
             int otherOffset, BinaryDoubleOp combiner) {
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    a[i] = combiner.op(x, other[k]);
                k++;
            }
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelDoubleArrayWithDoubleMapping other,
             int otherOffset, BinaryDoubleOp combiner) {
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    double x = a[i];
                    if (s.op(i, x) && other.isSelected(k))
                        a[i] = combiner.op(x, other.dget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i) {
                    double x = a[i];
                    if (s.op(i, x))
                        a[i] = combiner.op(x, other.dget(k));
                    k++;
                }
            }
            else {
                double[] b = other.array;
                for (int i = l; i < h; ++i) {
                    double x = a[i];
                    if (s.op(i, x))
                        a[i] = combiner.op(x, b[k]);
                    k++;
                }
            }
        }
    }

    static final class LRPap extends ParallelLongArrayWithFilter {
        final IntAndLongPredicate selector;
        LRPap(ForkJoinPool ex, int origin, int fence,
              long[] array,
              IntAndLongPredicate selector) {
            super(ex, origin, fence, array);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelLongArrayWithFilter withFilter(LongPredicate selector) {
            return new LRPap(ex, origin, fence, array,
                             compoundIndexedSelector(this.selector, selector));
        }

        public ParallelLongArrayWithFilter withIndexedFilter
            (IntAndLongPredicate selector) {
            return new LRPap
                (ex, origin, fence, array,
                 compoundIndexedSelector(this.selector, selector));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new LROMPap<U>(ex, origin, fence, array, selector, op);
        }

        public ParallelLongArrayWithLongMapping withMapping
            (LongOp op) {
            return new LRLMPap(ex, origin, fence, array, selector, op);
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new LRDMPap(ex, origin, fence, array, selector, op);
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LROCPap<V>(ex, origin, fence, array, selector, mapper);
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LRDCPap(ex, origin, fence, array, selector, mapper);
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LRLCPap(ex, origin, fence, array, selector, mapper);
        }

        final void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndLongPredicate s = selector;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    procedure.op(x);
            }
        }

        final long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final IntAndLongPredicate s = selector;
            boolean gotFirst = false;
            long r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x)) {
                    if (!gotFirst) {
                        gotFirst = true;
                        r = x;
                    }
                    else
                        r = reducer.op(r, x);
                }
            }
            return r;
        }

        final void leafTransform(int l, int h, LongOp op) {
            final long[] a = this.array;
            final IntAndLongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(x);
            }
        }

        final void leafIndexMap(int l, int h, IntToLong op) {
            final long[] a = this.array;
            final IntAndLongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(i);
            }
        }

        final void leafBinaryIndexMap(int l, int h, IntAndLongToLong op) {
            final long[] a = this.array;
            final IntAndLongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    a[i] = op.op(i, x);
            }
        }

        final void leafGenerate(int l, int h, LongGenerator generator) {
            final long[] a = this.array;
            final IntAndLongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(i, a[i]))
                    a[i] = generator.op();
            }
        }

        final void leafFill(int l, int h, long value) {
            final long[] a = this.array;
            final IntAndLongPredicate s = selector;
            for (int i = l; i < h; ++i) {
                if (s.op(i, a[i]))
                    a[i] = value;
            }
        }

        final void leafCombineInPlace
            (int l, int h, long[] other,
             int otherOffset, BinaryLongOp combiner) {
            final long[] a = this.array;
            final IntAndLongPredicate s = selector;
            int k = l + otherOffset;
            for (int i = l; i < h; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    a[i] = combiner.op(x, other[k]);
                k++;
            }
        }

        final void leafCombineInPlace
            (int l, int h,
             ParallelLongArrayWithLongMapping other,
             int otherOffset, BinaryLongOp combiner) {
            final long[] a = this.array;
            final IntAndLongPredicate s = selector;
            int k = l + otherOffset;
            if (other.hasFilter()) {
                for (int i = l; i < h; ++i) {
                    long x = a[i];
                    if (s.op(i, x) && other.isSelected(k))
                        a[i] = combiner.op(x, other.lget(k));
                    k++;
                }
            }
            else if (other.hasMap()) {
                for (int i = l; i < h; ++i) {
                    long x = a[i];
                    if (s.op(i, x))
                        a[i] = combiner.op(x, other.lget(k));
                    k++;
                }
            }
            else {
                long[] b = other.array;
                for (int i = l; i < h; ++i) {
                    long x = a[i];
                    if (s.op(i, x))
                        a[i] = combiner.op(x, b[k]);
                    k++;
                }
            }
        }
    }

    // Object-mapped

    abstract static class OOMPap<T,U> extends ParallelArrayWithMapping<T,U> {
        final Op<? super T, ? extends U> op;
        OOMPap(ForkJoinPool ex, int origin, int fence,
               T[] array,
               Op<? super T, ? extends U> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final Object oget(int i) { return op.op(this.array[i]); }
        final double dget(int i) { return ((Number)oget(i)).doubleValue(); }
        final long lget(int i) { return ((Number)oget(i)).longValue(); }

        final void leafTransfer(int lo, int hi, Object[] dest, int offset) {
            final Op f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       Object[] dest, int offset) {
            final Object[] a = this.array;
            final Op f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }
    }

    abstract static class DOMPap<U> extends ParallelDoubleArrayWithMapping<U> {
        final DoubleToObject<? extends U> op;
        DOMPap(ForkJoinPool ex, int origin, int fence,
               double[] array, DoubleToObject<? extends U> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final Object oget(int i) { return op.op(this.array[i]); }
        final double dget(int i) { return ((Number)oget(i)).doubleValue(); }
        final long lget(int i) { return ((Number)oget(i)).longValue(); }

        final void leafTransfer(int lo, int hi, Object[] dest, int offset) {
            final double[] a = this.array;
            final DoubleToObject f = op;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       Object[] dest, int offset) {
            final double[] a = this.array;
            final DoubleToObject f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }
    }

    abstract static class LOMPap<U> extends ParallelLongArrayWithMapping<U> {
        final LongToObject<? extends U> op;
        LOMPap(ForkJoinPool ex, int origin, int fence,
               long[] array, LongToObject<? extends U> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final Object oget(int i) { return op.op(this.array[i]); }
        final double dget(int i) { return ((Number)oget(i)).doubleValue(); }
        final long lget(int i) { return ((Number)oget(i)).longValue(); }

        final void leafTransfer(int lo, int hi, Object[] dest, int offset) {
            final long[] a = this.array;
            final LongToObject f = op;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       Object[] dest, int offset) {
            final long[] a = this.array;
            final LongToObject f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }
    }

    // Object mapped, unfiltered

    static final class OUOMPap<T,U> extends OOMPap<T,U> {
        OUOMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, Op<? super T, ? extends U> op) {
            super(ex, origin, fence, array, op);
        }

        public <V> ParallelArrayWithMapping<T,V> withMapping
            (Op<? super U, ? extends V> op) {
            return new OUOMPap<T,V>(ex, origin, fence, array,
                                    CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super U> op) {
            return new OUDMPap<T>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super U> op) {
            return new OULMPap<T>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new OUOCPap<T,V>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final Op f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            if (lo >= hi)
                return base;
            final Object[] a = this.array;
            final Op f = op;
            Object r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }

    }

    static final class DUOMPap<U> extends DOMPap<U> {
        DUOMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoubleToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
        }

        public <V> ParallelDoubleArrayWithMapping<V> withMapping
            (Op<? super U, ? extends V> op) {
            return new DUOMPap<V>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new DUDMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new DULMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new DUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final double[] a = this.array;
            final DoubleToObject f = op;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            if (lo >= hi)
                return base;
            final double[] a = this.array;
            final DoubleToObject f = op;
            Object r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }

    }

    static final class LUOMPap<U> extends LOMPap<U> {
        LUOMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
        }

        public <V> ParallelLongArrayWithMapping<V> withMapping
            (Op<? super U, ? extends V> op) {
            return new LUOMPap<V>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new LULMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new LUDMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new LUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final long[] a = this.array;
            final LongToObject f = op;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            if (lo >= hi)
                return base;
            final long[] a = this.array;
            final LongToObject f = op;
            Object r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }

    }

    // Object-mapped, filtered
    static final class OFOMPap<T,U> extends OOMPap<T,U> {
        final Predicate<? super T> selector;

        OFOMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, Predicate<? super T> selector,
                Op<? super T, ? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public <V> ParallelArrayWithMapping<T,V> withMapping
            (Op<? super U, ? extends V> op) {
            return new OFOMPap<T,V>
                (ex, origin, fence, array, selector,
                 CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super U> op) {
            return new OFDMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super U> op) {
            return new OFLMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new OFOCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final Op f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final Op f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x)) {
                    Object y = f.op(x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }

    }

    static final class DFOMPap<U> extends DOMPap<U> {
        final DoublePredicate selector;
        DFOMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoublePredicate selector,
                DoubleToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelArray<U> all(Class<? super U> elementType) {
            PAS.FJOSelectAllDriver r = new PAS.FJOSelectAllDriver
                (this, elementType);
            ex.invoke(r);
            return new ParallelArray<U>(ex, (U[])(r.results));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withMapping
            (Op<? super U, ? extends V> op) {
            return new DFOMPap<V>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new DFDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new DFLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new DFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final DoublePredicate s = selector;
            final DoubleToObject f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            boolean gotFirst = false;
            Object r = base;
            final DoublePredicate s = selector;
            final DoubleToObject f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x)) {
                    Object y = f.op(x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }

    }

    static final class LFOMPap<U> extends LOMPap<U> {
        final LongPredicate selector;
        LFOMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongPredicate selector,
                LongToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public <V> ParallelLongArrayWithMapping<V> withMapping
            (Op<? super U, ? extends V> op) {
            return new LFOMPap<V>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new LFLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new LFDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new LFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final LongPredicate s = selector;
            final LongToObject f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final LongPredicate s = selector;
            final LongToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x)) {
                    Object y = f.op(x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }

    }

    // Object-mapped, relational
    static final class OROMPap<T,U> extends OOMPap<T,U> {
        final IntAndObjectPredicate<? super T> selector;

        OROMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectPredicate<? super T> selector,
                Op<? super T, ? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public <V> ParallelArrayWithMapping<T,V> withMapping
            (Op<? super U, ? extends V> op) {
            return new OROMPap<T,V>
                (ex, origin, fence, array, selector,
                 CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithDoubleMapping<T> withMapping(ObjectToDouble<? super U> op) {
            return new ORDMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(ObjectToLong<? super U> op) {
            return new ORLMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new OROCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final Op f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final Op f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x)) {
                    Object y = f.op(x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }

    }

    static final class DROMPap<U> extends DOMPap<U> {
        final IntAndDoublePredicate selector;
        DROMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoublePredicate selector,
                DoubleToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelArray<U> all(Class<? super U> elementType) {
            PAS.FJOSelectAllDriver r = new PAS.FJOSelectAllDriver
                (this, elementType);
            ex.invoke(r);
            return new ParallelArray<U>(ex, (U[])(r.results));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withMapping
            (Op<? super U, ? extends V> op) {
            return new DROMPap<V>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new DRDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new DRLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new DROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndDoublePredicate s = selector;
            final DoubleToObject f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            boolean gotFirst = false;
            Object r = base;
            final IntAndDoublePredicate s = selector;
            final DoubleToObject f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x)) {
                    Object y = f.op(x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }

    }

    static final class LROMPap<U> extends LOMPap<U> {
        final IntAndLongPredicate selector;
        LROMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongPredicate selector,
                LongToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public <V> ParallelLongArrayWithMapping<V> withMapping
            (Op<? super U, ? extends V> op) {
            return new LROMPap<V>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new LRLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new LRDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new LROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndLongPredicate s = selector;
            final LongToObject f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final IntAndLongPredicate s = selector;
            final LongToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x)) {
                    Object y = f.op(x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // Object-combined

    abstract static class OOCPap<T,U> extends ParallelArrayWithMapping<T,U> {
        final IntAndObjectToObject<? super T, ? extends U> op;
        OOCPap(ForkJoinPool ex, int origin, int fence,
               T[] array,
               IntAndObjectToObject<? super T, ? extends U> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final Object oget(int i) { return op.op(i, this.array[i]); }
        final double dget(int i) { return ((Number)oget(i)).doubleValue(); }
        final long lget(int i) { return ((Number)oget(i)).longValue(); }

        final void leafTransfer(int lo, int hi, Object[] dest, int offset) {
            final IntAndObjectToObject f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       Object[] dest, int offset) {
            final Object[] a = this.array;
            final IntAndObjectToObject f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    abstract static class DOCPap<U> extends ParallelDoubleArrayWithMapping<U> {
        final IntAndDoubleToObject<? extends U> op;
        DOCPap(ForkJoinPool ex, int origin, int fence,
               double[] array, IntAndDoubleToObject<? extends U> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final Object oget(int i) { return op.op(i, this.array[i]); }
        final double dget(int i) { return ((Number)oget(i)).doubleValue(); }
        final long lget(int i) { return ((Number)oget(i)).longValue(); }

        final void leafTransfer(int lo, int hi, Object[] dest, int offset) {
            final IntAndDoubleToObject f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       Object[] dest, int offset) {
            final double[] a = this.array;
            final IntAndDoubleToObject f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    abstract static class LOCPap<U> extends ParallelLongArrayWithMapping<U> {
        final IntAndLongToObject<? extends U> op;
        LOCPap(ForkJoinPool ex, int origin, int fence,
               long[] array, IntAndLongToObject<? extends U> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final Object oget(int i) { return op.op(i, this.array[i]); }
        final double dget(int i) { return ((Number)oget(i)).doubleValue(); }
        final long lget(int i) { return ((Number)oget(i)).longValue(); }

        final void leafTransfer(int lo, int hi, Object[] dest, int offset) {
            final IntAndLongToObject f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       Object[] dest, int offset) {
            final long[] a = this.array;
            final IntAndLongToObject f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    // Object-combined, unfiltered

    static final class OUOCPap<T,U> extends OOCPap<T,U> {
        OUOCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectToObject<? super T, ? extends U> op) {
            super(ex, origin, fence, array, op);
        }

        public <V> ParallelArrayWithMapping<T,V> withMapping
            (Op<? super U, ? extends V> op) {
            return new OUOCPap<T,V>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super U> op) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super U> op) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new OUOCPap<T,V>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndObjectToObject f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            if (lo >= hi)
                return base;
            final Object[] a = this.array;
            final IntAndObjectToObject f = op;
            Object r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }

    }

    static final class DUOCPap<U> extends DOCPap<U> {
        DUOCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoubleToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
        }

        public <V> ParallelDoubleArrayWithMapping< V> withMapping
            (Op<? super U, ? extends V> op) {
            return new DUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new DUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndDoubleToObject f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            if (lo >= hi)
                return base;
            final double[] a = this.array;
            final IntAndDoubleToObject f = op;
            Object r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }

    }

    static final class LUOCPap<U> extends LOCPap<U> {
        LUOCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
        }

        public <V> ParallelLongArrayWithMapping< V> withMapping
            (Op<? super U, ? extends V> op) {
            return new LUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new LUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndLongToObject f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            if (lo >= hi)
                return base;
            final long[] a = this.array;
            final IntAndLongToObject f = op;
            Object r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }

    }

    // object-combined filtered

    static final class OFOCPap<T,U> extends OOCPap<T,U> {
        final Predicate<? super T> selector;
        OFOCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, Predicate<? super T> selector,
                IntAndObjectToObject<? super T, ? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public <V> ParallelArrayWithMapping<T,V> withMapping
            (Op<? super U, ? extends V> op) {
            return new OFOCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super U> op) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super U> op) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new OFOCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new OFLCPap<T>
                (ex, origin, fence, array, selector,
                 compoundIndexedOp
                 (this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToObject f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x)) {
                    Object y = f.op(i, x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DFOCPap<U> extends DOCPap<U> {
        final DoublePredicate selector;
        DFOCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoublePredicate selector,
                IntAndDoubleToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public <V> ParallelDoubleArrayWithMapping< V> withMapping
            (Op<? super U, ? extends V> op) {
            return new DFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new DFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final DoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToObject f = op;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final DoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x)) {
                    Object y = f.op(i, x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LFOCPap<U> extends LOCPap<U> {
        final LongPredicate selector;
        LFOCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongPredicate selector,
                IntAndLongToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public <V> ParallelLongArrayWithMapping< V> withMapping
            (Op<? super U, ? extends V> op) {
            return new LFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new LFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final LongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToObject f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final LongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x)) {
                    Object y = f.op(i, x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // Object-combined, relational
    static final class OROCPap<T,U> extends OOCPap<T,U> {
        final IntAndObjectPredicate<? super T> selector;
        OROCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectPredicate<? super T> selector,
                IntAndObjectToObject<? super T, ? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public <V> ParallelArrayWithMapping<T,V> withMapping
            (Op<? super U, ? extends V> op) {
            return new OROCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (ObjectToDouble<? super U> op) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (ObjectToLong<? super U> op) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new OROCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new ORLCPap<T>
                (ex, origin, fence, array, selector,
                 compoundIndexedOp
                 (this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToObject f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x)) {
                    Object y = f.op(i, x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DROCPap<U> extends DOCPap<U> {
        final IntAndDoublePredicate selector;
        DROCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoublePredicate selector,
                IntAndDoubleToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public <V> ParallelDoubleArrayWithMapping< V> withMapping
            (Op<? super U, ? extends V> op) {
            return new DROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new DROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndDoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToObject f = op;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final IntAndDoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x)) {
                    Object y = f.op(i, x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LROCPap<U> extends LOCPap<U> {
        final IntAndLongPredicate selector;
        LROCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongPredicate selector,
                IntAndLongToObject<? extends U> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public <V> ParallelLongArrayWithMapping< V> withMapping
            (Op<? super U, ? extends V> op) {
            return new LROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping
            (ObjectToDouble<? super U> op) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping
            (ObjectToLong<? super U> op) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndObjectToObject<? super U, ? extends V> mapper) {
            return new LROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndObjectToDouble<? super U> mapper) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndObjectToLong<? super U> mapper) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, Procedure procedure) {
            final IntAndLongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToObject f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }
        Object leafReduce(int lo, int hi, Reducer reducer, Object base) {
            final IntAndLongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToObject f = op;
            boolean gotFirst = false;
            Object r = base;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x)) {
                    Object y = f.op(i, x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // Double-mapped

    abstract static class ODMPap<T> extends ParallelArrayWithDoubleMapping<T> {
        final ObjectToDouble<? super T> op;
        ODMPap(ForkJoinPool ex, int origin, int fence,
               T[] array, ObjectToDouble<? super T> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final double dget(int i) { return op.op(this.array[i]); }
        final Object oget(int i) { return Double.valueOf(dget(i)); }
        final long lget(int i) { return (long)(dget(i)); }

        final void leafTransfer(int lo, int hi, double[] dest, int offset) {
            final ObjectToDouble f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       double[] dest, int offset) {
            final Object[] a = this.array;
            final ObjectToDouble f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }

    }

    abstract static class DDMPap extends ParallelDoubleArrayWithDoubleMapping {
        final DoubleOp op;
        DDMPap
            (ForkJoinPool ex, int origin, int fence,
             double[] array, DoubleOp op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final double dget(int i) { return op.op(this.array[i]); }
        final Object oget(int i) { return Double.valueOf(dget(i)); }
        final long lget(int i) { return (long)(dget(i)); }

        final void leafTransfer(int lo, int hi, double[] dest, int offset) {
            final double[] a = this.array;
            final DoubleOp f = op;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       double[] dest, int offset) {
            final double[] a = this.array;
            final DoubleOp f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }
    }

    abstract static class LDMPap extends ParallelLongArrayWithDoubleMapping {
        final LongToDouble op;
        LDMPap(ForkJoinPool ex, int origin, int fence,
               long[] array, LongToDouble op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final double dget(int i) { return op.op(this.array[i]); }
        final Object oget(int i) { return Double.valueOf(dget(i)); }
        final long lget(int i) { return (long)(dget(i)); }

        final void leafTransfer(int lo, int hi, double[] dest, int offset) {
            final long[] a = this.array;
            final LongToDouble f = op;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       double[] dest, int offset) {
            final long[] a = this.array;
            final LongToDouble f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }

    }

    // double-mapped, unfiltered

    static final class OUDMPap<T> extends ODMPap<T> {
        OUDMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, ObjectToDouble<? super T> op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelArrayWithDoubleMapping<T> withMapping(DoubleOp op) {
            return new OUDMPap<T>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(DoubleToLong op) {
            return new OULMPap<T>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (DoubleToObject<? extends U> op) {
            return new OUOMPap<T,U>(ex, origin, fence, array,
                                    CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new OUOCPap<T,V>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final ObjectToDouble f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            if (lo >= hi)
                return base;
            final Object[] a = this.array;
            final ObjectToDouble f = op;
            double r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }

    }

    static final class DUDMPap extends DDMPap {
        DUDMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoubleOp op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DUDMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DULMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DUOMPap<U>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final double[] a = this.array;
            final DoubleOp f = op;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            if (lo >= hi)
                return base;
            final double[] a = this.array;
            final DoubleOp f = op;
            double r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }

    }

    static final class LUDMPap extends LDMPap {
        LUDMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongToDouble op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelLongArrayWithLongMapping withMapping(DoubleToLong op) {
            return new LULMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new LUDMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new LUOMPap<U>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new LUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final LongToDouble f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            if (lo >= hi)
                return base;
            final long[] a = this.array;
            final LongToDouble f = op;
            double r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }

    }

    // double-mapped, filtered

    static final class OFDMPap<T> extends ODMPap<T> {
        final Predicate<? super T> selector;
        OFDMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, Predicate<? super T> selector,
                ObjectToDouble<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping(DoubleOp op) {
            return new OFDMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(DoubleToLong op) {
            return new OFLMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (DoubleToObject<? extends U> op) {
            return new OFOMPap<T,U>(ex, origin, fence, array, selector,
                                    CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new OFOCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final ObjectToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final Predicate s = selector;
            final ObjectToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(t)) {
                    double y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DFDMPap extends DDMPap {
        final DoublePredicate selector;
        DFDMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoublePredicate selector, DoubleOp op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DFDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DFLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DFOMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final DoublePredicate s = selector;
            final DoubleOp f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final DoublePredicate s = selector;
            boolean gotFirst = false;
            double r = base;
            final double[] a = this.array;
            final DoubleOp f = op;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(t)) {
                    double y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LFDMPap extends LDMPap {
        final LongPredicate selector;
        LFDMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongPredicate selector, LongToDouble op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelLongArrayWithLongMapping withMapping(DoubleToLong op) {
            return new LFLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new LFDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new LFOMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new LFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final LongPredicate s = selector;
            final long[] a = this.array;
            final LongToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final LongPredicate s = selector;
            final LongToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(t)) {
                    double y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // double-mapped, relational
    static final class ORDMPap<T> extends ODMPap<T> {
        final IntAndObjectPredicate<? super T> selector;
        ORDMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectPredicate<? super T> selector,
                ObjectToDouble<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping(DoubleOp op) {
            return new ORDMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(DoubleToLong op) {
            return new ORLMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (DoubleToObject<? extends U> op) {
            return new OROMPap<T,U>(ex, origin, fence, array, selector,
                                    CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new OROCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final ObjectToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final IntAndObjectPredicate s = selector;
            final ObjectToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(i, t)) {
                    double y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DRDMPap extends DDMPap {
        final IntAndDoublePredicate selector;
        DRDMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoublePredicate selector, DoubleOp op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DRDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DRLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DROMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndDoublePredicate s = selector;
            final DoubleOp f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final IntAndDoublePredicate s = selector;
            boolean gotFirst = false;
            double r = base;
            final double[] a = this.array;
            final DoubleOp f = op;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(i, t)) {
                    double y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LRDMPap extends LDMPap {
        final IntAndLongPredicate selector;
        LRDMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongPredicate selector, LongToDouble op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelLongArrayWithLongMapping withMapping(DoubleToLong op) {
            return new LRLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new LRDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (DoubleToObject<? extends U> op) {
            return new LROMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new LROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndLongPredicate s = selector;
            final long[] a = this.array;
            final LongToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final IntAndLongPredicate s = selector;
            final LongToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(i, t)) {
                    double y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // double-combined
    abstract static class ODCPap<T> extends ParallelArrayWithDoubleMapping<T> {
        final IntAndObjectToDouble<? super T> op;
        ODCPap(ForkJoinPool ex, int origin, int fence,
               T[] array, IntAndObjectToDouble<? super T> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final double dget(int i) { return op.op(i, this.array[i]); }
        final Object oget(int i) { return Double.valueOf(dget(i)); }
        final long lget(int i) { return (long)(dget(i)); }

        final void leafTransfer(int lo, int hi, double[] dest, int offset) {
            final IntAndObjectToDouble f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       double[] dest, int offset) {
            final Object[] a = this.array;
            final IntAndObjectToDouble f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }

    }

    abstract static class DDCPap extends ParallelDoubleArrayWithDoubleMapping {
        final IntAndDoubleToDouble op;
        DDCPap(ForkJoinPool ex, int origin, int fence,
               double[] array, IntAndDoubleToDouble op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final double dget(int i) { return op.op(i, this.array[i]); }
        final Object oget(int i) { return Double.valueOf(dget(i)); }
        final long lget(int i) { return (long)(dget(i)); }

        final void leafTransfer(int lo, int hi, double[] dest, int offset) {
            final IntAndDoubleToDouble f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       double[] dest, int offset) {
            final double[] a = this.array;
            final IntAndDoubleToDouble f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    abstract static class LDCPap extends ParallelLongArrayWithDoubleMapping {
        final IntAndLongToDouble op;
        LDCPap(ForkJoinPool ex, int origin, int fence,
               long[] array, IntAndLongToDouble op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final double dget(int i) { return op.op(i, this.array[i]); }
        final Object oget(int i) { return Double.valueOf(dget(i)); }
        final long lget(int i) { return (long)(dget(i)); }

        final void leafTransfer(int lo, int hi, double[] dest, int offset) {
            final IntAndLongToDouble f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       double[] dest, int offset) {
            final long[] a = this.array;
            final IntAndLongToDouble f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    // double-combined, unfiltered
    static final class OUDCPap<T> extends ODCPap<T> {
        OUDCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectToDouble<? super T> op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelArrayWithDoubleMapping<T> withMapping(DoubleOp op) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(DoubleToLong op) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (DoubleToObject<? extends U> op) {
            return new OUOCPap<T,U>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new OUOCPap<T,V>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndObjectToDouble f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            if (lo >= hi)
                return base;
            final Object[] a = this.array;
            final IntAndObjectToDouble f = op;
            double r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }

    }

    static final class DUDCPap extends DDCPap {
        DUDCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoubleToDouble op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping< U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DUOCPap<U>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndDoubleToDouble f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            if (lo >= hi)
                return base;
            final double[] a = this.array;
            final IntAndDoubleToDouble f = op;
            double r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }
    }

    static final class LUDCPap extends LDCPap {
        LUDCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongToDouble op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelLongArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping(DoubleToLong op) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping< U> withMapping
            (DoubleToObject<? extends U> op) {
            return new LUOCPap<U>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new LUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndLongToDouble f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            if (lo >= hi)
                return base;
            final long[] a = this.array;
            final IntAndLongToDouble f = op;
            double r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }

    }

    // double-combined, filtered
    static final class OFDCPap<T> extends ODCPap<T> {
        final Predicate<? super T> selector;
        OFDCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, Predicate<? super T> selector,
                IntAndObjectToDouble<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping(DoubleOp op) {
            return new OFDCPap<T>
                (ex, origin, fence, array, selector,
                 compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(DoubleToLong op) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (DoubleToObject<? extends U> op) {
            return new OFOCPap<T,U>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new OFOCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final Predicate s = selector;
            final IntAndObjectToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(t)) {
                    double y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DFDCPap extends DDCPap {
        final DoublePredicate selector;
        DFDCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoublePredicate selector,
                IntAndDoubleToDouble op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping< U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DFOCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final DoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final DoublePredicate s = selector;
            final IntAndDoubleToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(t)) {
                    double y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LFDCPap extends LDCPap {
        final LongPredicate selector;
        LFDCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongPredicate selector, IntAndLongToDouble op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelLongArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping(DoubleToLong op) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping< U> withMapping
            (DoubleToObject<? extends U> op) {
            return new LFOCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new LFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final LongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final LongPredicate s = selector;
            final IntAndLongToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(t)) {
                    double y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // double-combined, relational
    static final class ORDCPap<T> extends ODCPap<T> {
        final IntAndObjectPredicate<? super T> selector;
        ORDCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectPredicate<? super T> selector,
                IntAndObjectToDouble<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping(DoubleOp op) {
            return new ORDCPap<T>
                (ex, origin, fence, array, selector,
                 compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(DoubleToLong op) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (DoubleToObject<? extends U> op) {
            return new OROCPap<T,U>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new OROCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final IntAndObjectPredicate s = selector;
            final IntAndObjectToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(i, t)) {
                    double y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DRDCPap extends DDCPap {
        final IntAndDoublePredicate selector;
        DRDCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoublePredicate selector,
                IntAndDoubleToDouble op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(DoubleToLong op) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping< U> withMapping
            (DoubleToObject<? extends U> op) {
            return new DROCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new DROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndDoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final IntAndDoublePredicate s = selector;
            final IntAndDoubleToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(i, t)) {
                    double y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LRDCPap extends LDCPap {
        final IntAndLongPredicate selector;
        LRDCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongPredicate selector, IntAndLongToDouble op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelLongArrayWithDoubleMapping withMapping(DoubleOp op) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping(DoubleToLong op) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping< U> withMapping
            (DoubleToObject<? extends U> op) {
            return new LROCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndDoubleToObject<? extends V> mapper) {
            return new LROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndDoubleToDouble mapper) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndDoubleToLong mapper) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            final IntAndLongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToDouble f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }

        double leafReduce(int lo, int hi, DoubleReducer reducer, double base) {
            final IntAndLongPredicate s = selector;
            final IntAndLongToDouble f = op;
            boolean gotFirst = false;
            double r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(i, t)) {
                    double y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // long-combined
    abstract static class OLMPap<T> extends ParallelArrayWithLongMapping<T> {
        final ObjectToLong<? super T> op;
        OLMPap(ForkJoinPool ex, int origin, int fence,
               T[] array, final ObjectToLong<? super T> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final long lget(int i) { return op.op(this.array[i]); }
        final Object oget(int i) { return Long.valueOf(lget(i)); }
        final double dget(int i) { return (double)(lget(i)); }

        final void leafTransfer(int lo, int hi, long[] dest, int offset) {
            final ObjectToLong f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       long[] dest, int offset) {
            final Object[] a = this.array;
            final ObjectToLong f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }
    }

    abstract static class DLMPap extends ParallelDoubleArrayWithLongMapping {
        final DoubleToLong op;
        DLMPap(ForkJoinPool ex, int origin, int fence,
               double[] array, DoubleToLong op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final long lget(int i) { return op.op(this.array[i]); }
        final Object oget(int i) { return Long.valueOf(lget(i)); }
        final double dget(int i) { return (double)(lget(i)); }

        final void leafTransfer(int lo, int hi, long[] dest, int offset) {
            final double[] a = this.array;
            final DoubleToLong f = op;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       long[] dest, int offset) {
            final double[] a = this.array;
            final DoubleToLong f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }

    }

    abstract static class LLMPap extends ParallelLongArrayWithLongMapping {
        final LongOp op;
        LLMPap(ForkJoinPool ex, int origin, int fence,
               long[] array, LongOp op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final long lget(int i) { return op.op(this.array[i]); }
        final Object oget(int i) { return Long.valueOf(lget(i)); }
        final double dget(int i) { return (double)(lget(i)); }

        final void leafTransfer(int lo, int hi, long[] dest, int offset) {
            final long[] a = this.array;
            final LongOp f = op;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       long[] dest, int offset) {
            final long[] a = this.array;
            final LongOp f = op;
            for (int i = loIdx; i < hiIdx; ++i)
                dest[offset++] = f.op(a[indices[i]]);
        }

    }

    // long-combined, unfiltered
    static final class OULMPap<T> extends OLMPap<T> {
        OULMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, ObjectToLong<? super T> op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelArrayWithDoubleMapping<T> withMapping(LongToDouble op) {
            return new OUDMPap<T>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(LongOp op) {
            return new OULMPap<T>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (LongToObject<? extends U> op) {
            return new OUOMPap<T,U>(ex, origin, fence, array,
                                    CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new OUOCPap<T,V>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndLongToLong mapper) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final Object[] a = this.array;
            final ObjectToLong f = op;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            final Object[] a = this.array;
            final ObjectToLong f = op;
            long r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }
    }

    static final class DULMPap extends DLMPap {
        DULMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoubleToLong op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new DUDMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (LongOp op) {
            return new DULMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new DUOMPap<U>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new DUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final double[] a = this.array;
            final DoubleToLong f = op;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            final double[] a = this.array;
            final DoubleToLong f = op;
            long r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }

    }

    static final class LULMPap extends LLMPap {
        LULMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongOp op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
            return new LULMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
            return new LUDMPap(ex, origin, fence, array,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new LUOMPap<U>(ex, origin, fence, array,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final LongOp f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(a[i]));
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            final long[] a = this.array;
            final LongOp f = op;
            long r = f.op(a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(a[i]));
            return r;
        }
    }

    // long-combined, filtered
    static final class OFLMPap<T> extends OLMPap<T> {
        final Predicate<? super T> selector;
        OFLMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, Predicate<? super T> selector,
                ObjectToLong<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (LongToDouble op) {
            return new OFDMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (LongOp op) {
            return new OFLMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (LongToObject<? extends U> op) {
            return new OFOMPap<T,U>(ex, origin, fence, array, selector,
                                    CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new OFOCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndLongToLong mapper) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final ObjectToLong f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final Predicate s = selector;
            final ObjectToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(t)) {
                    long y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DFLMPap extends DLMPap {
        final DoublePredicate selector;
        DFLMPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoublePredicate selector, DoubleToLong op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new DFDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (LongOp op) {
            return new DFLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new DFOMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new DFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final DoublePredicate s = selector;
            final DoubleToLong f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            boolean gotFirst = false;
            long r = base;
            final double[] a = this.array;
            final DoublePredicate s = selector;
            final DoubleToLong f = op;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(t)) {
                    long y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LFLMPap extends LLMPap {
        final LongPredicate selector;
        LFLMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongPredicate selector, LongOp op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
            return new LFLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
            return new LFDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new LFOMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final LongPredicate s = selector;
            final LongOp f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x))
                    procedure.op(f.op(x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final LongPredicate s = selector;
            final LongOp f = op;
            boolean gotFirst = false;
            long r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(t)) {
                    long y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // Long-mapped, relational
    static final class ORLMPap<T> extends OLMPap<T> {
        final IntAndObjectPredicate<? super T> selector;
        ORLMPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectPredicate<? super T> selector,
                ObjectToLong<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping
            (LongToDouble op) {
            return new ORDMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping
            (LongOp op) {
            return new ORLMPap<T>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (LongToObject<? extends U> op) {
            return new OROMPap<T,U>(ex, origin, fence, array, selector,
                                    CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new OROCPap<T,V>(ex, origin, fence, array, selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndLongToLong mapper) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final ObjectToLong f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final IntAndObjectPredicate s = selector;
            final ObjectToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(i, t)) {
                    long y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class DRLMPap extends DLMPap {
        final IntAndDoublePredicate selector;
        DRLMPap(ForkJoinPool ex, int origin, int fence, double[] array,
                IntAndDoublePredicate selector, DoubleToLong op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new DRDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping
            (LongOp op) {
            return new DRLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new DROMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new DROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndDoublePredicate s = selector;
            final DoubleToLong f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            boolean gotFirst = false;
            long r = base;
            final double[] a = this.array;
            final IntAndDoublePredicate s = selector;
            final DoubleToLong f = op;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(i, t)) {
                    long y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LRLMPap extends LLMPap {
        final IntAndLongPredicate selector;
        LRLMPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongPredicate selector, LongOp op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
            return new LRLMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
            return new LRDMPap(ex, origin, fence, array, selector,
                               CommonOps.compoundOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping<U> withMapping
            (LongToObject<? extends U> op) {
            return new LROMPap<U>(ex, origin, fence, array, selector,
                                  CommonOps.compoundOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndLongPredicate s = selector;
            final LongOp f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final IntAndLongPredicate s = selector;
            final LongOp f = op;
            boolean gotFirst = false;
            long r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(i, t)) {
                    long y = f.op(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // long-combined
    abstract static class OLCPap<T> extends ParallelArrayWithLongMapping<T> {
        final IntAndObjectToLong<? super T> op;
        OLCPap(ForkJoinPool ex, int origin, int fence,
               T[] array, IntAndObjectToLong<? super T> op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final long lget(int i) { return op.op(i, this.array[i]); }
        final Object oget(int i) { return Long.valueOf(lget(i)); }
        final double dget(int i) { return (double)(lget(i)); }

        final void leafTransfer(int lo, int hi, long[] dest, int offset) {
            final IntAndObjectToLong f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       long[] dest, int offset) {
            final Object[] a = this.array;
            final IntAndObjectToLong f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    abstract static class DLCPap extends ParallelDoubleArrayWithLongMapping {
        final IntAndDoubleToLong op;
        DLCPap(ForkJoinPool ex, int origin, int fence,
               double[] array, IntAndDoubleToLong op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final long lget(int i) { return op.op(i, this.array[i]); }
        final Object oget(int i) { return Long.valueOf(lget(i)); }
        final double dget(int i) { return (double)(lget(i)); }

        final void leafTransfer(int lo, int hi, long[] dest, int offset) {
            final IntAndDoubleToLong f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       long[] dest, int offset) {
            final double[] a = this.array;
            final IntAndDoubleToLong f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    abstract static class LLCPap extends ParallelLongArrayWithLongMapping {
        final IntAndLongToLong op;
        LLCPap(ForkJoinPool ex, int origin, int fence,
               long[] array, IntAndLongToLong op) {
            super(ex, origin, fence, array);
            this.op = op;
        }

        final boolean hasMap() { return true; }
        final long lget(int i) { return op.op(i, this.array[i]); }
        final Object oget(int i) { return Long.valueOf(lget(i)); }
        final double dget(int i) { return (double)(lget(i)); }

        final void leafTransfer(int lo, int hi, long[] dest, int offset) {
            final IntAndLongToLong f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                dest[offset++] = f.op(i, a[i]);
        }

        final void leafTransferByIndex(int[] indices, int loIdx, int hiIdx,
                                       long[] dest, int offset) {
            final long[] a = this.array;
            final IntAndLongToLong f = op;
            for (int i = loIdx; i < hiIdx; ++i) {
                int idx = indices[i];
                dest[offset++] = f.op(idx, a[idx]);
            }
        }
    }

    // long-combined, unfiltered
    static final class OULCPap<T> extends OLCPap<T> {
        OULCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectToLong<? super T> op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelArrayWithDoubleMapping<T> withMapping(LongToDouble op) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(LongOp op) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (LongToObject<? extends U> op) {
            return new OUOCPap<T,U>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new OUOCPap<T,V>(ex, origin, fence, array,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new OUDCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndLongToLong mapper) {
            return new OULCPap<T>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndObjectToLong f = op;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            final Object[] a = this.array;
            final IntAndObjectToLong f = op;
            long r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }

    }
    static final class DULCPap extends DLCPap {
        DULCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoubleToLong op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(LongOp op) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping< U> withMapping
            (LongToObject<? extends U> op) {
            return new DUOCPap<U>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new DUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new DUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new DULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndDoubleToLong f = op;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            final double[] a = this.array;
            final IntAndDoubleToLong f = op;
            long r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }
    }

    static final class LULCPap extends LLCPap {
        LULCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongToLong op) {
            super(ex, origin, fence, array, op);
        }

        public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping< U> withMapping
            (LongToObject<? extends U> op) {
            return new LUOCPap<U>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LUOCPap<V>(ex, origin, fence, array,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LUDCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LULCPap(ex, origin, fence, array,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndLongToLong f = op;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i)
                procedure.op(f.op(i, a[i]));
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            final long[] a = this.array;
            final IntAndLongToLong f = op;
            long r = f.op(lo, a[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.op(r, f.op(i, a[i]));
            return r;
        }
    }

    // long-combined, filtered
    static final class OFLCPap<T> extends OLCPap<T> {
        final Predicate<? super T> selector;
        OFLCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, Predicate<? super T> selector,
                IntAndObjectToLong<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping(LongToDouble op) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(LongOp op) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (LongToObject<? extends U> op) {
            return new OFOCPap<T,U>(ex, origin, fence, array,
                                    selector,
                                    compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new OFOCPap<T,V>(ex, origin, fence, array,
                                    selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new OFDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndLongToLong mapper) {
            return new OFLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final Predicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToLong f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final Predicate s = selector;
            final IntAndObjectToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(t)) {
                    long y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }

    }

    static final class DFLCPap extends DLCPap {
        final DoublePredicate selector;
        DFLCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, DoublePredicate selector,
                IntAndDoubleToLong op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(LongOp op) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping< U> withMapping
            (LongToObject<? extends U> op) {
            return new DFOCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new DFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new DFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new DFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final DoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToLong f = op;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final DoublePredicate s = selector;
            final IntAndDoubleToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(t)) {
                    long y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LFLCPap extends LLCPap {
        final LongPredicate selector;
        LFLCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, LongPredicate selector,
                IntAndLongToLong op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(this.array[i]); }

        public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping< U> withMapping
            (LongToObject<? extends U> op) {
            return new LFOCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LFOCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LFDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LFLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final LongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToLong f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(x))
                    procedure.op(f.op(i, x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final LongPredicate s = selector;
            final IntAndLongToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(t)) {
                    long y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    // long-combined, relational
    static final class ORLCPap<T> extends OLCPap<T> {
        final IntAndObjectPredicate<? super T> selector;
        ORLCPap(ForkJoinPool ex, int origin, int fence,
                T[] array, IntAndObjectPredicate<? super T> selector,
                IntAndObjectToLong<? super T> op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelArrayWithDoubleMapping<T> withMapping(LongToDouble op) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public ParallelArrayWithLongMapping<T> withMapping(LongOp op) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <U> ParallelArrayWithMapping<T,U> withMapping
            (LongToObject<? extends U> op) {
            return new OROCPap<T,U>(ex, origin, fence, array,
                                    selector,
                                    compoundIndexedOp(this.op, op));
        }

        public <V> ParallelArrayWithMapping<T,V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new OROCPap<T,V>(ex, origin, fence, array,
                                    selector,
                                    compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithDoubleMapping<T> withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new ORDCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelArrayWithLongMapping<T> withIndexedMapping
            (IntAndLongToLong mapper) {
            return new ORLCPap<T>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndObjectPredicate s = selector;
            final Object[] a = this.array;
            final IntAndObjectToLong f = op;
            for (int i = lo; i < hi; ++i) {
                Object x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final IntAndObjectPredicate s = selector;
            final IntAndObjectToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final Object[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                Object t = a[i];
                if (s.op(i, t)) {
                    long y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }

    }

    static final class DRLCPap extends DLCPap {
        final IntAndDoublePredicate selector;
        DRLCPap(ForkJoinPool ex, int origin, int fence,
                double[] array, IntAndDoublePredicate selector,
                IntAndDoubleToLong op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelDoubleArrayWithDoubleMapping withMapping
            (LongToDouble op) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelDoubleArrayWithLongMapping withMapping(LongOp op) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelDoubleArrayWithMapping< U> withMapping
            (LongToObject<? extends U> op) {
            return new DROCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelDoubleArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new DROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new DRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelDoubleArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new DRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndDoublePredicate s = selector;
            final double[] a = this.array;
            final IntAndDoubleToLong f = op;
            for (int i = lo; i < hi; ++i) {
                double x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final IntAndDoublePredicate s = selector;
            final IntAndDoubleToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final double[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                double t = a[i];
                if (s.op(i, t)) {
                    long y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    static final class LRLCPap extends LLCPap {
        final IntAndLongPredicate selector;
        LRLCPap(ForkJoinPool ex, int origin, int fence,
                long[] array, IntAndLongPredicate selector,
                IntAndLongToLong op) {
            super(ex, origin, fence, array, op);
            this.selector = selector;
        }

        boolean hasFilter() { return true; }
        boolean isSelected(int i) { return selector.op(i, this.array[i]); }

        public ParallelLongArrayWithDoubleMapping withMapping(LongToDouble op) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public ParallelLongArrayWithLongMapping withMapping(LongOp op) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, op));
        }

        public <U> ParallelLongArrayWithMapping< U> withMapping
            (LongToObject<? extends U> op) {
            return new LROCPap<U>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, op));
        }

        public <V> ParallelLongArrayWithMapping<V> withIndexedMapping
            (IntAndLongToObject<? extends V> mapper) {
            return new LROCPap<V>(ex, origin, fence, array, selector,
                                  compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithDoubleMapping withIndexedMapping
            (IntAndLongToDouble mapper) {
            return new LRDCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        public ParallelLongArrayWithLongMapping withIndexedMapping
            (IntAndLongToLong mapper) {
            return new LRLCPap(ex, origin, fence, array, selector,
                               compoundIndexedOp(this.op, mapper));
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            final IntAndLongPredicate s = selector;
            final long[] a = this.array;
            final IntAndLongToLong f = op;
            for (int i = lo; i < hi; ++i) {
                long x = a[i];
                if (s.op(i, x))
                    procedure.op(f.op(i, x));
            }
        }

        long leafReduce(int lo, int hi, LongReducer reducer, long base) {
            final IntAndLongPredicate s = selector;
            final IntAndLongToLong f = op;
            boolean gotFirst = false;
            long r = base;
            final long[] a = this.array;
            for (int i = lo; i < hi; ++i) {
                long t = a[i];
                if (s.op(i, t)) {
                    long y = f.op(i, t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.op(r, y);
                }
            }
            return r;
        }
    }

    /*
     * Iterator support
     */

    class SequentiallyAsDouble implements Iterable<Double> {
        public Iterator<Double> iterator() {
            if (hasFilter())
                return new FilteredAsDoubleIterator();
            else
                return new UnfilteredAsDoubleIterator();
        }
    }

    class UnfilteredAsDoubleIterator implements Iterator<Double> {
        int cursor = origin;
        public boolean hasNext() { return cursor < fence; }
        public Double next() {
            if (cursor >= fence)
                throw new NoSuchElementException();
            return Double.valueOf(dget(cursor++));
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class FilteredAsDoubleIterator implements Iterator<Double> {
        double next;
        int cursor;
        FilteredAsDoubleIterator() {
            cursor = origin;
            advance() ;
        }
        private void advance() {
            while (cursor < fence) {
                if (isSelected(cursor)) {
                    next = dget(cursor);
                    break;
                }
                cursor++;
            }
        }

        public boolean hasNext() { return cursor < fence; }
        public Double next() {
            if (cursor >= fence)
                throw new NoSuchElementException();
            Double x = Double.valueOf(next);
            cursor++;
            advance();
            return x;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class SequentiallyAsLong implements Iterable<Long> {
        public Iterator<Long> iterator() {
            if (hasFilter())
                return new FilteredAsLongIterator();
            else
                return new UnfilteredAsLongIterator();
        }
    }

    class UnfilteredAsLongIterator implements Iterator<Long> {
        int cursor = origin;
        public boolean hasNext() { return cursor < fence; }
        public Long next() {
            if (cursor >= fence)
                throw new NoSuchElementException();
            return Long.valueOf(lget(cursor++));
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class FilteredAsLongIterator implements Iterator<Long> {
        long next;
        int cursor;
        FilteredAsLongIterator() {
            cursor = origin;
            advance() ;
        }
        private void advance() {
            while (cursor < fence) {
                if (isSelected(cursor)) {
                    next = lget(cursor);
                    break;
                }
                cursor++;
            }
        }

        public boolean hasNext() { return cursor < fence; }
        public Long next() {
            if (cursor >= fence)
                throw new NoSuchElementException();
            Long x = Long.valueOf(next);
            cursor++;
            advance();
            return x;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class Sequentially<U> implements Iterable<U> {
        public Iterator<U> iterator() {
            if (hasFilter())
                return new FilteredIterator<U>();
            else
                return new UnfilteredIterator<U>();
        }
    }

    class UnfilteredIterator<U> implements Iterator<U> {
        int cursor = origin;
        public boolean hasNext() { return cursor < fence; }
        public U next() {
            if (cursor >= fence)
                throw new NoSuchElementException();
            return (U)oget(cursor++);
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class FilteredIterator<U> implements Iterator<U> {
        Object next;
        int cursor;
        FilteredIterator() {
            cursor = origin;
            advance() ;
        }
        private void advance() {
            while (cursor < fence) {
                if (isSelected(cursor)) {
                    next = oget(cursor);
                    break;
                }
                cursor++;
            }
        }

        public boolean hasNext() { return cursor < fence; }
        public U next() {
            if (cursor >= fence)
                throw new NoSuchElementException();
            U x = (U)next;
            cursor++;
            advance();
            return x;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // Zillions of little classes to support binary ops
    // ToDo: specialize to flatten dispatch

    static <T,U,V,W> IntAndObjectToObject<T,V> indexedMapper
        (final BinaryOp<? super T, ? super U, ? extends V> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndObjectToObject<T,V>() {
            final int offset = u.origin - origin;
            public V op(int i, T a) { return combiner.op(a, (U)(u.oget(i+offset))); }
        };
    }

    static <T,U,W> IntAndObjectToDouble<T> indexedMapper
        (final ObjectAndObjectToDouble<? super T, ? super U> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndObjectToDouble<T>() {
            final int offset = u.origin - origin;
            public double op(int i, T a) { return combiner.op(a, (U)(u.oget(i+offset))); }
        };
    }

    static <T,U,W> IntAndObjectToLong<T> indexedMapper
        (final ObjectAndObjectToLong<? super T, ? super U> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndObjectToLong<T>() {
            final int offset = u.origin - origin;
            public long op(int i, T a) { return combiner.op(a, (U)(u.oget(i+offset))); }
        };
    }

    static <T,V> IntAndObjectToObject<T,V> indexedMapper
        (final ObjectAndDoubleToObject<? super T, ? extends V> combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndObjectToObject<T,V>() {
            final int offset = u.origin - origin;
            public V op(int i, T a) { return combiner.op(a, u.dget(i+offset)); }
        };
    }

    static <T> IntAndObjectToDouble<T> indexedMapper
        (final ObjectAndDoubleToDouble<? super T> combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndObjectToDouble<T>() {
            final int offset = u.origin - origin;
            public double op(int i, T a) { return combiner.op(a, u.dget(i+offset)); }
        };
    }

    static <T,U> IntAndObjectToLong<T> indexedMapper
        (final ObjectAndDoubleToLong<? super T> combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndObjectToLong<T>() {
            final int offset = u.origin - origin;
            public long op(int i, T a) { return combiner.op(a, u.dget(i+offset)); }
        };
    }

    static <T,V> IntAndObjectToObject<T,V> indexedMapper
        (final ObjectAndLongToObject<? super T, ? extends V> combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndObjectToObject<T,V>() {
            final int offset = u.origin - origin;
            public V op(int i, T a) { return combiner.op(a, u.lget(i+offset)); }
        };
    }

    static <T> IntAndObjectToDouble<T> indexedMapper
        (final ObjectAndLongToDouble<? super T> combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndObjectToDouble<T>() {
            final int offset = u.origin - origin;
            public double op(int i, T a) { return combiner.op(a, u.lget(i+offset)); }
        };
    }

    static <T> IntAndObjectToLong<T> indexedMapper
        (final ObjectAndLongToLong<? super T> combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndObjectToLong<T>() {
            final int offset = u.origin - origin;
            public long op(int i, T a) { return combiner.op(a, u.lget(i+offset)); }
        };
    }

    static <U,V,W> IntAndDoubleToObject<V> indexedMapper
        (final DoubleAndObjectToObject<? super U, ? extends V> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndDoubleToObject<V>() {
            final int offset = u.origin - origin;
            public V op(int i, double a) { return combiner.op(a, (U)(u.oget(i+offset))); }
        };
    }

    static <U,W> IntAndDoubleToDouble indexedMapper
        (final DoubleAndObjectToDouble<? super U> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndDoubleToDouble() {
                final int offset = u.origin - origin;
                public double op(int i, double a) { return combiner.op(a, (U)(u.oget(i+offset))); }
            };
    }

    static <U,W> IntAndDoubleToLong indexedMapper
        (final DoubleAndObjectToLong<? super U> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndDoubleToLong() {
                final int offset = u.origin - origin;
                public long op(int i, double a) { return combiner.op(a, (U)(u.oget(i+offset))); }
            };
    }

    static <V> IntAndDoubleToObject<V> indexedMapper
        (final DoubleAndDoubleToObject<? extends V> combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndDoubleToObject<V>() {
            final int offset = u.origin - origin;
            public V op(int i, double a) { return combiner.op(a, u.dget(i+offset)); }
        };
    }

    static IntAndDoubleToDouble indexedMapper
        (final BinaryDoubleOp combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndDoubleToDouble() {
                final int offset = u.origin - origin;
                public double op(int i, double a) { return combiner.op(a, u.dget(i+offset)); }
            };
    }

    static IntAndDoubleToLong indexedMapper
        (final DoubleAndDoubleToLong combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndDoubleToLong() {
                final int offset = u.origin - origin;
                public long op(int i, double a) { return combiner.op(a, u.dget(i+offset)); }
            };
    }

    static <V> IntAndDoubleToObject<V> indexedMapper
        (final DoubleAndLongToObject<? extends V> combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndDoubleToObject<V>() {
            final int offset = u.origin - origin;
            public V op(int i, double a) { return combiner.op(a, u.lget(i+offset)); }
        };
    }

    static IntAndDoubleToDouble indexedMapper
        (final DoubleAndLongToDouble combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndDoubleToDouble() {
                final int offset = u.origin - origin;
                public double op(int i, double a) { return combiner.op(a, u.lget(i+offset)); }
            };
    }

    static IntAndDoubleToLong indexedMapper
        (final DoubleAndLongToLong combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndDoubleToLong() {
                final int offset = u.origin - origin;
                public long op(int i, double a) { return combiner.op(a, u.lget(i+offset)); }
            };
    }

    static <U,V,W> IntAndLongToObject<V> indexedMapper
        (final LongAndObjectToObject<? super U, ? extends V> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndLongToObject<V>() {
            final int offset = u.origin - origin;
            public V op(int i, long a) { return combiner.op(a, (U)(u.oget(i+offset))); }
        };
    }

    static <U,W> IntAndLongToDouble indexedMapper
        (final LongAndObjectToDouble<? super U> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndLongToDouble() {
                final int offset = u.origin - origin;
                public double op(int i, long a) { return combiner.op(a, (U)(u.oget(i+offset))); }
            };
    }

    static <U,W> IntAndLongToLong indexedMapper
        (final LongAndObjectToLong<? super U> combiner,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndLongToLong() {
                final int offset = u.origin - origin;
                public long op(int i, long a) { return combiner.op(a, (U)(u.oget(i+offset))); }
            };
    }

    static <V> IntAndLongToObject<V> indexedMapper
        (final LongAndDoubleToObject<? extends V> combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndLongToObject<V>() {
            final int offset = u.origin - origin;
            public V op(int i, long a) { return combiner.op(a, u.dget(i+offset)); }
        };
    }

    static IntAndLongToDouble indexedMapper
        (final LongAndDoubleToDouble combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndLongToDouble() {
                final int offset = u.origin - origin;
                public double op(int i, long a) { return combiner.op(a, u.dget(i+offset)); }
            };
    }

    static IntAndLongToLong indexedMapper
        (final LongAndDoubleToLong combiner,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndLongToLong() {
                final int offset = u.origin - origin;
                public long op(int i, long a) { return combiner.op(a, u.dget(i+offset)); }
            };
    }

    static <V> IntAndLongToObject<V> indexedMapper
        (final LongAndLongToObject<? extends V> combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndLongToObject<V>() {
            final int offset = u.origin - origin;
            public V op(int i, long a) { return combiner.op(a, u.lget(i+offset)); }
        };
    }

    static IntAndLongToDouble indexedMapper
        (final LongAndLongToDouble combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndLongToDouble() {
                final int offset = u.origin - origin;
                public double op(int i, long a) { return combiner.op(a, u.lget(i+offset)); }
            };
    }

    static IntAndLongToLong indexedMapper
        (final BinaryLongOp combiner,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndLongToLong() {
                final int offset = u.origin - origin;
                public long op(int i, long a) { return combiner.op(a, u.lget(i+offset)); }
            };
    }

    static <T,U,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final IntAndObjectToObject<? super T, ? extends U> fst,
         final IntAndObjectToObject<? super U, ? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <T,U> IntAndObjectToDouble<T> compoundIndexedOp
        (final IntAndObjectToObject<? super T, ? extends U> fst,
         final IntAndObjectToDouble<? super U> snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <T,U> IntAndObjectToLong<T> compoundIndexedOp
        (final IntAndObjectToObject<? super T, ? extends U> fst,
         final IntAndObjectToLong<? super U> snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <U,V> IntAndDoubleToObject<V> compoundIndexedOp
        (final IntAndDoubleToObject<? extends U> fst,
         final IntAndObjectToObject<? super U, ? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <U> IntAndDoubleToDouble compoundIndexedOp
        (final IntAndDoubleToObject<? extends U> fst,
         final IntAndObjectToDouble<? super U> snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <U> IntAndDoubleToLong compoundIndexedOp
        (final IntAndDoubleToObject<? extends U> fst,
         final IntAndObjectToLong<? super U> snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <U,V> IntAndLongToObject<V> compoundIndexedOp
        (final IntAndLongToObject<? extends U> fst,
         final IntAndObjectToObject<? super U, ? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <U> IntAndLongToDouble compoundIndexedOp
        (final IntAndLongToObject<? extends U> fst,
         final IntAndObjectToDouble<? super U> snd) {
        return new IntAndLongToDouble() {
                public double op(int i, long a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <U> IntAndLongToLong compoundIndexedOp
        (final IntAndLongToObject<? extends U> fst,
         final IntAndObjectToLong<? super U> snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <T,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final IntAndObjectToDouble<? super T> fst,
         final IntAndDoubleToObject<? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToDouble<T> compoundIndexedOp
        (final IntAndObjectToDouble<? super T> fst,
         final IntAndDoubleToDouble snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToLong<T> compoundIndexedOp
        (final IntAndObjectToLong<? super T> fst,
         final IntAndLongToLong snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <V> IntAndDoubleToObject<V> compoundIndexedOp
        (final IntAndDoubleToLong fst,
         final IntAndLongToObject<? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static IntAndDoubleToDouble compoundIndexedOp
        (final IntAndDoubleToDouble fst,
         final IntAndDoubleToDouble snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static IntAndDoubleToLong compoundIndexedOp
        (final IntAndDoubleToDouble fst,
         final IntAndDoubleToLong snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <V> IntAndLongToObject<V> compoundIndexedOp
        (final IntAndLongToDouble fst,
         final IntAndDoubleToObject<? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static IntAndLongToDouble compoundIndexedOp
        (final IntAndLongToDouble fst,
         final IntAndDoubleToDouble snd) {
        return new IntAndLongToDouble() {
                public double op(int i, long a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static IntAndLongToLong compoundIndexedOp
        (final IntAndLongToDouble fst,
         final IntAndDoubleToLong snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <T,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final IntAndObjectToLong<? super T> fst,
         final IntAndLongToObject<? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToDouble<T> compoundIndexedOp
        (final IntAndObjectToLong<? super T> fst,
         final IntAndLongToDouble snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToLong<T> compoundIndexedOp
        (final IntAndObjectToDouble<? super T> fst,
         final IntAndDoubleToLong snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static <V> IntAndDoubleToObject<V> compoundIndexedOp
        (final IntAndDoubleToDouble fst,
         final IntAndDoubleToObject<? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static IntAndDoubleToDouble compoundIndexedOp
        (final IntAndDoubleToLong fst,
         final IntAndLongToDouble snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static IntAndDoubleToLong compoundIndexedOp
        (final IntAndDoubleToLong fst,
         final IntAndLongToLong snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <V> IntAndLongToObject<V> compoundIndexedOp
        (final IntAndLongToLong fst,
         final IntAndLongToObject<? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(i, fst.op(i, a)); }
        };
    }

    static IntAndLongToDouble compoundIndexedOp
        (final IntAndLongToLong fst,
         final IntAndLongToDouble snd) {
        return new IntAndLongToDouble() {
                public double op(int i, long a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static IntAndLongToLong compoundIndexedOp
        (final IntAndLongToLong fst,
         final IntAndLongToLong snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(i, fst.op(i, a)); }
            };
    }

    static <T,U,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final IntAndObjectToObject<? super T, ? extends U> fst,
         final Op<? super U, ? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <T,U> IntAndObjectToDouble<T> compoundIndexedOp
        (final IntAndObjectToObject<? super T, ? extends U> fst,
         final ObjectToDouble<? super U> snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <T,U> IntAndObjectToLong<T> compoundIndexedOp
        (final IntAndObjectToObject<? super T, ? extends U> fst,
         final ObjectToLong<? super U> snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <U,V> IntAndDoubleToObject<V> compoundIndexedOp
        (final IntAndDoubleToObject<? extends U> fst,
         final Op<? super U, ? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <U> IntAndDoubleToDouble compoundIndexedOp
        (final IntAndDoubleToObject<? extends U> fst,
         final ObjectToDouble<? super U> snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <U> IntAndDoubleToLong compoundIndexedOp
        (final IntAndDoubleToObject<? extends U> fst,
         final ObjectToLong<? super U> snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <U,V> IntAndLongToObject<V> compoundIndexedOp
        (final IntAndLongToObject<? extends U> fst,
         final Op<? super U, ? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <U> IntAndLongToDouble compoundIndexedOp
        (final IntAndLongToObject<? extends U> fst,
         final ObjectToDouble<? super U> snd) {
        return new IntAndLongToDouble() {
                public double op(int i, long a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <U> IntAndLongToLong compoundIndexedOp
        (final IntAndLongToObject<? extends U> fst,
         final ObjectToLong<? super U> snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <T,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final IntAndObjectToDouble<? super T> fst,
         final DoubleToObject<? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToDouble<T> compoundIndexedOp
        (final IntAndObjectToDouble<? super T> fst, final DoubleOp snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToLong<T> compoundIndexedOp
        (final IntAndObjectToDouble<? super T> fst, final DoubleToLong snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <V> IntAndDoubleToObject<V> compoundIndexedOp
        (final IntAndDoubleToDouble fst,
         final DoubleToObject<? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(fst.op(i, a)); }
        };
    }

    static IntAndDoubleToDouble compoundIndexedOp
        (final IntAndDoubleToDouble fst, final DoubleOp snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(fst.op(i, a)); }
            };
    }

    static IntAndDoubleToLong compoundIndexedOp
        (final IntAndDoubleToDouble fst, final DoubleToLong snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <V> IntAndLongToObject<V> compoundIndexedOp
        (final IntAndLongToDouble fst, final DoubleToObject<? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(fst.op(i, a)); }
        };
    }

    static IntAndLongToDouble compoundIndexedOp
        (final IntAndLongToDouble fst, final DoubleOp snd) {
        return new IntAndLongToDouble() {
                public double op(int i,long a) { return snd.op(fst.op(i, a)); }
            };
    }

    static IntAndLongToLong compoundIndexedOp
        (final IntAndLongToDouble fst, final DoubleToLong snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <T,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final IntAndObjectToLong<? super T> fst,
         final LongToObject<? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToDouble<T> compoundIndexedOp
        (final IntAndObjectToLong<? super T> fst, final LongToDouble snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <T> IntAndObjectToLong<T> compoundIndexedOp
        (final IntAndObjectToLong<? super T> fst, final LongOp snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(fst.op(i, a)); }
        };
    }

    static <V> IntAndDoubleToObject<V> compoundIndexedOp
        (final IntAndDoubleToLong fst, final LongToObject<? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(fst.op(i, a)); }
        };
    }

    static IntAndDoubleToDouble compoundIndexedOp
        (final IntAndDoubleToLong fst, final LongToDouble snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(fst.op(i, a)); }
            };
    }

    static IntAndDoubleToLong compoundIndexedOp
        (final IntAndDoubleToLong fst, final LongOp snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <V> IntAndLongToObject<V> compoundIndexedOp
        (final IntAndLongToLong fst, final LongToObject<? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(fst.op(i, a)); }
        };
    }

    static IntAndLongToDouble compoundIndexedOp
        (final IntAndLongToLong fst, final LongToDouble snd) {
        return new IntAndLongToDouble() {
                public double op(int i,long a) { return snd.op(fst.op(i, a)); }
            };
    }

    static IntAndLongToLong compoundIndexedOp
        (final IntAndLongToLong fst,
         final LongOp snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(fst.op(i, a)); }
            };
    }

    static <T,U,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final Op<? super T, ? extends U> fst,
         final IntAndObjectToObject<? super U, ? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <T,U> IntAndObjectToDouble<T> compoundIndexedOp
        (final Op<? super T, ? extends U> fst,
         final IntAndObjectToDouble<? super U> snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <T,U> IntAndObjectToLong<T> compoundIndexedOp
        (final Op<? super T, ? extends U> fst,
         final IntAndObjectToLong<? super U> snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <U,V> IntAndDoubleToObject<V> compoundIndexedOp
        (final DoubleToObject<? extends U> fst,
         final IntAndObjectToObject<? super U, ? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <U> IntAndDoubleToDouble compoundIndexedOp
        (final DoubleToObject<? extends U> fst,
         final IntAndObjectToDouble<? super U> snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(i, fst.op(a)); }
            };
    }

    static <U> IntAndDoubleToLong compoundIndexedOp
        (final DoubleToObject<? extends U> fst,
         final IntAndObjectToLong<? super U> snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(i, fst.op(a)); }
            };
    }

    static <U,V> IntAndLongToObject<V> compoundIndexedOp
        (final LongToObject<? extends U> fst,
         final IntAndObjectToObject<? super U, ? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <U> IntAndLongToDouble compoundIndexedOp
        (final LongToObject<? extends U> fst,
         final IntAndObjectToDouble<? super U> snd) {
        return new IntAndLongToDouble() {
                public double op(int i, long a) { return snd.op(i, fst.op(a)); }
            };
    }

    static <U> IntAndLongToLong compoundIndexedOp
        (final LongToObject<? extends U> fst,
         final IntAndObjectToLong<? super U> snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(i, fst.op(a)); }
            };
    }

    static <T,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final ObjectToDouble<? super T> fst,
         final IntAndDoubleToObject<? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <T> IntAndObjectToDouble<T> compoundIndexedOp
        (final ObjectToDouble<? super T> fst, final IntAndDoubleToDouble snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <T> IntAndObjectToLong<T> compoundIndexedOp
        (final ObjectToDouble<? super T> fst, final IntAndDoubleToLong snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <V> IntAndDoubleToObject<V> compoundIndexedOp
        (final DoubleOp fst, final IntAndDoubleToObject<? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(i, fst.op(a)); }
        };
    }

    static IntAndDoubleToDouble compoundIndexedOp
        (final DoubleOp fst, final IntAndDoubleToDouble snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(i, fst.op(a)); }
            };
    }

    static IntAndDoubleToLong compoundIndexedOp
        (final DoubleOp fst, final IntAndDoubleToLong snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(i, fst.op(a)); }
            };
    }

    static <V> IntAndLongToObject<V> compoundIndexedOp
        (final LongToDouble fst, final IntAndDoubleToObject<? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(i, fst.op(a)); }
        };
    }

    static IntAndLongToDouble compoundIndexedOp
        (final LongToDouble fst, final IntAndDoubleToDouble snd) {
        return new IntAndLongToDouble() {
                public double op(int i, long a) { return snd.op(i, fst.op(a)); }
            };
    }

    static IntAndLongToLong compoundIndexedOp
        (final LongToDouble fst, final IntAndDoubleToLong snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(i, fst.op(a)); }
            };
    }

    static <T,V> IntAndObjectToObject<T,V> compoundIndexedOp
        (final ObjectToLong<? super T> fst,
         final IntAndLongToObject<? extends V> snd) {
        return new IntAndObjectToObject<T,V>() {
            public V op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <T> IntAndObjectToDouble<T> compoundIndexedOp
        (final ObjectToLong<? super T> fst, final IntAndLongToDouble snd) {
        return new IntAndObjectToDouble<T>() {
            public double op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <T> IntAndObjectToLong<T> compoundIndexedOp
        (final ObjectToLong<? super T> fst, final IntAndLongToLong snd) {
        return new IntAndObjectToLong<T>() {
            public long op(int i, T a) { return snd.op(i, fst.op(a)); }
        };
    }

    static <V> IntAndDoubleToObject<V> compoundIndexedOp
        (final DoubleToLong fst, final IntAndLongToObject<? extends V> snd) {
        return new IntAndDoubleToObject<V>() {
            public V op(int i, double a) { return snd.op(i, fst.op(a)); }
        };
    }

    static IntAndDoubleToDouble compoundIndexedOp
        (final DoubleToLong fst, final IntAndLongToDouble snd) {
        return new IntAndDoubleToDouble() {
                public double op(int i, double a) { return snd.op(i, fst.op(a)); }
            };
    }

    static IntAndDoubleToLong compoundIndexedOp
        (final DoubleToLong fst, final IntAndLongToLong snd) {
        return new IntAndDoubleToLong() {
                public long op(int i, double a) { return snd.op(i, fst.op(a)); }
            };
    }

    static <V> IntAndLongToObject<V> compoundIndexedOp
        (final LongOp fst, final IntAndLongToObject<? extends V> snd) {
        return new IntAndLongToObject<V>() {
            public V op(int i, long a) { return snd.op(i, fst.op(a)); }
        };
    }

    static IntAndLongToDouble compoundIndexedOp
        (final LongOp fst, final IntAndLongToDouble snd) {
        return new IntAndLongToDouble() {
                public double op(int i, long a) { return snd.op(i, fst.op(a)); }
            };
    }

    static IntAndLongToLong compoundIndexedOp
        (final LongOp fst, final IntAndLongToLong snd) {
        return new IntAndLongToLong() {
                public long op(int i, long a) { return snd.op(i, fst.op(a)); }
            };
    }

    // binary predicates

    static <T,U,W> IntAndObjectPredicate<T> indexedSelector
        (final BinaryPredicate<? super T, ? super U> bp,
         final ParallelArrayWithMapping<W,U> u, final int origin) {
        return new IntAndObjectPredicate<T>() {
            final int offset = u.origin - origin;
            public boolean op(int i, T a) {
                int k = i + offset;
                return u.isSelected(k) && bp.op(a, (U)(u.oget(k)));
            }
        };
    }

    static IntAndDoublePredicate indexedSelector
        (final BinaryDoublePredicate bp,
         final ParallelDoubleArrayWithDoubleMapping u, final int origin) {
        return new IntAndDoublePredicate() {
                final int offset = u.origin - origin;
                public boolean op(int i, double a) {
                    int k = i + offset;
                    return u.isSelected(k) && bp.op(a, u.dget(k));
                }
            };
    }

    static IntAndLongPredicate indexedSelector
        (final BinaryLongPredicate bp,
         final ParallelLongArrayWithLongMapping u, final int origin) {
        return new IntAndLongPredicate() {
                final int offset = u.origin - origin;
                public boolean op(int i, long a) {
                    int k = i + offset;
                    return u.isSelected(k) && bp.op(a, u.lget(k));
                }
            };
    }

    static <S, T extends S> IntAndObjectPredicate<T> compoundIndexedSelector
                         (final Predicate<S> fst, final IntAndObjectPredicate<? super T> snd) {
        return new IntAndObjectPredicate<T>() {
            public boolean op(int i, T a) { return fst.op(a) && snd.op(i, a); }
        };
    }

    static <S, T extends S> IntAndObjectPredicate<T> compoundIndexedSelector
                         (final IntAndObjectPredicate<S> fst,
                          final IntAndObjectPredicate<? super T> snd) {
        return new IntAndObjectPredicate<T>() {
            public boolean op(int i, T a) { return fst.op(i, a) && snd.op(i, a); }
        };
    }

    static <S, T extends S> IntAndObjectPredicate<T> compoundIndexedSelector
                         (final IntAndObjectPredicate<S> fst, final Predicate<? super T> snd) {
        return new IntAndObjectPredicate<T>() {
            public boolean op(int i, T a) { return fst.op(i, a) && snd.op(a); }
        };
    }

    static IntAndDoublePredicate compoundIndexedSelector
        (final DoublePredicate fst, final IntAndDoublePredicate snd) {
        return new IntAndDoublePredicate() {
                public boolean op(int i, double a) { return fst.op(a) && snd.op(i, a); }
            };
    }

    static IntAndDoublePredicate compoundIndexedSelector
        (final IntAndDoublePredicate fst, final IntAndDoublePredicate snd) {
        return new IntAndDoublePredicate() {
                public boolean op(int i, double a) { return fst.op(i, a) && snd.op(i, a); }
            };
    }

    static IntAndDoublePredicate compoundIndexedSelector
        (final IntAndDoublePredicate fst, final DoublePredicate snd) {
        return new IntAndDoublePredicate() {
                public boolean op(int i, double a) { return fst.op(i, a) && snd.op(a); }
            };
    }

    static IntAndLongPredicate compoundIndexedSelector
        (final LongPredicate fst, final IntAndLongPredicate snd) {
        return new IntAndLongPredicate() {
                public boolean op(int i, long a) { return fst.op(a) && snd.op(i, a); }
            };
    }

    static IntAndLongPredicate compoundIndexedSelector
        (final IntAndLongPredicate fst, final IntAndLongPredicate snd) {
        return new IntAndLongPredicate() {
                public boolean op(int i, long a) { return fst.op(i, a) && snd.op(i, a); }
            };
    }

    static IntAndLongPredicate compoundIndexedSelector
        (final IntAndLongPredicate fst, final LongPredicate snd) {
        return new IntAndLongPredicate() {
                public boolean op(int i, long a) { return fst.op(i, a) && snd.op(a); }
            };
    }

}
