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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildNativeEnvironment;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.nodes.GenericWithParams;

public class GenericNativeConstructor extends GenericConstructor {

    private String name;

    public GenericNativeConstructor(Environment env,
            GenericWithParams odefOrDecl, String name) {
        super(env, odefOrDecl);
        this.name = name;
    }

    protected Constructor makeAConstructor(BetterEnv clenv, FTypeObject objectType, List<Parameter> objectParams) {
        Constructor cl = BuildNativeEnvironment.nativeConstructor(clenv, objectType, odefOrDecl, name);
        cl.setParams(objectParams);
        cl.finishInitializing();
        return cl;
    }

}