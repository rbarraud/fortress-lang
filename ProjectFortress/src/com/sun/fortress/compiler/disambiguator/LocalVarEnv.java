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

package com.sun.fortress.compiler.disambiguator;

import java.util.Set;
import java.util.Collections;

import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;

public class LocalVarEnv extends DelegatingNameEnv {
    private Set<IdName> _vars;
    
    public LocalVarEnv(NameEnv parent, Set<IdName> vars) {
        super(parent);
        _vars = vars;
    }
    
    @Override public Set<QualifiedIdName> explicitVariableNames(IdName name) {
        if (_vars.contains(name)) {
            return Collections.singleton(NodeFactory.makeQualifiedIdName(name));
        }
        else { return super.explicitVariableNames(name); }
    }
    
}