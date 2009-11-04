/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.SelfType;
import com.sun.fortress.nodes.Id;
import edu.rice.cs.plt.tuple.Option;

/**
 * A Functional index that occurs in a trait. We need to be able to get the
 * static parameters on the declaring trait and get the type of `self`.
 */
public interface HasSelfType {
    public Id declaringTrait();
    public Option<SelfType> selfType();
    public int selfPosition();
}
