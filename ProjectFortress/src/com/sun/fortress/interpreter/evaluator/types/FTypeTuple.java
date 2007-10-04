/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.useful.BoundingMap;
import com.sun.fortress.useful.Factory1;
import com.sun.fortress.useful.ListComparer;
import com.sun.fortress.useful.Memo1C;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static com.sun.fortress.interpreter.evaluator.values.OverloadedFunction.exclDump;
import static com.sun.fortress.interpreter.evaluator.values.OverloadedFunction.exclDumpln;

// TODO need to memoize this to preserver type EQuality
public class FTypeTuple extends FType {

    List<FType> l;

    private static class Factory implements Factory1<List<FType>, FType> {

        public FType make(List<FType> part1) {
            if (part1.size() == 0) return FTypeVoid.ONLY;
            if (part1.size() == 1) return part1.get(0);
            return new FTypeTuple(part1);
        }

    }

    static Memo1C<List<FType>, FType> memo = null;

    public static void reset() {
        memo = new Memo1C<List<FType>, FType>( new Factory(), listComparer);
    }
    static public FType make(List<FType> l) {
        return l.size() == 0 ? FTypeVoid.ONLY : memo.make(l);
    }

    protected FTypeTuple(List<FType> l) {
        super("Tuple"); // TODO need a better name -- ought to do it lazily
        this.l = l;
    }

    public String toString() {
        return Useful.listInParens(l);
    }

    /**
     * Returns this subtypeof other.
     */
    public boolean subtypeOf(FType other) {
        if (commonSubtypeOf(other))
            return true;
        // TODO This is going to get hairy in a world of overloaded function
        // types and rest types.
        if (other instanceof FTypeTuple) {
            FTypeTuple that = (FTypeTuple) other;

            // For rest types, iterate to min-1, verify that
            // either tails are length 1 and equal, or else
            // that 'that' ends in rest T and the tail of this
            // is all subtypeof T.

            List<FType> thisl = this.l;
            List<FType> thatl = that.l;

            int min = thatl.size();
            if (min == 0) {
                // if other is empty, and this != other (above), then false.
                return false;
            }

            FType thatt = thatl.get(min - 1);

            // TODO lacking default parameters, a short list cannot
            // possibly match a long list. I.e., if "min" isn't
            // the minimum, the answer is false.

            if (thatt instanceof FTypeRest) {
                return subtypeOf(thisl, thatl, 0, min - 1)
                        && subtypeOf(thisl, ((FTypeRest) thatt).getType(),
                                min - 1, thisl.size());
            } else if (min != thisl.size()) {
                return false;
            } else {
                return subtypeOf(thisl, thatl, 0, min);
            }

        }
        return false;
    }

    /**
     * Returns true iff candidate is more specific than current.
     * Note that it is possible that neither one of a pair of types
     * is more specific than the other.
     *
     * This code was simplified and moved here in order to guarantee
     * that tuple subtype checking is consistent with most-specific-
     * overloading checks.
     */
    public static boolean  moreSpecificThan(List<FType> candidate, List<FType> current) {
        FType candType = make(candidate);
        FType currType = make(current);
        if (candType.subtypeOf(currType)) {
            if (currType.subtypeOf(candType)) {
                /* All other things being equal, choose the
                 * non-single-argument-without-type candidate if it is
                 * unique.  This catches the rather special case of
                 * omitted type information in
                 * tests/overloadTest3.fss. */
                return (currType==FTypeDynamic.ONLY);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Tests subtype-of for a pair of tuple sections. Returns true iff
     * thisl[start, end) subtypeof thatl[start, end).
     */
    private static boolean subtypeOf(List<FType> thisl, List<FType> thatl,
            int start, int end) {
        if (thisl.size() < end || thatl.size() < end)
            return false;
        for (int i = start; i < end; i++) {
            if (!thisl.get(i).subtypeOf(thatl.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Tests subtype-of for a tuple section against a type. Returns true iff
     * thisl[start, end) subtypeof thatt.
     */
    private static boolean subtypeOf(List<FType> thisl, FType thatt, int start,
            int end) {
        if (thisl.size() < end)
            return false;
        for (int i = start; i < end; i++) {
            if (!thisl.get(i).subtypeOf(thatt))
                return false;
        }
        return true;
    }

    public Set<FType> meet(FType other) {
        TreeSet<FType> result = new TreeSet<FType>();
        if (other instanceof FTypeDynamic || other instanceof FTypeTop) {
            result.add(this);
        } else if (other instanceof BottomType) {
            result.add(other);
        } else if (other instanceof FTypeTuple) {
            FTypeTuple ftt_other = (FTypeTuple) other;
            Set<List<FType>> slf = meet(l, ftt_other.l);
            if (slf.size() == 0)
                return Collections.<FType> emptySet();
            for (List<FType> lt : slf)
                result.add(FTypeTuple.make(lt));
        }
        return result;
    }

    public Set<FType> join(FType other) {
        TreeSet<FType> result = new TreeSet<FType>();
        if (other instanceof FTypeDynamic || other instanceof FTypeTop) {
            result.add(other);
        } else if (other instanceof BottomType) {
            result.add(this);
        } else if (other instanceof FTypeTuple) {
            FTypeTuple ftt_other = (FTypeTuple) other;
            Set<List<FType>> slf = join(l, ftt_other.l);
            if (slf.size() == 0)
                return Collections.<FType> emptySet();
            for (List<FType> lt : slf)
                result.add(FTypeTuple.make(lt));
        }
        return result;
    }

    public static Set<List<FType>> meet(List<FType> pl1, List<FType> pl2) {

        // TODO For efficiency, we can filter out combinations that are
        // guaranteed to fail.

        TreeSet<List<FType>> s = new TreeSet<List<FType>>();
        s.add(Collections.<FType> emptyList());

        return meet(s, pl1, pl2);
    }

    public static Set<List<FType>> join(List<FType> pl1, List<FType> pl2) {
        // Join is a lot more martian than meet.

        // If the two lists are clearly disjoint, then the join is just
        // the pair of lists.

        // If there is a rest-parameter, then it is messier.

        // The goal is to attempt to generate a singleton answer, because
        // in many cases within the Fortress type system that is the only
        // acceptable result.

        TreeSet<List<FType>> s = new TreeSet<List<FType>>();

        int s1 = pl1.size();
        int s2 = pl2.size();

        // TODO these cases make my brain hurt.

        if (s1 != s2)
            NI.nyi("JOIN of lists of types of unequal length");

        if (s1 > 0 && pl1.get(s1 - 1) instanceof FTypeRest)
            NI.nyi("JOIN of lists, first has REST parameter");

        if (s2 > 0 && pl2.get(s2 - 1) instanceof FTypeRest)
            NI.nyi("JOIN of lists, second has REST parameter");

        // TODO today, if we can arrive at a single answer, we declare victory,
        // otherwise punt.

        ArrayList<FType> a = new ArrayList<FType>();

        for (int i = 0; i < s1; i++) {
            FType t1 = pl1.get(i);
            FType t2 = pl2.get(i);
            Set<FType> m = t1.join(t2);
            if (m.size() != 1)
                return Collections.<List<FType>> emptySet();
            // m.iterator().next() gets the singleton out.
            a.add(m.iterator().next());
        }

        s.add(a);

        return s;
    }

    private static Set<List<FType>> meet(Set<List<FType>> s, List<FType> pl1,
            List<FType> pl2) {
        int s1 = pl1.size();
        int s2 = pl2.size();
        if (s1 == 0 && s2 == 0)
            return s;
        else if (s1 == 0) {
            if (s2 == 1 && pl2.get(0) instanceof FTypeRest)
                return s;
            else
                return Collections.<List<FType>> emptySet();
        } else if (s2 == 0) {
            if (s1 == 1 && pl1.get(0) instanceof FTypeRest)
                return s;
            else
                return Collections.<List<FType>> emptySet();
        } else {
            FType t1 = pl1.get(0);
            FType t2 = pl2.get(0);
            boolean r1 = t1 instanceof FTypeRest;
            boolean r2 = t2 instanceof FTypeRest;
            List<FType> sl1 = pl1;
            List<FType> sl2 = pl2;

            if (r1 && !r2) {
                if (s1 != 1)
                    bug("Varargs parameter not last parameter");
                sl2 = sl2.subList(1, s2);
            } else if (r2 && !r1) {
                if (s2 != 1)
                    bug("Varargs parameter not last parameter");
                sl1 = sl1.subList(1, s1);
            } else {
                // If tails have equal rest-ness, shorten them both.
                sl1 = sl1.subList(1, s1);
                sl2 = sl2.subList(1, s2);
            }

            return meet(Useful.setProduct(s, t1.meet(t2)), sl1, sl2);
        }
    }

    /**
     * Unify a tuple type with a list of Type.
     *  This gets used here and in FTypeArrow.
     */
    public boolean unifyTuple(BetterEnv env, Set<StaticParam> tp_set,
                              BoundingMap<String, FType, TypeLatticeOps> abm,
                              List<Type> vals, Option<VarargsType> varargs) {
        Iterator<FType> ftIterator = l.iterator();
        Iterator<Type> trIterator = vals.iterator();
        FType ft = null;
        Type tr = null;
        try {
            while (ftIterator.hasNext() && trIterator.hasNext()) {
                ft = ftIterator.next();
                tr = trIterator.next();
                ft.unify(env,tp_set,abm,tr);
            }
            while (varargs.isSome() && ftIterator.hasNext()) {
                ft = ftIterator.next();
                ft.unify(env, tp_set, abm, Option.unwrap(varargs));
            }
            while (ft instanceof FTypeRest && trIterator.hasNext()) {
                tr = trIterator.next();
                ft.unify(env,tp_set,abm,tr);
            }
        } catch (FortressError p) {
            return false;
        }
        return true;
    }

    /*
     * @see com.sun.fortress.interpreter.evaluator.types.FType#unifyNonVar(java.util.Set, com.sun.fortress.interpreter.useful.ABoundingMap,
     *      com.sun.fortress.interpreter.nodes.Type)
     */
    @Override
    protected boolean unifyNonVar(BetterEnv env, Set<StaticParam> tp_set,
            BoundingMap<String, FType, TypeLatticeOps> abm, Type val) {
        if (FType.DUMP_UNIFY)
            System.out.println("unify tuple "+this+" and "+val+", abm="+abm);
        if (!(val instanceof TupleType)) return false;
        TupleType tup = (TupleType) val;
        if (!(tup.getKeywords().isEmpty())) return false;
        return unifyTuple(env, tp_set, abm, tup.getElements(), tup.getVarargs());
    }

    @Override
    public boolean excludesOther(FType other) {
        exclDump(this,".excludesOther(",other,"):");

        if (this==other) {
            exclDumpln(" No.  Equal.");
            return false;
        }
        if (!(other instanceof FTypeTuple)) {
            exclDumpln(" Excludes.  Non-tuple.");
            return true;
        }

        /* Step 1: Check cached excludes before doing anything fancy. */
        if (getExcludes().contains(other)) {
            exclDumpln(" Cached.");
            return true;
        }

        /* Step 2: Extract lists of types, and swap if this is longer
         * just to keep the number of cases sane. */
        List<FType> otherTypes = ((FTypeTuple) other).l;
        int lSize = l.size();
        int otherSize = otherTypes.size();
        if (lSize > otherSize) {
            exclDumpln(" Swapping.");
            return other.excludesOther(this);
        }

        /* Step 3: Figure out what to do with last element of shorter
         * tuple.  Hope to fail fast by finding length mismatch w/o rest. */
        FType last = l.get(lSize-1);
        if (last instanceof FTypeRest) {
            last = ((FTypeRest)last).getType();
        } else if (otherSize > lSize) {
            if (otherSize == lSize+1 &&
                otherTypes.get(lSize) instanceof FTypeRest) {
                /* Ignore this final rest argument when doing matching. */
                lSize = lSize - 1;
            } else {
                exclDumpln(" Excludes. Length mismatch, no rest");
                this.addExclude(other);
                return true;
            }
        }

        /* Step 4: Check the tuple components for exclusion. */
        for (int i=0; i < otherSize; i++) {
            FType thisElt = (i < lSize-1) ? l.get(i) : last;
            FType otherElt = otherTypes.get(i);
            if (otherElt instanceof FTypeRest) {
                otherElt = ((FTypeRest)otherElt).getType();
            }
            if (thisElt.excludesOther(otherElt)) {
                exclDumpln(" Excludes type ",i);
                this.addExclude(other);
                return true;
            }
            exclDump(" ",i);
        }
        exclDumpln(" No exclusion.");
        return false;
    }

}
