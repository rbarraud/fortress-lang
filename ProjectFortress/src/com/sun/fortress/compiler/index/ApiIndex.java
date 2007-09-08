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

package com.sun.fortress.compiler.index;

import java.util.Map;
import edu.rice.cs.plt.collect.Relation;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.FnName;

public class ApiIndex extends CompilationUnitIndex {
    
    public ApiIndex(Api ast,
                    Map<IdName, Variable> variables,
                    Relation<FnName, Function> functions,
                    Map<IdName, TypeConsIndex> typeConses) {
        super(ast, variables, functions, typeConses);
    }
    
}
