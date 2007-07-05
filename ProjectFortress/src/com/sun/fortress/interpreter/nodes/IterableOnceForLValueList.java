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

package com.sun.fortress.interpreter.nodes;

import java.util.Iterator;
import java.util.List;

import com.sun.fortress.interpreter.useful.IterableOnce;


final public class IterableOnceForLValueList implements IterableOnce<String> {
    /**
     * i = -1 before iterator() is called. i in 0..lhs.size()-1 while iterating.
     * i = lhs.size() when done.
     */
    int i = -1;

    List<? extends LValue> lhs;

    IterableOnce<String> current;

    public IterableOnceForLValueList(List<? extends LValue> lhs) {
        this.lhs = lhs;
    }

    public Iterator<String> iterator() {
        if (i >= 0) {
            throw new IllegalStateException("One-shot iterable");
        }
        nextCurrent();
        return this;
    }

    void nextCurrent() {
        if (++i < lhs.size()) {
            current = lhs.get(i).stringNames();
        }
    }

    public boolean hasNext() {
        return i < lhs.size() && current.hasNext();
    }

    public String next() {
        String s = current.next();
        if (!current.hasNext()) {
            nextCurrent();
        }
        return s;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
