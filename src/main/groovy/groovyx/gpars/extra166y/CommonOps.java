/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package groovyx.gpars.extra166y;

import jsr166y.ThreadLocalRandom;

import java.util.Comparator;

import static groovyx.gpars.extra166y.Ops.BinaryDoublePredicate;
import static groovyx.gpars.extra166y.Ops.BinaryIntPredicate;
import static groovyx.gpars.extra166y.Ops.BinaryLongPredicate;
import static groovyx.gpars.extra166y.Ops.BinaryPredicate;
import static groovyx.gpars.extra166y.Ops.DoubleComparator;
import static groovyx.gpars.extra166y.Ops.DoubleGenerator;
import static groovyx.gpars.extra166y.Ops.DoubleOp;
import static groovyx.gpars.extra166y.Ops.DoublePredicate;
import static groovyx.gpars.extra166y.Ops.DoubleReducer;
import static groovyx.gpars.extra166y.Ops.DoubleToLong;
import static groovyx.gpars.extra166y.Ops.DoubleToObject;
import static groovyx.gpars.extra166y.Ops.IntGenerator;
import static groovyx.gpars.extra166y.Ops.IntReducer;
import static groovyx.gpars.extra166y.Ops.LongComparator;
import static groovyx.gpars.extra166y.Ops.LongGenerator;
import static groovyx.gpars.extra166y.Ops.LongOp;
import static groovyx.gpars.extra166y.Ops.LongPredicate;
import static groovyx.gpars.extra166y.Ops.LongReducer;
import static groovyx.gpars.extra166y.Ops.LongToDouble;
import static groovyx.gpars.extra166y.Ops.LongToObject;
import static groovyx.gpars.extra166y.Ops.ObjectToDouble;
import static groovyx.gpars.extra166y.Ops.ObjectToLong;
import static groovyx.gpars.extra166y.Ops.Op;
import static groovyx.gpars.extra166y.Ops.Predicate;
import static groovyx.gpars.extra166y.Ops.Reducer;

/**
 * A collection of static factory methods providing commonly useful
 * implementations of operations.
 */
public class CommonOps {
    private CommonOps() {} // disable construction

    /**
     * Returns a Comparator for Comparable objects.
     */
    public static <T extends Comparable<? super T>> Comparator<T>
                             naturalComparator(Class<T> type) {
        return new Comparator<T>() {
            public int compare(T a, T b) { return a.compareTo(b); }
        };
    }

    /**
     * Returns a reducer returning the maximum of two Comparable
     * elements, treating null as less than any non-null element.
     */
    public static <T extends Comparable<? super T>> Reducer<T>
                             naturalMaxReducer(Class<T> type) {
        return new Reducer<T>() {
            public T op(T a, T b) {
                return (a != null &&
                        (b == null || a.compareTo(b) >= 0)) ? a : b;
            }
        };
    }

    /**
     * Returns a reducer returning the minimum of two Comparable
     * elements, treating null as greater than any non-null element.
     */
    public static <T extends Comparable<? super T>> Reducer<T>
                             naturalMinReducer(Class<T> type) {
        return new Reducer<T>() {
            public T op(T a, T b) {
                return (a != null &&
                        (b == null || a.compareTo(b) <= 0)) ? a : b;
            }
        };
    }

    /**
     * Returns a reducer returning the maximum of two elements, using
     * the given comparator, and treating null as less than any
     * non-null element.
     */
    public static <T> Reducer<T> maxReducer
        (final Comparator<? super T> comparator) {
        return new Reducer<T>() {
            public T op(T a, T b) {
                return (a != null &&
                        (b == null || comparator.compare(a, b) >= 0)) ? a : b;
            }
        };
    }

    /**
     * Returns a reducer returning the minimum of two elements, using
     * the given comparator, and treating null as greater than any
     * non-null element.
     */
    public static <T> Reducer<T> minReducer
        (final Comparator<? super T> comparator) {
        return new Reducer<T>() {
            public T op(T a, T b) {
                return (a != null &&
                        (b == null || comparator.compare(a, b) <= 0)) ? a : b;
            }
        };
    }

    /**
     * Returns a Comparator that casts its arguments as Comparable on
     * each comparison, throwing ClassCastException on failure.
     */
    public static Comparator<Object> castedComparator() {
        return (Comparator<Object>)(RawComparator.cmp);
    }
    static final class RawComparator implements Comparator {
        static final RawComparator cmp = new RawComparator();
        public int compare(Object a, Object b) {
            return ((Comparable)a).compareTo((Comparable)b);
        }
    }

    /**
     * Returns a reducer returning maximum of two values, or
     * {@code null} if both arguments are null, and that casts
     * its arguments as Comparable on each comparison, throwing
     * ClassCastException on failure.
     */
    public static Reducer<Object> castedMaxReducer() {
        return (Reducer<Object>)RawMaxReducer.max;
    }
    static final class RawMaxReducer implements Reducer {
        static final RawMaxReducer max = new RawMaxReducer();
        public Object op(Object a, Object b) {
            return (a != null &&
                    (b == null ||
                     ((Comparable)a).compareTo((Comparable)b) >= 0)) ? a : b;
        }
    }

    /**
     * Returns a reducer returning minimum of two values, or
     * {@code null} if both arguments are null, and that casts
     * its arguments as Comparable on each comparison, throwing
     * ClassCastException on failure.
     */
    public static Reducer<Object> castedMinReducer() {
        return (Reducer<Object>)RawMinReducer.min;
    }
    static final class RawMinReducer implements Reducer {
        static final RawMinReducer min = new RawMinReducer();
        public Object op(Object a, Object b) {
            return (a != null &&
                    (b == null ||
                     ((Comparable)a).compareTo((Comparable)b) <= 0)) ? a : b;
        }
    }


    /**
     * Returns a comparator for doubles relying on natural ordering.
     */
    public static DoubleComparator naturalDoubleComparator() {
        return NaturalDoubleComparator.comparator;
    }
    static final class NaturalDoubleComparator
        implements DoubleComparator {
        static final NaturalDoubleComparator comparator = new
            NaturalDoubleComparator();
        public int compare(double a, double b) {
            return Double.compare(a, b);
        }
    }

    /**
     * Returns a reducer returning the maximum of two double elements,
     * using natural comparator.
     */
    public static DoubleReducer naturalDoubleMaxReducer() {
        return NaturalDoubleMaxReducer.max;
    }

    static final class NaturalDoubleMaxReducer
        implements DoubleReducer {
        public static final NaturalDoubleMaxReducer max =
            new NaturalDoubleMaxReducer();
        public double op(double a, double b) { return Math.max(a, b); }
    }

    /**
     * Returns a reducer returning the minimum of two double elements,
     * using natural comparator.
     */
    public static DoubleReducer naturalDoubleMinReducer() {
        return NaturalDoubleMinReducer.min;
    }
    static final class NaturalDoubleMinReducer
        implements DoubleReducer {
        public static final NaturalDoubleMinReducer min =
            new NaturalDoubleMinReducer();
        public double op(double a, double b) { return Math.min(a, b); }
    }

    /**
     * Returns a reducer returning the maximum of two double elements,
     * using the given comparator.
     */
    public static DoubleReducer doubleMaxReducer
        (final DoubleComparator comparator) {
        return new DoubleReducer() {
                public double op(double a, double b) {
                    return (comparator.compare(a, b) >= 0) ? a : b;
                }
            };
    }

    /**
     * Returns a reducer returning the minimum of two double elements,
     * using the given comparator.
     */
    public static DoubleReducer doubleMinReducer
        (final DoubleComparator comparator) {
        return new DoubleReducer() {
                public double op(double a, double b) {
                    return (comparator.compare(a, b) <= 0) ? a : b;
                }
            };
    }

    /**
     * Returns a comparator for longs relying on natural ordering.
     */
    public static LongComparator naturalLongComparator() {
        return NaturalLongComparator.comparator;
    }
    static final class NaturalLongComparator
        implements LongComparator {
        static final NaturalLongComparator comparator = new
            NaturalLongComparator();
        public int compare(long a, long b) {
            return (a < b) ? -1 : ((a > b) ? 1 : 0);
        }
    }

    /**
     * Returns a reducer returning the maximum of two long elements,
     * using natural comparator.
     */
    public static LongReducer naturalLongMaxReducer() {
        return NaturalLongMaxReducer.max;
    }

    static final class NaturalLongMaxReducer
        implements LongReducer {
        public static final NaturalLongMaxReducer max =
            new NaturalLongMaxReducer();
        public long op(long a, long b) { return (a >= b) ? a : b; }
    }

    /**
     * A reducer returning the minimum of two long elements,
     * using natural comparator.
     */
    public static LongReducer naturalLongMinReducer() {
        return NaturalLongMinReducer.min;
    }
    static final class NaturalLongMinReducer
        implements LongReducer {
        public static final NaturalLongMinReducer min =
            new NaturalLongMinReducer();
        public long op(long a, long b) { return (a <= b) ? a : b; }
    }

    /**
     * Returns a reducer returning the maximum of two long elements,
     * using the given comparator.
     */
    public static LongReducer longMaxReducer
        (final LongComparator comparator) {
        return new LongReducer() {
                public long op(long a, long b) {
                    return (comparator.compare(a, b) >= 0) ? a : b;
                }
            };
    }

    /**
     * Returns a reducer returning the minimum of two long elements,
     * using the given comparator.
     */
    public static LongReducer longMinReducer
        (final LongComparator comparator) {
        return new LongReducer() {
                public long op(long a, long b) {
                    return (comparator.compare(a, b) <= 0) ? a : b;
                }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T,U,V> Op<T,V> compoundOp
        (final Op<? super T, ? extends U> first,
         final Op<? super U, ? extends V> second) {
        return new Op<T,V>() {
            public final V op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T,V> Op<T,V> compoundOp
        (final ObjectToDouble<? super T> first,
         final DoubleToObject<? extends V> second) {
        return new Op<T,V>() {
            public final V op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T,V> Op<T,V> compoundOp
        (final ObjectToLong<? super T> first,
         final LongToObject<? extends V> second) {
        return new Op<T,V>() {
            public final V op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T,V> DoubleToObject<V> compoundOp
        (final DoubleToObject<? extends T> first,
         final Op<? super T,? extends V> second) {
        return new DoubleToObject<V>() {
            public final V op(double t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T,V> LongToObject<V> compoundOp
        (final LongToObject<? extends T> first,
         final Op<? super T,? extends V> second) {
        return new LongToObject<V>() {
            public final V op(long t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T,U> ObjectToDouble<T> compoundOp
        (final Op<? super T, ? extends U> first,
         final ObjectToDouble<? super U> second) {
        return new ObjectToDouble<T>() {
            public final double op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T,U> ObjectToLong<T> compoundOp
        (final Op<? super T, ? extends U> first,
         final ObjectToLong<? super U> second) {
        return new ObjectToLong<T>() {
            public final long op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> ObjectToDouble<T> compoundOp
        (final ObjectToDouble<? super T> first,
         final DoubleOp second) {
        return new ObjectToDouble<T>() {
            public final double op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> ObjectToLong<T> compoundOp
        (final ObjectToDouble<? super T> first,
         final DoubleToLong second) {
        return new ObjectToLong<T>() {
            public final long op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> ObjectToLong<T> compoundOp
        (final ObjectToLong<? super T> first,
         final LongOp second) {
        return new ObjectToLong<T>() {
            public final long op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> ObjectToDouble<T> compoundOp
        (final ObjectToLong<? super T> first,
         final LongToDouble second) {
        return new ObjectToDouble<T>() {
            public final double op(T t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static DoubleOp compoundOp
        (final DoubleOp first,
         final DoubleOp second) {
        return new DoubleOp() {
                public final double op(double t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static DoubleToLong compoundOp
        (final DoubleOp first,
         final DoubleToLong second) {
        return new DoubleToLong() {
                public final long op(double t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static DoubleToLong compoundOp
        (final DoubleToLong first,
         final LongOp second) {
        return new DoubleToLong() {
                public final long op(double t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> DoubleToObject<T> compoundOp
        (final DoubleToLong first,
         final LongToObject<? extends T> second) {
        return new DoubleToObject<T>() {
            public final T op(double t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> LongToObject<T> compoundOp
        (final LongToDouble first,
         final DoubleToObject<? extends T> second) {
        return new LongToObject<T>() {
            public final T op(long t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static LongToDouble compoundOp
        (final LongOp first,
         final LongToDouble second) {
        return new LongToDouble() {
                public final double op(long t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static LongToDouble compoundOp
        (final LongToDouble first,
         final DoubleOp second) {
        return new LongToDouble() {
                public final double op(long t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> DoubleToObject<T> compoundOp
        (final DoubleOp first,
         final DoubleToObject<? extends T> second) {
        return new DoubleToObject<T>() {
            public final T op(double t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> LongToObject<T> compoundOp
        (final LongOp first,
         final LongToObject<? extends T> second) {
        return new LongToObject<T>() {
            public final T op(long t) { return second.op(first.op(t)); }
        };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> DoubleOp compoundOp
        (final DoubleToObject<? extends T> first,
         final ObjectToDouble<? super T> second) {
        return new DoubleOp() {
                public final double op(double t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> LongToDouble compoundOp
        (final LongToObject<? extends T> first,
         final ObjectToDouble<? super T> second) {
        return new LongToDouble() {
                public final double op(long t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> DoubleToLong compoundOp
        (final DoubleToObject<? extends T> first,
         final ObjectToLong<? super T> second) {
        return new DoubleToLong() {
                public final long op(double t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static <T> LongOp compoundOp
        (final LongToObject<? extends T> first,
         final ObjectToLong<? super T> second) {
        return new LongOp() {
                public final long op(long t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static LongOp compoundOp
        (final LongOp first,
         final LongOp second) {
        return new LongOp() {
                public final long op(long t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static DoubleOp compoundOp
        (final DoubleToLong first,
         final LongToDouble second) {
        return new DoubleOp() {
                public final double op(double t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a composite mapper that applies a second mapper to the results
     * of applying the first one.
     */
    public static LongOp compoundOp
        (final LongToDouble first,
         final DoubleToLong second) {
        return new LongOp() {
                public final long op(long t) { return second.op(first.op(t)); }
            };
    }

    /**
     * Returns a predicate evaluating to the negation of its contained predicate.
     */
    public static <T> Predicate<T> notPredicate
        (final Predicate<T> pred) {
        return new Predicate<T>() {
            public final boolean op(T x) { return !pred.op(x); }
        };
    }

    /**
     * Returns a predicate evaluating to the negation of its contained predicate.
     */
    public static DoublePredicate notPredicate
        (final DoublePredicate pred) {
        return new DoublePredicate() {
                public final boolean op(double x) { return !pred.op(x); }
            };
    }

    /**
     * Returns a predicate evaluating to the negation of its contained predicate.
     */
    public static LongPredicate notPredicate
        (final LongPredicate pred) {
        return new LongPredicate() {
                public final boolean op(long x) { return !pred.op(x); }
            };
    }

    /**
     * Returns a predicate evaluating to the conjunction of its contained predicates.
     */
    public static <S, T extends S> Predicate<T> andPredicate
                                (final Predicate<S> first,
                                 final Predicate<? super T> second) {
        return new Predicate<T>() {
            public final boolean op(T x) {
                return first.op(x) && second.op(x);
            }
        };
    }

    /**
     * Returns a predicate evaluating to the disjunction of its contained predicates.
     */
    public static <S, T extends S> Predicate<T> orPredicate
                                (final Predicate<S> first,
                                 final Predicate<? super T> second) {
        return new Predicate<T>() {
            public final boolean op(T x) {
                return first.op(x) || second.op(x);
            }
        };
    }

    /**
     * Returns a predicate evaluating to the conjunction of its contained predicates.
     */
    public static DoublePredicate andPredicate
        (final DoublePredicate first,
         final DoublePredicate second) {
        return new DoublePredicate() {
                public final boolean op(double x) {
                    return first.op(x) && second.op(x);
                }
            };
    }

    /**
     * Returns a predicate evaluating to the disjunction of its contained predicates.
     */
    public static DoublePredicate orPredicate
        (final DoublePredicate first,
         final DoublePredicate second) {
        return new DoublePredicate() {
                public final boolean op(double x) {
                    return first.op(x) || second.op(x);
                }
            };
    }


    /**
     * Returns a predicate evaluating to the conjunction of its contained predicates.
     */
    public static LongPredicate andPredicate
        (final LongPredicate first,
         final LongPredicate second) {
        return new LongPredicate() {
                public final boolean op(long x) {
                    return first.op(x) && second.op(x);
                }
            };
    }

    /**
     * Returns a predicate evaluating to the disjunction of its contained predicates.
     */
    public static LongPredicate orPredicate
        (final LongPredicate first,
         final LongPredicate second) {
        return new LongPredicate() {
                public final boolean op(long x) {
                    return first.op(x) || second.op(x);
                }
            };
    }

    /**
     * Returns a predicate evaluating to true if its argument is non-null.
     */
    public static Predicate<Object> isNonNullPredicate() {
        return IsNonNullPredicate.predicate;
    }
    static final class IsNonNullPredicate implements Predicate<Object> {
        static final IsNonNullPredicate predicate =
            new IsNonNullPredicate();
        public final boolean op(Object x) {
            return x != null;
        }
    }

    /**
     * Returns a predicate evaluating to true if its argument is null.
     */
    public static Predicate<Object> isNullPredicate() {
        return IsNullPredicate.predicate;
    }
    static final class IsNullPredicate implements Predicate<Object> {
        static final IsNullPredicate predicate =
            new IsNullPredicate();
        public final boolean op(Object x) {
            return x != null;
        }
    }

    /**
     * Returns a predicate evaluating to true if its argument is an instance
     * of (see {@link Class#isInstance} the given type (class).
     */
    public static Predicate<Object> instanceofPredicate(final Class type) {
        return new Predicate<Object>() {
            public final boolean op(Object x) {
                return type.isInstance(x);
            }
        };
    }

    /**
     * Returns a predicate evaluating to true if its argument is assignable
     * from (see {@link Class#isAssignableFrom} the given type (class).
     */
    public static Predicate<Object> isAssignablePredicate(final Class type) {
        return new Predicate<Object>() {
            public final boolean op(Object x) {
                return type.isAssignableFrom(x.getClass());
            }
        };
    }

    /**
     * Returns a reducer that adds two double elements.
     */
    public static DoubleReducer doubleAdder() { return DoubleAdder.adder; }
    static final class DoubleAdder implements DoubleReducer {
        static final DoubleAdder adder = new DoubleAdder();
        public double op(double a, double b) { return a + b; }
    }

    /**
     * Returns a reducer that adds two long elements.
     */
    public static LongReducer longAdder() { return LongAdder.adder; }
    static final class LongAdder implements LongReducer {
        static final LongAdder adder = new LongAdder();
        public long op(long a, long b) { return a + b; }
    }

    /**
     * Returns a reducer that adds two int elements.
     */
    public static IntReducer intAdder() { return IntAdder.adder; }
    static final class IntAdder implements IntReducer {
        static final IntAdder adder = new IntAdder();
        public int op(int a, int b) { return a + b; }
    }

    /**
     * Returns a generator producing uniform random values between
     * zero and one, with the same properties as {@link
     * java.util.Random#nextDouble}.
     */
    public static DoubleGenerator doubleRandom() {
        return DoubleRandomGenerator.generator;
    }
    static final class DoubleRandomGenerator implements DoubleGenerator {
        static final DoubleRandomGenerator generator =
            new DoubleRandomGenerator();
        public double op() {
            return ThreadLocalRandom.current().nextDouble();
        }
    }

    /**
     * Returns a generator producing uniform random values between
     * zero and the given bound, with the same properties as {@link
     * java.util.Random#nextDouble}.
     * @param bound the upper bound (exclusive) of opd values
     */
    public static DoubleGenerator doubleRandom(double bound) {
        return new DoubleBoundedRandomGenerator(bound);
    }
    static final class DoubleBoundedRandomGenerator implements DoubleGenerator {
        final double bound;
        DoubleBoundedRandomGenerator(double bound) { this.bound = bound; }
        public double op() {
            return ThreadLocalRandom.current().nextDouble() * bound;
        }
    }

    /**
     * Returns a generator producing uniform random values between the
     * given least value (inclusive) and bound (exclusive).
     * @param least the least value returned
     * @param bound the upper bound (exclusive) of opd values
     */
    public static DoubleGenerator doubleRandom(double least, double bound) {
        return new DoubleIntervalRandomGenerator(least, bound);
    }
    static final class DoubleIntervalRandomGenerator implements DoubleGenerator {
        final double least;
        final double range;
        DoubleIntervalRandomGenerator(double least, double bound) {
            this.least = least; this.range = bound - least;
        }
        public double op() {
            return ThreadLocalRandom.current().nextDouble() * range + least;
        }
    }

    /**
     * Returns a generator producing uniform random values with the
     * same properties as {@link java.util.Random#nextLong}.
     */
    public static LongGenerator longRandom() {
        return LongRandomGenerator.generator;
    }
    static final class LongRandomGenerator implements LongGenerator {
        static final LongRandomGenerator generator =
            new LongRandomGenerator();
        public long op() {
            return ThreadLocalRandom.current().nextLong();
        }
    }

    /**
     * Returns a generator producing uniform random values with the
     * same properties as {@link java.util.Random#nextInt(int)}.
     * @param bound the upper bound (exclusive) of opd values
     */
    public static LongGenerator longRandom(long bound) {
        if (bound <= 0)
            throw new IllegalArgumentException();
        return new LongBoundedRandomGenerator(bound);
    }
    static final class LongBoundedRandomGenerator implements LongGenerator {
        final long bound;
        LongBoundedRandomGenerator(long bound) { this.bound = bound; }
        public long op() {
            return ThreadLocalRandom.current().nextLong(bound);
        }
    }

    /**
     * Returns a generator producing uniform random values between the
     * given least value (inclusive) and bound (exclusive).
     * @param least the least value returned
     * @param bound the upper bound (exclusive) of opd values
     */
    public static LongGenerator longRandom(long least, long bound) {
        if (least >= bound)
            throw new IllegalArgumentException();
        return new LongIntervalRandomGenerator(least, bound);
    }
    static final class LongIntervalRandomGenerator implements LongGenerator {
        final long least;
        final long range;
        LongIntervalRandomGenerator(long least, long bound) {
            this.least = least; this.range = bound - least;
        }
        public long op() {
            return ThreadLocalRandom.current().nextLong(range) + least;
        }
    }

    /**
     * Returns a generator producing uniform random values with the
     * same properties as {@link java.util.Random#nextInt}.
     */
    public static IntGenerator intRandom() {
        return IntRandomGenerator.generator;
    }
    static final class IntRandomGenerator implements IntGenerator {
        static final IntRandomGenerator generator =
            new IntRandomGenerator();
        public int op() {
            return ThreadLocalRandom.current().nextInt();
        }
    }

    /**
     * Returns a generator producing uniform random values with the
     * same properties as {@link java.util.Random#nextInt(int)}.
     * @param bound the upper bound (exclusive) of opd values
     */
    public static IntGenerator intRandom(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException();
        return new IntBoundedRandomGenerator(bound);
    }
    static final class IntBoundedRandomGenerator implements IntGenerator {
        final int bound;
        IntBoundedRandomGenerator(int bound) { this.bound = bound; }
        public int op() {
            return ThreadLocalRandom.current().nextInt(bound);
        }
    }

    /**
     * Returns a generator producing uniform random values between the
     * given least value (inclusive) and bound (exclusive).
     * @param least the least value returned
     * @param bound the upper bound (exclusive) of opd values
     */
    public static IntGenerator intRandom(int least, int bound) {
        if (least >= bound)
            throw new IllegalArgumentException();
        return new IntIntervalRandomGenerator(least, bound);
    }
    static final class IntIntervalRandomGenerator implements IntGenerator {
        final int least;
        final int range;
        IntIntervalRandomGenerator(int least, int bound) {
            this.least = least; this.range = bound - least;
        }
        public int op() {
            return ThreadLocalRandom.current().nextInt(range) + least;
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code equals} the second.
     */
    public static BinaryPredicate<Object, Object> equalityPredicate() {
        return EqualityPredicate.predicate;
    }
    static final class EqualityPredicate implements BinaryPredicate<Object, Object> {
        static final EqualityPredicate predicate =
            new EqualityPredicate();
        public final boolean op(Object x, Object y) {
            return x.equals(y);
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code ==} the second.
     */
    public static BinaryPredicate<Object, Object> identityPredicate() {
        return IdentityPredicate.predicate;
    }
    static final class IdentityPredicate implements BinaryPredicate<Object, Object> {
        static final IdentityPredicate predicate =
            new IdentityPredicate();
        public final boolean op(Object x, Object y) {
            return x == y;
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code ==} the second.
     */
    public static BinaryIntPredicate intEqualityPredicate() {
        return IntEqualityPredicate.predicate;
    }
    static final class IntEqualityPredicate implements BinaryIntPredicate {
        static final IntEqualityPredicate predicate =
            new IntEqualityPredicate();
        public final boolean op(int x, int y) {
            return x == y;
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code ==} the second.
     */
    public static BinaryLongPredicate longEqualityPredicate() {
        return LongEqualityPredicate.predicate;
    }
    static final class LongEqualityPredicate implements BinaryLongPredicate {
        static final LongEqualityPredicate predicate =
            new LongEqualityPredicate();
        public final boolean op(long x, long y) {
            return x == y;
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code ==} the second.
     */
    public static BinaryDoublePredicate doubleEqualityPredicate() {
        return DoubleEqualityPredicate.predicate;
    }
    static final class DoubleEqualityPredicate implements BinaryDoublePredicate {
        static final DoubleEqualityPredicate predicate =
            new DoubleEqualityPredicate();
        public final boolean op(double x, double y) {
            return x == y;
        }
    }


    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code !equals} the second.
     */
    public static BinaryPredicate<Object, Object> inequalityPredicate() {
        return InequalityPredicate.predicate;
    }
    static final class InequalityPredicate implements BinaryPredicate<Object, Object> {
        static final InequalityPredicate predicate =
            new InequalityPredicate();
        public final boolean op(Object x, Object y) {
            return !x.equals(y);
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code !=} the second.
     */
    public static BinaryPredicate<Object, Object> nonidentityPredicate() {
        return NonidentityPredicate.predicate;
    }
    static final class NonidentityPredicate implements BinaryPredicate<Object, Object> {
        static final NonidentityPredicate predicate =
            new NonidentityPredicate();
        public final boolean op(Object x, Object y) {
            return x != y;
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code !=} the second.
     */
    public static BinaryIntPredicate intInequalityPredicate() {
        return IntInequalityPredicate.predicate;
    }
    static final class IntInequalityPredicate implements BinaryIntPredicate {
        static final IntInequalityPredicate predicate =
            new IntInequalityPredicate();
        public final boolean op(int x, int y) {
            return x != y;
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code ==} the second.
     */
    public static BinaryLongPredicate longInequalityPredicate() {
        return LongInequalityPredicate.predicate;
    }
    static final class LongInequalityPredicate implements BinaryLongPredicate {
        static final LongInequalityPredicate predicate =
            new LongInequalityPredicate();
        public final boolean op(long x, long y) {
            return x != y;
        }
    }

    /**
     * Returns a predicate evaluating to true if the
     * first argument {@code !=} the second.
     */
    public static BinaryDoublePredicate doubleInequalityPredicate() {
        return DoubleInequalityPredicate.predicate;
    }
    static final class DoubleInequalityPredicate implements BinaryDoublePredicate {
        static final DoubleInequalityPredicate predicate =
            new DoubleInequalityPredicate();
        public final boolean op(double x, double y) {
            return x != y;
        }
    }


}
