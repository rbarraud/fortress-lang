/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler.typechecker;

import junit.framework.TestCase;
import java.util.*;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.parser_util.FortressUtil;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.*;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;
import static com.sun.fortress.compiler.Types.*;
import static com.sun.fortress.compiler.typechecker.TypeCheckerTestCase.*;

public class SubtypeCheckerJUTest extends TypeCheckerTestCase {

    Type alpha = NodeFactory.makeVarType("ALPHA");
    Type beta  = NodeFactory.makeVarType("BETA");

    // trait E[\T extends Number, bool b, int i, nat n, opr ODOT\] extends B end
    ProperTraitIndex traitE =
        makeTrait("E",
                  makeSparams(NodeFactory.makeTypeParam("T", "Number"),
                              NodeFactory.makeBoolParam("b"),
                              NodeFactory.makeIntParam("i"),
                              NodeFactory.makeNatParam("n"),
                              NodeFactory.makeOpParam("ODOT")),
                  "D");

    TraitType instE =
        NodeFactory.makeTraitType("E",
                                  makeSargs(NodeFactory.makeTypeArg("ZZ32"),
                                            NodeFactory.makeBoolArg("trueV"),
                                            NodeFactory.makeIntArg("two"),
                                            NodeFactory.makeIntArg("three"),
                                            NodeFactory.makeOpArg("+")));

    TraitType instEp =
        NodeFactory.makeTraitType("E",
                                  makeSargs(NodeFactory.makeTypeArg("ZZ32"),
                                            NodeFactory.makeBoolArg("falseV"),
                                            NodeFactory.makeIntArg("two"),
                                            NodeFactory.makeIntArg("two"),
                                            NodeFactory.makeOpArg("+")));

    SubtypeChecker checker = makeAnalyzer(trait("Number"),
                                          trait("ZZ32", "Number"),
                                          trait("RR64", "Number"),
                                          trait("A"),
                                          trait("B", "A"),
                                          trait("C", "B"),
                                          trait("D", "B"),
                                          traitE);

    public void testAnySubtyping() {
        assertEquals(TRUE, sub(checker, ANY,      ANY));
        assertEquals(TRUE, sub(checker, BOTTOM,   ANY));
        assertEquals(TRUE, sub(checker, VOID,     ANY));
        assertEquals(TRUE, sub(checker, "B",      ANY));
        assertEquals(TRUE, sub(checker, alpha,    ANY));
        assertEquals(TRUE, sub(checker, "A->B",   ANY));
        assertEquals(TRUE, sub(checker, "(A, B)", ANY));
        assertEquals(TRUE, sub(checker, instE,    ANY));

        assertEquals(FALSE, sub(checker, ANY, BOTTOM));
        assertEquals(FALSE, sub(checker, ANY, VOID));
        assertEquals(FALSE, sub(checker, ANY, "B"));
        assertEquals(FALSE, sub(checker, ANY, alpha));
        assertEquals(FALSE, sub(checker, ANY, "A->B"));
        assertEquals(FALSE, sub(checker, ANY, "(A, B)"));
        assertEquals(FALSE, sub(checker, ANY, instE));
    }

    public void testBottomSubtyping() {
        assertEquals(TRUE, sub(checker, BOTTOM, ANY));
        assertEquals(TRUE, sub(checker, BOTTOM, BOTTOM));
        assertEquals(TRUE, sub(checker, BOTTOM, VOID));
        assertEquals(TRUE, sub(checker, BOTTOM, "B"));
        assertEquals(TRUE, sub(checker, BOTTOM, alpha));
        assertEquals(TRUE, sub(checker, BOTTOM, "A->B"));
        assertEquals(TRUE, sub(checker, BOTTOM, "(A, B)"));
        assertEquals(TRUE, sub(checker, BOTTOM, instE));

        assertEquals(FALSE, sub(checker, ANY,      BOTTOM));
        assertEquals(FALSE, sub(checker, VOID,     BOTTOM));
        assertEquals(FALSE, sub(checker, "B",      BOTTOM));
        assertEquals(FALSE, sub(checker, alpha,    BOTTOM));
        assertEquals(FALSE, sub(checker, "A->B",   BOTTOM));
        assertEquals(FALSE, sub(checker, "(A, B)", BOTTOM));
        assertEquals(FALSE, sub(checker, instE,    BOTTOM));
    }

    public void testReflexiveSubtyping() {
        assertEquals(TRUE, sub(checker, ANY, ANY));
        assertEquals(TRUE, sub(checker, BOTTOM, BOTTOM));
        assertEquals(TRUE, sub(checker, VOID, VOID));
        assertEquals(TRUE, sub(checker, "B", "B"));
        assertEquals(TRUE, sub(checker, alpha, alpha));
        assertEquals(TRUE, sub(checker, "A->B", "A->B"));
        assertEquals(TRUE, sub(checker, "(A, B)", "(A, B)"));
        assertEquals(TRUE, sub(checker, instE, instE));
    }

    public void testVarSubtyping() {
        assertEquals(TRUE,  sub(checker, alpha, "Number"));

        assertEquals(FALSE, sub(checker, alpha, BOTTOM));
        assertEquals(FALSE, sub(checker, alpha, VOID));
        assertEquals(FALSE, sub(checker, alpha, "B"));
        assertEquals(FALSE, sub(checker, alpha, beta));
        assertEquals(FALSE, sub(checker, alpha, "A->B"));
        assertEquals(FALSE, sub(checker, alpha, "(A, B)"));
        assertEquals(FALSE, sub(checker, alpha, instE));
    }

    public void testBasicTraitSubtyping() {
        assertEquals(TRUE,  sub(checker, "A", "A"));
        assertEquals(FALSE, sub(checker, "A", "B"));
        assertEquals(FALSE, sub(checker, "A", "C"));
        assertEquals(FALSE, sub(checker, "A", "D"));

        assertEquals(TRUE,  sub(checker, "B", "A"));
        assertEquals(TRUE,  sub(checker, "B", "B"));
        assertEquals(FALSE, sub(checker, "B", "C"));
        assertEquals(FALSE, sub(checker, "B", "D"));

        assertEquals(TRUE,  sub(checker, "C", "A"));
        assertEquals(TRUE,  sub(checker, "C", "B"));
        assertEquals(TRUE,  sub(checker, "C", "C"));
        assertEquals(FALSE, sub(checker, "C", "D"));

        assertEquals(TRUE,  sub(checker, "D", "A"));
        assertEquals(TRUE,  sub(checker, "D", "B"));
        assertEquals(FALSE, sub(checker, "D", "C"));
        assertEquals(TRUE,  sub(checker, "D", "D"));
    }

    public void testArrowSubtyping() {
        assertEquals(TRUE, sub(checker, "A->A", "A->A"));
        assertEquals(TRUE, sub(checker, "A->B", "A->A"));
        assertEquals(TRUE, sub(checker, "A->C", "A->A"));
        assertEquals(TRUE, sub(checker, "A->D", "A->A"));

        assertEquals(TRUE,  sub(checker, "A->C", "C->C"));
        assertEquals(TRUE,  sub(checker, "B->C", "C->C"));
        assertEquals(TRUE,  sub(checker, "C->C", "C->C"));
        assertEquals(FALSE, sub(checker, "D->C", "C->C"));

        assertEquals(FALSE, sub(checker, "C->A", "A->C"));
        assertEquals(TRUE,  sub(checker, "A->C", "C->A"));

        assertEquals(FALSE, sub(checker, ANY,      "A->B"));
        assertEquals(FALSE, sub(checker, VOID,     "A->B"));
        assertEquals(FALSE, sub(checker, "B",      "A->B"));
        assertEquals(FALSE, sub(checker, alpha,    "A->B"));
        assertEquals(FALSE, sub(checker, "(A, B)", "A->B"));
        assertEquals(FALSE, sub(checker, instE,    "A->B"));
    }

    public void testVoidSubtyping() {
        assertEquals(FALSE, sub(checker, ANY,      VOID));
        assertEquals(FALSE, sub(checker, "A",      VOID));
        assertEquals(FALSE, sub(checker, alpha,    VOID));
        assertEquals(FALSE, sub(checker, "A->B",   VOID));
        assertEquals(FALSE, sub(checker, "(A, B)", VOID));
        assertEquals(FALSE, sub(checker, instE,    VOID));
    }

    public void testTupleSubtyping() {
        assertEquals(TRUE,  sub(checker, "(B, C)", "(A, B)"));
        assertEquals(FALSE, sub(checker, "(A, B)", "(B, C)"));
        assertEquals(FALSE, sub(checker, ANY,      "(A, B)"));
        assertEquals(FALSE, sub(checker, VOID,     "(A, B)"));
        assertEquals(FALSE, sub(checker, "B",      "(A, B)"));
        assertEquals(FALSE, sub(checker, alpha,    "(A, B)"));
        assertEquals(FALSE, sub(checker, "A->B",   "(A, B)"));
        assertEquals(FALSE, sub(checker, instE,    "(A, B)"));
    }

    public void testInstSubtyping() {
        assertEquals(TRUE,  sub(checker, instE,    "A"));
        assertEquals(TRUE,  sub(checker, instE,    "B"));
        assertEquals(TRUE,  sub(checker, instE,    "D"));
        assertEquals(FALSE, sub(checker, instE,    "C"));

        assertEquals(FALSE, sub(checker, instE,    instEp));
        assertEquals(FALSE, sub(checker, instEp,   instE));

        assertEquals(FALSE, sub(checker, ANY,      instE));
        assertEquals(FALSE, sub(checker, VOID,     instE));
        assertEquals(FALSE, sub(checker, "Number", instE));
        assertEquals(FALSE, sub(checker, alpha,    instE));
        assertEquals(FALSE, sub(checker, "A->B",   instE));
        assertEquals(FALSE, sub(checker, "(A, B)", instE));

        assertEquals(FALSE, sub(checker, instE, BOTTOM));
        assertEquals(FALSE, sub(checker, instE, VOID));
        assertEquals(FALSE, sub(checker, instE, "Number"));
        assertEquals(FALSE, sub(checker, instE, alpha));
        assertEquals(FALSE, sub(checker, instE, "A->B"));
        assertEquals(FALSE, sub(checker, instE, "(A, B)"));
    }

    private ExtentRange makeExtentRange(IntArg arg) {
        return new ExtentRange(arg.getSpan(), Option.<StaticArg>none(),
                               Option.<StaticArg>some(arg));
    }

    private Option<Indices> makeIndices(List<ExtentRange> ext) {
        return Option.some(new Indices(new Span(), ext));
    }

    public void testAbbreviatedSubtyping() {
        Span span = new Span();
        Type zz = parseType("ZZ32");
        TypeArg zzA = NodeFactory.makeTypeArg("ZZ32");
        IntArg zero = NodeFactory.makeIntArgVal("0");
        IntArg three = NodeFactory.makeIntArg("three");
        Id arrName1 = NodeFactory.makeId(span, "FortressLibrary", "Array1");
        TraitType arr1 = NodeFactory.makeTraitType(span, false,
                                                   arrName1, zzA,
                                                   zero, three);
        Id arrName2 = NodeFactory.makeId(span, "FortressLibrary", "Array2");
        TraitType arr2 = NodeFactory.makeTraitType(span, false,
                                                   arrName2, zzA,
                                                   zero, three,
                                                   zero, three);
        Id arrName3 = NodeFactory.makeId(span, "FortressLibrary", "Array3");
        TraitType arr3 = NodeFactory.makeTraitType(span, false,
                                                   arrName3, zzA,
                                                   zero, three,
                                                   zero, three,
                                                   zero, three);
        Id matName = NodeFactory.makeId(span, "FortressLibrary", "Matrix");
        TraitType mat  = NodeFactory.makeTraitType(span, false,
                                                   matName, zzA,
                                                   three, three);

        List<ExtentRange> ext1 = new ArrayList<ExtentRange>(1);
        ext1.add(makeExtentRange(three));
        Option<Indices> ind1 = makeIndices(ext1);

        List<ExtentRange> ext2 = new ArrayList<ExtentRange>(2);
        ext2.add(makeExtentRange(three));
        ext2.add(makeExtentRange(three));
        Option<Indices> ind2 = makeIndices(ext2);

        List<ExtentRange> ext3 = new ArrayList<ExtentRange>(3);
        ext3.add(makeExtentRange(three));
        ext3.add(makeExtentRange(three));
        ext3.add(makeExtentRange(three));
        Option<Indices> ind3 = makeIndices(ext3);

        ExtentRange dim1 = makeExtentRange(three);
        List<ExtentRange> dim2 = new ArrayList<ExtentRange>(1);
        dim2.add(makeExtentRange(three));
        ArrayType arrT1 = NodeFactory.makeArrayType(span, zz, ind1);
        ArrayType arrT2 = NodeFactory.makeArrayType(span, zz, ind2);
        ArrayType arrT3 = NodeFactory.makeArrayType(span, zz, ind3);
        MatrixType matT = NodeFactory.makeMatrixType(span, zz, dim1, dim2);

        assertEquals(TRUE, sub(checker, arr1, arrT1));
        assertEquals(TRUE, sub(checker, arrT1, arr1));
        assertEquals(TRUE, sub(checker, arr2, arrT2));
        assertEquals(TRUE, sub(checker, arrT2, arr2));
        assertEquals(TRUE, sub(checker, arr3, arrT3));
        assertEquals(TRUE, sub(checker, arrT3, arr3));
        assertEquals(TRUE, sub(checker, mat,   matT));
        assertEquals(TRUE, sub(checker, matT,   mat));
    }

    private static final GlobalEnvironment GLOBAL_ENV;
    static {
        Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();

        ApiIndex builtin =
            api("AnyType",
                absTrait("Any"),
                absTrait("Tuple", "AnyType.Any"));
        apis.put(builtin.ast().getName(), builtin);

        ApiIndex library =
            api("FortressLibrary",
                absTrait("Object", "AnyType.Any"),
                absTrait("Array1", "AnyType.Any"),
                absTrait("Array2", "AnyType.Any"),
                absTrait("Array3", "AnyType.Any"),
                absTrait("Matrix", "AnyType.Any"));
        apis.put(library.ast().getName(), library);

        GLOBAL_ENV = new GlobalEnvironment.FromMap(apis);
    }

    /**
     * Make an ApiIndex with the given name and traits.
     * @param name  Assumed to consist of a single part (has no '.' separators).
     * @param traits  Each index is assumed to wrap an AbsDecl (not a Decl).
     */
    private static ApiIndex api(String name, TraitIndex... traits) {
        List<AbsDecl> traitDecls = new ArrayList<AbsDecl>(traits.length);
        Map<Id, TypeConsIndex> traitMap = new HashMap<Id, TypeConsIndex>();
        for (TraitIndex t : traits) {
            traitDecls.add((AbsDecl) t.ast());
            traitMap.put(t.ast().getName(), t);
        }
        Api ast = new Api(NodeFactory.makeAPIName(name),
                          Collections.<Import>emptyList(),
                          traitDecls);
        return new ApiIndex(ast,
                            Collections.<Id, Variable>emptyMap(),
                            CollectUtil.<IdOrOpOrAnonymousName, Function>emptyRelation(),
                            traitMap,
                            CollectUtil.<Id, Dimension>emptyMap(),
                            CollectUtil.<Id, Unit>emptyMap(),
                            Collections.<String, GrammarIndex>emptyMap(),
                            0);
    }

    /**
     * Make a ComponentIndex with the given name and traits.
     * @param name  Assumed to consist of a single part (has no '.' separators).
     * @param traits  Each index is assumed to wrap a Decl (not an AbsDecl).
     */
    private static ComponentIndex component(String name, TraitIndex... traits) {
        List<Decl> traitDecls = new ArrayList<Decl>(traits.length);
        Map<Id, TypeConsIndex> traitMap = new HashMap<Id, TypeConsIndex>();
        for (TraitIndex t : traits) {
            traitDecls.add((Decl) t.ast());
            traitMap.put(t.ast().getName(), t);
        }
        Component ast = new Component(NodeFactory.makeAPIName(name),
                                      Collections.<Import>emptyList(),
                                      Collections.<Export>emptyList(),
                                      traitDecls);
        return new ComponentIndex(ast,
                                  Collections.<Id, Variable>emptyMap(),
                                  Collections.<VarDecl>emptySet(),
                                  CollectUtil.<IdOrOpOrAnonymousName, Function>emptyRelation(),
                                  traitMap,
                                  CollectUtil.<Id, Dimension>emptyMap(),
                                  CollectUtil.<Id, Unit>emptyMap(),
                                  0);
    }

    /**
     * Make a ProperTraitIndex with the given name and supertypes.  Wrapped AST is a Decl.
     * @param name  A simple name.
     * @param supers  Type strings (parsed by parseType()); must parse to TraitTypes.
     */
    private static ProperTraitIndex trait(String name, String... supers) {
        return traitHelper(name, supers, false);
    }

    /**
     * Make a ProperTraitIndex with the given name and supertypes.  Wrapped AST is an AbsDecl.
     * @param name  A simple name.
     * @param supers  Type strings (parsed by parseType()); must parse to TraitTypes.
     */
    private static ProperTraitIndex absTrait(String name, String... supers) {
        return traitHelper(name, supers, true);
    }

    private static ProperTraitIndex traitHelper(String name, String[] supers, boolean absDecl) {
        List<TraitTypeWhere> extendsClause = new ArrayList<TraitTypeWhere>(supers.length);
        for (String sup : supers) {
            BaseType supT = (BaseType) parseType(sup);
            extendsClause.add(new TraitTypeWhere(supT, new WhereClause()));
        }
        TraitAbsDeclOrDecl ast;
        List<StaticParam> sparams = Collections.<StaticParam>emptyList();
        if (absDecl) {
            ast = new AbsTraitDecl(NodeFactory.makeId(name), sparams,
                                   extendsClause,
                                   Collections.<AbsDecl>emptyList());
        }
        else {
            ast = new TraitDecl(NodeFactory.makeId(name), sparams,
                                extendsClause,
                                Collections.<Decl>emptyList());
        }
        return new ProperTraitIndex(ast,
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Function>emptySet(),
                                    CollectUtil.<IdOrOpOrAnonymousName, Method>emptyRelation(),
                                    CollectUtil.<IdOrOpOrAnonymousName, FunctionalMethod>emptyRelation());
    }

    /** Assumes each TraitIndex wraps a non-abstract declaration (a Decl). */
    private static SubtypeChecker makeAnalyzer(TraitIndex... traits) {
        ComponentIndex c = component("SubtypeCheckerTestComponent", traits);
        SubtypeChecker sc = SubtypeChecker.make(new TraitTable(c, GLOBAL_ENV));
        List<StaticParam> sparams = new ArrayList<StaticParam>();
        sparams.add(NodeFactory.makeTypeParam("ALPHA", "Number"));
        sparams.add(NodeFactory.makeTypeParam("BETA",  "A"));
        return sc.extend(sparams, FortressUtil.emptyWhereClause());
    }

    /** Test the application type checking. */
    public void testApplication() {
        Type arrow1 = parseType("(A, B) -> C");
        Type lhs1 = parseType("A");
        Type rhs1 = parseType("B");
        assertEquals(parseType("C"),
                     TypesUtil.applicationType(checker,
                                               arrow1,
                                               Arrays.asList(lhs1, rhs1)).unwrap(BOTTOM));
    }

}
