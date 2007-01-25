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
import com.sun.fortress.interpreter.evaluator.Evaluator;
import com.sun.fortress.interpreter.useful.HasAt;


public class TraitMethodInstance extends TraitMethod implements MethodInstance {

    GenericMethod generator;

    public TraitMethodInstance(PartiallyDefinedMethod method, BetterEnv env,
            String selfName, GenericMethod generator) {
        super(method, env, selfName);
        this.generator = generator;
    }

    public GenericMethod getGenerator() {
        return generator;
    }

    public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc) {
        args = conditionallyUnwrapTupledArgs(args);
        // TraitMethods do not get their environment from the object.
        Evaluator eval = new Evaluator(buildEnvFromEnvAndParams(getWithin(), args, loc));
        eval.e.putValue(selfName(), selfValue);
        return eval.eval(getBody());
     }

}
