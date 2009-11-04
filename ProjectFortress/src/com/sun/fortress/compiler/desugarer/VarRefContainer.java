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

package com.sun.fortress.compiler.desugarer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;

/** Generate object to hold hoisted mutable variables that are shared by
 *  an object expression and its enclosing context.
 */
public class VarRefContainer {
    private VarRef origVar;
    private ASTNode origDeclNode;
    private String uniqueSuffix;

    private static final String CONTAINER_DECL_PREFIX = "Box";
    private static final String CONTAINER_FIELD_PREFIX = "box";

    public VarRefContainer(VarRef origVar,
                           ASTNode origDeclNode,
                           String uniqueSuffix) {
        this.origVar = origVar;
        this.origDeclNode = origDeclNode;
        this.uniqueSuffix = uniqueSuffix;
    }

    public Id containerDeclId() {
        String name = ObjectExpressionVisitor.MANGLE_CHAR +
            CONTAINER_DECL_PREFIX + "_" + uniqueSuffix +
            "_" + origVar.getVarId().getText();
        return NodeFactory.makeId( NodeUtil.getSpan(origDeclNode), name );
    }

    public Id containerVarId() {
        String name = ObjectExpressionVisitor.MANGLE_CHAR +
            CONTAINER_FIELD_PREFIX + "_" + uniqueSuffix +
            "_" + origVar.getVarId().getText();
        return NodeFactory.makeId( NodeUtil.getSpan(origDeclNode), name );
    }

    public ASTNode origDeclNode() {
        return origDeclNode;
    }

    public Option<Type> containerType() {
        Option<Type> containerType = Option.<Type>some(
                         NodeFactory.makeTraitType(containerDeclId()) );
        return containerType;
    }

    public ObjectDecl containerDecl() {
        List<Param> params = new LinkedList<Param>();
        Param param = makeVarParamFromVarRef( origVar,
                                              NodeUtil.getSpan(origDeclNode),
                                              NodeUtil.getExprType(origVar) );
        params.add(param);

        ObjectDecl container =
            NodeFactory.makeObjectDecl( NodeUtil.getSpan(origDeclNode), containerDeclId(),
                                        Option.<List<Param>>some(params), Option.<SelfType>none() );

        return container;
    }

    public Param containerTypeParam() {
        return NodeFactory.makeParam( NodeUtil.getSpan(origDeclNode),
                                      containerVarId(), containerType() );
    }

    public VarDecl containerField() {
        List<LValue> lhs = new LinkedList<LValue>();
        // set the field to be immutable
        lhs.add( NodeFactory.makeLValue(NodeUtil.getSpan(origDeclNode), containerVarId(),
                                        containerType()) );
        VarDecl field = NodeFactory.makeVarDecl( NodeUtil.getSpan(origDeclNode),
                                     lhs, Option.<Expr>some(makeCallToContainerObj()) );

        return field;
    }

    public VarRef containerVarRef(Span varRefSpan) {
        return ExprFactory.makeVarRef( varRefSpan,
                                       containerType(),
                                       containerVarId() );
    }

    public FieldRef containerFieldRef(Span varRefSpan) {
        return ExprFactory.makeFieldRef( varRefSpan,
                                         this.containerVarRef(varRefSpan),
                                         origVar.getVarId() );
    }

    public LocalVarDecl containerLocalVarDecl(List<Expr> bodyExprs) {
        List<LValue> lhs = new LinkedList<LValue>();
        // set the field to be immutable
        lhs.add( NodeFactory.makeLValue(NodeUtil.getSpan(origDeclNode), containerVarId(),
                                        containerType()) );
        LocalVarDecl ret = ExprFactory.makeLocalVarDecl( NodeUtil.getSpan(origDeclNode),
                            lhs, makeCallToContainerObj(), bodyExprs );

        return ret;
    }

    private Param makeVarParamFromVarRef(VarRef var,
                                         Span paramSpan,
                                               Option<Type> typeOp) {
        Param param = NodeFactory.makeParam(paramSpan, Modifiers.Settable, var.getVarId(), typeOp);
        return param;
    }

    private Expr makeCallToContainerObj() {
        List<IdOrOp> fns = new LinkedList<IdOrOp>();
        Span origSpan = NodeUtil.getSpan(origDeclNode);

        fns.add( containerDeclId() );
        FnRef fnRefToDecl = ExprFactory.makeFnRef( origSpan, false,
                                                   containerDeclId(), fns, Collections.<StaticArg>emptyList() );

        return( ExprFactory.make_RewriteFnApp(fnRefToDecl, origVar) );
    }

}
