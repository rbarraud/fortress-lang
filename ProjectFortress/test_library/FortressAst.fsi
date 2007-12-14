(*******************************************************************************
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
 ******************************************************************************)

api FortressAst

   (* Fortress representation of Fortress AST nodes *)
   trait Node
      toString():String
   end

   trait AbsDeclOrDecl extends Node end
   trait AbsDecl extends AbsDeclOrDecl end
   trait Decl extends AbsDeclOrDecl end
   trait Lhs extends AbsDeclOrDecl end

   trait Generic extends Node
      staticParam:List[\StaticParam\]
   end

   trait HasParams extends Node
      params:Maybe[\List[\Param\]\]
      decls:List[\AbsDeclOrDecl\]
   end

   trait HasWhere extends Node
      wheres:List[\WhereClause\]
   end

   trait ExprOrUnitExpr extends Node end

   trait Applicable extends { AbsDeclOrDecl }
     name : SimpleName
     staticParams : List[\StaticParam\]
     params:List[\Param\]
     returnType:Maybe[\Type\]
     whereClause:List[\WhereClause\]
   end

   trait GenericWithParams extends { Generic, HasParams } end
   trait GenericAbsDeclOrDecl extends { Generic, AbsDeclOrDecl } end
   trait GenericDecl extends Decl end
   trait GenericAbsDeclOrDeclWithParams extends { GenericWithParams, GenericAbsDeclOrDecl } end
   trait GenericDeclWithParams extends GenericDecl end

   (* TODO: put comprises clauses on AbstractNode and all subtypes
                  so that they can't be extended *)
   trait AbstractNode extends { Node } end

   (* TODO: Implement compilation unit *)

   (* TODO: Implement import *)

   (* TODO: Implement export *)

   (* TODO: Implement aliasedName *)

   (* TODO: Implement aliasedDottedName *)

   (* TODO: Implement traitObjectAbsDeclOrDecls *)

   trait VarAbsDeclOrDecl extends AbsDeclOrDecl
      lhs:List[\LValueBind\]
   end
   trait AbsVarDecl extends { AbsDecl, Decl } end
   object VarDecl(lhs:List[\LValueBind\], init:Expr) extends { VarAbsDeclOrDecl, Decl } end

   trait LValue extends AbstractNode end

   object LValueBind(name:IdName, atype:Maybe[\Type\], mods:List[\Modifier\], mutable:Boolean) extends { LValue, Lhs } end
   trait Unpasting extends LValue end
   object UnpastingBind(name:IdName, dims:List[\ExtentRange\]) extends Unpasting end
   object UnpastingSplit(elems:List[\Unpasting\], dims:ZZ32) extends Unpasting end

   (**
     * functional declaration in components or APIs
     *)
   trait FnAbsDeclOrDecl extends { AbstractNode, Applicable, GenericDecl }
      mods:List[\Modifier\]
      name:SimpleName
      staticParams:List[\StaticParam\]
      params:List[\Param\]
      returnType:Maybe[\Type\]
      throwsClause:Maybe[\List[\TraitType\]\]
      whereClauses:List[\WhereClause\]
      acontract:Contract
      selfName:String
   end
   (**
     * functional declaration in APIs
     * AbsFnDecl ::= AbsFnMods? FnHeaderFront FnHeaderClause
     *             | FnSig
     * FnSig ::= Name : ArrowType
     * FnHeaderFront ::= Id StaticParams? ValParam
     *                 | OpHeaderFront
     * OpHeaderFront ::= opr StaticParams? (LeftEncloser | Encloser) Params
     *                     (RightEncloser | Encloser)
     *                     (:= ( SubscriptAssignParam ))?
     *                 | opr StaticParams? ValParam Op
     *                 | opr (Op | ExponentOp | Encloser) StaticParams? ValParam
     * SubscriptAssignParam ::= Varargs | Param
     * FnHeaderClause ::= IsType? FnClauses
     * FnDecl ::= FnMods? FnHeaderFront FnHeaderClause
     *          | FnSig
     * e.g.) swap (x: Object, y: Object): (Object, Object)
     *)
   object AbsFnDecl() extends {FnAbsDeclOrDecl, AbsDecl } end
   (**
     * functional declaration in components
     * FnDecl ::= FnMods? FnHeaderFront FnHeaderClause = Expr
     * e.g.) swap (x, y) = (y, x)
     *)
   trait FnDecl extends FnAbsDeclOrDecl end
   object FnDef(body:Expr) extends FnDecl end

   trait Param extends Node
      mods:List[\Modifier\]
      name:IdName
   end
   object NormalParam(mods:List[\Modifier\], name:IdName, atype:Maybe[\Type\], defaultExpr:Maybe[\Expr\]) extends Param end
   object VarargsParam(mods:List[\Modifier\], name:IdName, varargsType:VarargsType) extends Param end

   (* TODO: Implement DimUnitDecl *)

   (* TODO: Implement TestDecl *)

   (* TODO: Implement PropertyDecl *)

   (* TODO: Implement GrammarDecl *)

   (* TODO: Implement ProductionDecl *)

   (* TODO: Implement SyntaxDecl *)

   (* TODO: Implement SymbolDecl *)

   trait Expr extends AbstractNode end

   object AsExpr(expr : Expr, atype : Type) extends Expr end
   object AsIfExpr(expr:Expr, atype:Type) extends Expr end

   trait TaggedUnitExpr extends Expr
      val : Expr
      unitExpr : UnitExpr
   end
   object ProductUnitExpr(val:Expr, unitExpr:UnitExpr) extends TaggedUnitExpr end
   object QuotientUnitExpr(val:Expr, unitExpr:UnitExpr) extends TaggedUnitExpr end

   object ChangeUnitExpr(val:Expr, unitExpr:UnitExpr) extends Expr end
   object Assignment(lhs:List[\Lhs\], aopr:Maybe[\Opr\], rhs:Expr) extends Expr end

   trait DelimitedExpr extends Expr end
   object Block(exprs:List[\Expr\]) extends DelimitedExpr end
   object CaseExpr(param:CaseParam, compare:Maybe[\Opr\], clauses:List[\CaseClause\], elseClause:Maybe[\Block\]) extends DelimitedExpr end
   object Do(fronts:List[\DoFront\]) extends DelimitedExpr end
   object For(gens:List[\Generator\], body:DoFront) extends DelimitedExpr end
   object If(clauses:List[\IfClause\], elseClause:Maybe[\Block\]) extends DelimitedExpr end
   object Label(name:IdName, body:Block) extends DelimitedExpr end
   object Try(body:Block, catchClause:Maybe[\Catch\], forbids:List[\TraitType\], finallyClause:Maybe[\Block\]) extends DelimitedExpr end
   object TupleExpr(exprs:List[\Expr\], varargs:Maybe[\VarargsExpr\], keywords:List[\Binding\]) extends DelimitedExpr end
   object Typecase(bind:List[\Binding\], clauses:List[\TypecaseClause\], elseClause:Maybe[\Block\]) extends DelimitedExpr end
   object While(testExpr:Expr, body:Do) extends DelimitedExpr end

   trait FlowExpr extends Expr end
   object Accumulator(aopr:Opr, gens:List[\Generator\], body:Expr) extends FlowExpr end
   object AtomicExpr(expr:Expr) extends FlowExpr end
   object Exit(target:Maybe[\IdName\], returnExpr:Maybe[\Expr\]) extends FlowExpr end
   object Spawn(body:Expr) extends FlowExpr end
   object Throw(expr:Expr) extends FlowExpr end
   object TryAtomicExpr(expr:Expr) extends FlowExpr end

   object FnExpr(parenthesized:Boolean, name:SimpleName, staticParams:List[\StaticParam\], params:List[\Param\], returnType:Maybe[\Type\], whereClause:List[\WhereClause\], throwsClause:Maybe[\List[\TraitType\]\], body:Expr) extends { Expr, Applicable } end

   trait LetExpr extends Expr
      body:List[\Expr\]
   end
   object LetFn(body:List[\Expr\], fns:List[\FnDef\]) extends LetExpr end
   object LocalVarDecl(lhs:List[\LValue\], rhs:Maybe[\IntLiteral\]) extends LetExpr end

   object GeneratedExpr(expr:Expr, gens:List[\Generator\]) extends Expr end

   trait OpExpr extends Expr end
   object OprExpr(ops:List[\QualifiedOpName\], args:List[\Expr\]) extends OpExpr end
   object SubscriptExpr(obj:Expr, subs:List[\Expr\], op:Maybe[\SubscriptOp\]) extends { Lhs, OpExpr } end

   trait Primary extends OpExpr end
   object CoercionInvocation(traitType:TraitType, staticArgs:List[\StaticArg\], arg:Expr) extends Primary end
   object MethodInvocation(obj:Expr, method:IdName, staticArgs:List[\StaticArg\], arg:Expr) extends Primary end

   trait AbstractFieldRef extends { Primary, Lhs }
      obj:Expr
   end
   object FieldRef(field:IdName) extends AbstractFieldRef end

   trait Juxt extends Primary
      exprs:List[\Expr\]
   end
   object LooseJuxt(exprs:List[\Expr\]) extends Juxt end
   object TightJuxt(exprs:List[\Expr\]) extends Juxt end

   (* TODO: Pair
     object ChainExpr(first:Expr, links:List[\Pair[\Opr, Expr\]\]) extends Primary end
    *)
   object FnRef(fns:List[\QualifiedIdName\], staticArgs:List[\StaticArg\]) extends Primary end

   trait BaseExpr extends Primary end

   object VarRef(avar:QualifiedIdName) extends { BaseExpr, Lhs } end

   trait Literal extends BaseExpr
      text:String
   end

   trait NumberLiteral extends Literal end
   object FloatLiteral(text:String, intPart:ZZ64, numerator:ZZ64, denomBase:ZZ32, denomPower:ZZ32) extends NumberLiteral end
   object IntLiteral(val:ZZ32) extends NumberLiteral end

   object CharLiteral(val:ZZ32) extends Literal end
   object StringLiteral(val:String) extends Literal end
   object VoidLiteral() extends Literal end


   trait Aggregate extends BaseExpr end
   object MapExpr(elements:List[\Entry\]) extends Aggregate end

   trait ArrayExpr extends Aggregate end
   object ArrayElement(element:Expr) extends ArrayExpr end
   object ArrayElements(dimension:ZZ32, elements:List[\ArrayExpr\]) extends ArrayExpr end

   trait Comprehension extends Primary end
(* TODO:
   Exception in thread "main" java.lang.IllegalArgumentException: Visitor com.sun.fortress.interpreter.evaluator.EvalType does not support visiting values of type com.sun.fortress.nodes.TaggedDimType
   object ArrayComprehension(List[\ArrayComprehensionClause\] clauses) extends Comprehension end
*)
   trait GeneratedComprehension extends Comprehension
      gens:List[\Generator\]
   end
   object SetComprehension(gens:List[\Generator\], element:Expr) extends GeneratedComprehension end
   object MapComprehension(gens:List[\Generator\], entry:Entry) extends GeneratedComprehension end
   object ListComprehension(gens:List[\Generator\], element:Expr) extends GeneratedComprehension end

   trait Type extends AbstractNode end
   object ArrowType(domain:Type, range:Type, throwsClause:Maybe[\List[\TraitType\]\]) extends Type end

   trait NonArrowType extends Type end
   trait TraitType extends NonArrowType end
   object ArrayType(element:Type, indices:Indices) extends TraitType end
   object IdType(name:QualifiedIdName) extends TraitType end
   object MatrixType(element:Type, dimensions:List[\ExtentRange\]) extends TraitType end
   object InstantiatedType(name:QualifiedIdName, args:List[\StaticArg\]) extends TraitType end

   object TupleType(elements:List[\Type\], varargs:Maybe[\VarargsType\], keywords:List[\KeywordType\]) extends NonArrowType end
   object VoidType() extends NonArrowType end

   trait DimType extends NonArrowType end
   object TaggedDimType(atype:Type, dims:DimExpr, Maybe[\UnitExpr\]) extends DimType end
   object TaggedUnitType(atype:Type, unitExpr:UnitExpr) extends NonArrowType end

   trait StaticArg extends Type end
   (* TODO: Implement static args *)

   trait StaticExpr extends AbstractNode end
   (* TODO: Implement static exprs. *)

   (**
     * integer expression
     *)
   trait IntExpr extends StaticExpr end

   (**
     * boolean expression
     * StaticExpr ::= BoolExpr
     *)
   trait BoolExpr extends StaticExpr end

   (* TODO: rest *)
   (**
     * boolean constraint
     * BoolExpr ::= BoolConstraint
     *)
   trait BoolConstraint extends BoolExpr end

   trait UnitExpr extends StaticExpr end

    (**
      * dimension expression
      *)
   trait DimExpr extends StaticExpr end

   trait WhereClause extends AbstractNode end
   object WhereExtends(name:IdName, supers:List[\TraitType\]) extends WhereClause end
   object TypeAlias(name:IdName, staticParams:List[\StaticParam\], atype:Type)  extends { WhereClause, Decl, AbsDecl } end
   object WhereNat(name:IdName) extends WhereClause end
   object WhereInt(name:IdName) extends WhereClause end
   object WhereBool(name:IdName) extends WhereClause end
   object WhereUnit(name:IdName) extends WhereClause end
   object WhereCoerces(left:Type, right:Type) extends WhereClause end
   object WhereWidens(left:Type, right:Type) extends WhereClause end
   object WhereWidensCoerces(left:Type, right:Type) extends WhereClause end
   object WhereEquals(left:QualifiedIdName, right:QualifiedIdName) extends WhereClause end
   object UnitConstraint(name:IdName) extends WhereClause end

   trait IntConstraint extends WhereClause end
   object LEConstraint(left:IntExpr, right:IntExpr) extends IntConstraint end
   object LTConstraint(left:IntExpr, right:IntExpr) extends IntConstraint end
   object GEConstraint(left:IntExpr, right:IntExpr) extends IntConstraint end
   object GTConstraint(left:IntExpr, right:IntExpr) extends IntConstraint end
   object IEConstraint(left:IntExpr, right:IntExpr) extends IntConstraint end
   object BoolConstraintExpr(constraint:BoolConstraint) extends WhereClause end
   (**
     * contracts used in functional declarations and object declarations
     * Contract ::= Requires? Ensures? Invariant?
     * Requires ::= requires { ExprList? }
     * Ensures ::= ensures { EnsuresClauseList? }
     * Invariant ::= invariant { ExprList? }
     * CoercionContract ::= Ensures? Invariant?
     * e.g.) requires { n GE 0 } ensures { result GE 0 }
     *)
   object Contract(requiresLs:Maybe[\List[\Expr\]\], ensuresLs:Maybe[\List[\EnsuresClause\]\], invariantsLs:Maybe[\List[\Expr\]\]) extends AbstractNode end
   (**
     * ensures clause used in contracts
     * EnsuresClause ::= Expr (provided Expr)?
     * e.g.) sorted(result) provided sorted(input)
     *)
   object EnsuresClause(post:Expr, pre:Maybe[\Expr\]) extends AbstractNode end

   trait Modifier extends AbstractNode end
   (* TODO: Implement modifiers *)

   (**
    * static parameter
    *)
   trait StaticParam extends AbstractNode end

   (* TODO: Implement static params *)

   trait Name extends AbstractNode end
   object DottedName(ids:List[\Id\]) extends Name end

   (**
     * name prefixed by an optional API name
     *)
   trait QualifiedName extends Name
      aapi:Maybe[\DottedName\]
      name:SimpleName
   end
   (**
     * qualified id name
     * QualifiedIdName ::= (Id.)*Id
     * e.g.) com.sun.fortress.nodes_util.getName
     *)
   object QualifiedIdName(aapi:Maybe[\DottedName\], name:SimpleName, name:IdName) extends QualifiedName end
   (**
     * qualified operator name
     * internal uses only
     * e.g.) com.sun.fortress.nodes_util.+
     *)
   object QualifiedOpName(aapi:Maybe[\DottedName\], name:SimpleName, name:OpName) extends QualifiedName end
   (**
     * unqualified name
     *)
   trait SimpleName extends Name end
   (**
     * unqualified id name
     *)
   object IdName(id:Id) extends SimpleName end
   (**
     * unqualified operator name
     *)
   trait OpName extends SimpleName end
   (**
     * infix/prefix/nofix/multifix operator
     * e.g.) COMPOSE
     * e.g.) +
     *)
   object Opr(op:Op) extends OpName end
   (**
     * postfix operator
     * e.g.) 3!
     *)
   object PostFix(op:Op) extends OpName end
   (**
     * pair of enclosing operators
     * EncloserPair ::= LeftEncloser RightEncloser
     * e.g.) </ />
     *)
   trait Enclosing extends OpName
      open:Op
      close:Op
   end
   (**
     * bracketing operator
     * e.g.) a[i]
     *)
   object Bracketing(open:Op, close:Op) extends Enclosing end
   (**
      * subscripting or subscripted assignment operator
      * e.g.) a[i]
      *)
   object SubscriptOp(open:Op, close:Op) extends Enclosing end

   (* TODO: Implement AnonymousFnName *)

   (* TODO: Implement ConstructorFNName *)

   (* TODO: Implement ArrayComprehensionClause *)

   (**
    * binding used in tuple expressions and typecase expressions
    * Binding ::= Id = Expr
    * e.g.) x = myLoser.myField
    *)
   object Binding(name:IdName, init:Expr) extends AbstractNode end
   (**
     * case clause used in case expressions and extremum expressions
     * CaseClause ::= Expr => BlockElems
     * e.g.) { Jupiter, Saturn, Uranus, Neptune } => "outer"
    *)
   object CaseClause(match:Expr, body:Block) extends AbstractNode end
   (**
     * condition expression used in case expressions and extremum expressions
     *)
   trait CaseParam extends AbstractNode end
   (**
     * condition expression used in case expressions
     *)
   object CaseParamExpr(expr:Expr) extends CaseParam end
   (**
     * "largest" used in extremum expressions
     *)
   object CaseParamLargest() extends CaseParam end
   (**
     * "smallest" used in extremum expressions
     *)
   object CaseParamSmallest() extends CaseParam end

   (* TODO: Implement contract *)

   (**
    * catch clause used in try expressions
    * Catch ::= catch Id CatchClauses
    * e.g.) catch e IOException => throw ForbiddenException(e)
    *)
   object Catch(name:IdName, clauses:List[\CatchClause\]) extends AbstractNode end
   (**
    * each clause in a catch clause used in try expressions
    * CatchClause ::= TraitType => BlockElems
    * e.g.) IOException => throw ForbiddenException(e)
    *)
   object CatchClause(match:TraitType, body:Block) extends AbstractNode end
   (**
    * block expression used in do expressions
    * DoFront ::= (at Expr)? atomic? do BlockElems?
    * e.g.) at a.region(j) do w := a_j
    *)
   object DoFront(loc:Maybe[\Expr\], isatomic:Boolean, body:Block) extends AbstractNode end
   (**
    * if clause used in if expressions
    * DelimitedExpr ::= if Expr then BlockElems Elifs? Else? end
    * Elif ::= elif Expr then BlockElems
    * e.g.) if x IN { 0, 1, 2 } then 0
    *)
   object IfClause(condition:Expr, body:Block) extends AbstractNode end
   (**
    * typecase clause used in typecase expressions
    * TypecaseClause ::= TypecaseTypes => BlockElems
    * TypecaseTypes ::= ( TypeList )
    *                    | Type
    * e.g.) String => x.append("foo")
    *)
   object TypecaseClause(match:List[\Type\], body:Block) extends AbstractNode end
   (**
    * key/value pair used in map expressions and map comprehensions
    * Entry ::= Expr |-> Expr
    * e.g.) 'a' |-> 0
    *)
   object Entry(key:Expr, valueExpr:Expr) extends AbstractNode end

   (* TODO: Implement extent range *)

   (* TODO: Implement generator *)

   (**
    * identifier
    * e.g.) hashCode
    *)
   object Id(text:String) extends AbstractNode end
   (**
    * operator symbol
    * e.g.) ===
    *)
   object Op(text:String) extends AbstractNode end

   (**
    * varargs expression used in tuple expressions
    * Expr...
    * e.g.) [3 4 5]...
    *)
   object VarargsExpr(varargs:Expr) extends AbstractNode end
   (**
    * varargs type used in tuple types and varargs parameters
    * Type ...
    * e.g.) ZZ32...
    *)
   object VarargsType(baseType:Type) extends AbstractNode end
   (**
     * keyword type used in tuple types
     * KeywordType ::= Id = Type
     * e.g.) x = String
     *)
   object KeywordType(name:IdName, atype:Type)  extends AbstractNode end
   (**
     * trait type with a where clause used in extends clauses
     * TraitTypeWhere ::= TraitType Where?
     *)
   object TraitTypeWhere(traitType:TraitType, whereLs:List[\WhereClause\]) extends AbstractNode end
   (**
     * array indices
     *)
   trait Indices extends AbstractNode end
   (**
     * array dimensionality
     * ArraySize ::= ExtentRange(, ExtentRange)*
     * e.g.) 3, 2#1, 3:5
     *)
   object FixedDim(extents:List[\ExtentRange\]) extends Indices end

   (* TODO: Implement DimUnitOps *)

end