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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

/** An interface for visitors over Node that do not return a value. */
public interface NodeVisitor_void {

  /** Process an instance of AbstractNode. */
  public void forAbstractNode(AbstractNode that);

  /** Process an instance of AbsExternalSyntax. */
  public void forAbsExternalSyntax(AbsExternalSyntax that);

  /** Process an instance of AliasedDottedId. */
  public void forAliasedDottedId(AliasedDottedId that);

  /** Process an instance of AliasedName. */
  public void forAliasedName(AliasedName that);

  /** Process an instance of ArrayComprehensionClause. */
  public void forArrayComprehensionClause(ArrayComprehensionClause that);

  /** Process an instance of Binding. */
  public void forBinding(Binding that);

  /** Process an instance of CaseClause. */
  public void forCaseClause(CaseClause that);

  /** Process an instance of Contract. */
  public void forContract(Contract that);

  /** Process an instance of CaseParamExpr. */
  public void forCaseParamExpr(CaseParamExpr that);

  /** Process an instance of CaseParamLargest. */
  public void forCaseParamLargest(CaseParamLargest that);

  /** Process an instance of CaseParamSmallest. */
  public void forCaseParamSmallest(CaseParamSmallest that);

  /** Process an instance of Catch. */
  public void forCatch(Catch that);

  /** Process an instance of CatchClause. */
  public void forCatchClause(CatchClause that);

  /** Process an instance of Component. */
  public void forComponent(Component that);

  /** Process an instance of Api. */
  public void forApi(Api that);

  /** Process an instance of SquareDimUnit. */
  public void forSquareDimUnit(SquareDimUnit that);

  /** Process an instance of CubicDimUnit. */
  public void forCubicDimUnit(CubicDimUnit that);

  /** Process an instance of InverseDimUnit. */
  public void forInverseDimUnit(InverseDimUnit that);

  /** Process an instance of DimDecl. */
  public void forDimDecl(DimDecl that);

  /** Process an instance of DoFront. */
  public void forDoFront(DoFront that);

  /** Process an instance of EnsuresClause. */
  public void forEnsuresClause(EnsuresClause that);

  /** Process an instance of Entry. */
  public void forEntry(Entry that);

  /** Process an instance of Export. */
  public void forExport(Export that);

  /** Process an instance of Expr. */
  public void forExpr(Expr that);

  /** Process an instance of AsExpr. */
  public void forAsExpr(AsExpr that);

  /** Process an instance of AsIfExpr. */
  public void forAsIfExpr(AsIfExpr that);

  /** Process an instance of Block. */
  public void forBlock(Block that);

  /** Process an instance of CaseExpr. */
  public void forCaseExpr(CaseExpr that);

  /** Process an instance of Do. */
  public void forDo(Do that);

  /** Process an instance of For. */
  public void forFor(For that);

  /** Process an instance of If. */
  public void forIf(If that);

  /** Process an instance of Label. */
  public void forLabel(Label that);

  /** Process an instance of Try. */
  public void forTry(Try that);

  /** Process an instance of TypeCase. */
  public void forTypeCase(TypeCase that);

  /** Process an instance of While. */
  public void forWhile(While that);

  /** Process an instance of Accumulator. */
  public void forAccumulator(Accumulator that);

  /** Process an instance of AtomicExpr. */
  public void forAtomicExpr(AtomicExpr that);

  /** Process an instance of Exit. */
  public void forExit(Exit that);

  /** Process an instance of Spawn. */
  public void forSpawn(Spawn that);

  /** Process an instance of Throw. */
  public void forThrow(Throw that);

  /** Process an instance of TryAtomicExpr. */
  public void forTryAtomicExpr(TryAtomicExpr that);

  /** Process an instance of FnExpr. */
  public void forFnExpr(FnExpr that);

  /** Process an instance of FloatLiteral. */
  public void forFloatLiteral(FloatLiteral that);

  /** Process an instance of IntLiteral. */
  public void forIntLiteral(IntLiteral that);

  /** Process an instance of CharLiteral. */
  public void forCharLiteral(CharLiteral that);

  /** Process an instance of StringLiteral. */
  public void forStringLiteral(StringLiteral that);

  /** Process an instance of VoidLiteral. */
  public void forVoidLiteral(VoidLiteral that);

  /** Process an instance of VarRefExpr. */
  public void forVarRefExpr(VarRefExpr that);

  /** Process an instance of ArrayComprehension. */
  public void forArrayComprehension(ArrayComprehension that);

  /** Process an instance of SetComprehension. */
  public void forSetComprehension(SetComprehension that);

  /** Process an instance of MapComprehension. */
  public void forMapComprehension(MapComprehension that);

  /** Process an instance of ListComprehension. */
  public void forListComprehension(ListComprehension that);

  /** Process an instance of UnitRef. */
  public void forUnitRef(UnitRef that);

  /** Process an instance of ExtentRange. */
  public void forExtentRange(ExtentRange that);

  /** Process an instance of ExternalSyntax. */
  public void forExternalSyntax(ExternalSyntax that);

  /** Process an instance of DottedId. */
  public void forDottedId(DottedId that);

  /** Process an instance of Fun. */
  public void forFun(Fun that);

  /** Process an instance of Name. */
  public void forName(Name that);

  /** Process an instance of Enclosing. */
  public void forEnclosing(Enclosing that);

  /** Process an instance of Opr. */
  public void forOpr(Opr that);

  /** Process an instance of PostFix. */
  public void forPostFix(PostFix that);

  /** Process an instance of SubscriptAssign. */
  public void forSubscriptAssign(SubscriptAssign that);

  /** Process an instance of SubscriptOp. */
  public void forSubscriptOp(SubscriptOp that);

  /** Process an instance of AnonymousFnName. */
  public void forAnonymousFnName(AnonymousFnName that);

  /** Process an instance of ConstructorFnName. */
  public void forConstructorFnName(ConstructorFnName that);

  /** Process an instance of Generator. */
  public void forGenerator(Generator that);

  /** Process an instance of Id. */
  public void forId(Id that);

  /** Process an instance of IfClause. */
  public void forIfClause(IfClause that);

  /** Process an instance of ImportApi. */
  public void forImportApi(ImportApi that);

  /** Process an instance of ImportStar. */
  public void forImportStar(ImportStar that);

  /** Process an instance of ImportNames. */
  public void forImportNames(ImportNames that);

  /** Process an instance of FixedDim. */
  public void forFixedDim(FixedDim that);

  /** Process an instance of KeywordType. */
  public void forKeywordType(KeywordType that);

  /** Process an instance of VarargsType. */
  public void forVarargsType(VarargsType that);

  /** Process an instance of Op. */
  public void forOp(Op that);

  /** Process an instance of Param. */
  public void forParam(Param that);

  /** Process an instance of PropertyDecl. */
  public void forPropertyDecl(PropertyDecl that);

  /** Process an instance of BoolParam. */
  public void forBoolParam(BoolParam that);

  /** Process an instance of DimensionParam. */
  public void forDimensionParam(DimensionParam that);

  /** Process an instance of IntParam. */
  public void forIntParam(IntParam that);

  /** Process an instance of NatParam. */
  public void forNatParam(NatParam that);

  /** Process an instance of OperatorParam. */
  public void forOperatorParam(OperatorParam that);

  /** Process an instance of SimpleTypeParam. */
  public void forSimpleTypeParam(SimpleTypeParam that);

  /** Process an instance of TestDecl. */
  public void forTestDecl(TestDecl that);

  /** Process an instance of TypeCaseClause. */
  public void forTypeCaseClause(TypeCaseClause that);

  /** Process an instance of ArrowType. */
  public void forArrowType(ArrowType that);

  /** Process an instance of ArrayType. */
  public void forArrayType(ArrayType that);

  /** Process an instance of ListType. */
  public void forListType(ListType that);

  /** Process an instance of MapType. */
  public void forMapType(MapType that);

  /** Process an instance of MatrixType. */
  public void forMatrixType(MatrixType that);

  /** Process an instance of ParamType. */
  public void forParamType(ParamType that);

  /** Process an instance of VectorType. */
  public void forVectorType(VectorType that);

  /** Process an instance of TupleType. */
  public void forTupleType(TupleType that);

  /** Process an instance of IdType. */
  public void forIdType(IdType that);

  /** Process an instance of VoidType. */
  public void forVoidType(VoidType that);

  /** Process an instance of DimRef. */
  public void forDimRef(DimRef that);

  /** Process an instance of ProductDimType. */
  public void forProductDimType(ProductDimType that);

  /** Process an instance of QuotientDimType. */
  public void forQuotientDimType(QuotientDimType that);

  /** Process an instance of ProductUnitType. */
  public void forProductUnitType(ProductUnitType that);

  /** Process an instance of QuotientUnitType. */
  public void forQuotientUnitType(QuotientUnitType that);

  /** Process an instance of DimTypeConversion. */
  public void forDimTypeConversion(DimTypeConversion that);

  /** Process an instance of BaseNatRef. */
  public void forBaseNatRef(BaseNatRef that);

  /** Process an instance of BaseOprRef. */
  public void forBaseOprRef(BaseOprRef that);

  /** Process an instance of BaseDimRef. */
  public void forBaseDimRef(BaseDimRef that);

  /** Process an instance of BaseUnitRef. */
  public void forBaseUnitRef(BaseUnitRef that);

  /** Process an instance of BaseBoolRef. */
  public void forBaseBoolRef(BaseBoolRef that);

  /** Process an instance of NotBoolRef. */
  public void forNotBoolRef(NotBoolRef that);

  /** Process an instance of OrBoolRef. */
  public void forOrBoolRef(OrBoolRef that);

  /** Process an instance of AndBoolRef. */
  public void forAndBoolRef(AndBoolRef that);

  /** Process an instance of ImpliesBoolRef. */
  public void forImpliesBoolRef(ImpliesBoolRef that);

  /** Process an instance of SumStaticArg. */
  public void forSumStaticArg(SumStaticArg that);

  /** Process an instance of ProductStaticArg. */
  public void forProductStaticArg(ProductStaticArg that);

  /** Process an instance of QuotientStaticArg. */
  public void forQuotientStaticArg(QuotientStaticArg that);

  /** Process an instance of ExponentStaticArg. */
  public void forExponentStaticArg(ExponentStaticArg that);

  /** Process an instance of DimensionStaticArg. */
  public void forDimensionStaticArg(DimensionStaticArg that);

  /** Process an instance of TypeArg. */
  public void forTypeArg(TypeArg that);

  /** Process an instance of UnitDecl. */
  public void forUnitDecl(UnitDecl that);

  /** Process an instance of AbsVarDecl. */
  public void forAbsVarDecl(AbsVarDecl that);

  /** Process an instance of VarDecl. */
  public void forVarDecl(VarDecl that);

  /** Process an instance of AbsTypeAlias. */
  public void forAbsTypeAlias(AbsTypeAlias that);

  /** Process an instance of TypeAlias. */
  public void forTypeAlias(TypeAlias that);

  /** Process an instance of WhereBool. */
  public void forWhereBool(WhereBool that);

  /** Process an instance of WhereExtends. */
  public void forWhereExtends(WhereExtends that);

  /** Process an instance of WhereNat. */
  public void forWhereNat(WhereNat that);

  /** Process an instance of WhereUnit. */
  public void forWhereUnit(WhereUnit that);

  /** Process an instance of WhereWidensCoerces. */
  public void forWhereWidensCoerces(WhereWidensCoerces that);
}
