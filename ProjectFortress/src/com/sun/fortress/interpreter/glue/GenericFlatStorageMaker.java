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

package com.sun.fortress.interpreter.glue;

import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.types.FTypeVoid;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.FObject;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.HasIntValue;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.evaluator.values.PartiallyDefinedMethod;
import com.sun.fortress.interpreter.nodes.Contract;
import com.sun.fortress.interpreter.nodes.Decl;
import com.sun.fortress.interpreter.nodes.GenericDefWithParams;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.Modifier;
import com.sun.fortress.interpreter.nodes.NatParam;
import com.sun.fortress.interpreter.nodes.None;
import com.sun.fortress.interpreter.nodes.ObjectDecl;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.SimpleTypeParam;
import com.sun.fortress.interpreter.nodes.Some;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.WhereClause;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Useful;


public class GenericFlatStorageMaker extends GenericConstructor {

    // FlatStorageMaker[\T, m\]() traits Array1[\T, m\]
    // GFSM[] -> FSM instance (extends Constructor)
    // FSM() -> FS (extends Object, includes get/put + Array1 methods)

    /**
     * This method creates enough AST to define a generic native object
     * generator.  It includes explicit references and encoding rules for
     * naming user/library-supplied traits in the source code.
     */
    static ObjectDecl bogusObjectDecl() {
        List<Decl> defs = Collections.emptyList();
        List<Modifier> mods = Collections.emptyList();
        List<TypeRef> throws_ = Collections.emptyList();
        List<WhereClause> where = Collections.emptyList();
        Contract contract = Contract.make();

        Id name = Id.make("FlatStorageMaker");
        Option<List<StaticParam>> staticParams =  // [T, m]
            Some.<StaticParam>makeSomeList(Useful.list(SimpleTypeParam.make("T"), NatParam.make("m")));
        Option<List<Param>> params = Some.makeSomeList(Collections.<Param>emptyList()); // ()
        Option<List<TypeRef>> traits =  // Array1[\T, 0, m\]
            // new Some<List<TypeRef>>(Useful.<TypeRef>list(
            //         ParamType.make("Array1",
            //                 Useful.<StaticArg>list(TypeArg.make("T"),
            //                             BaseNatType.make(0),
            //                             TypeArg.make("m")
            //                             )
            //                        )
            //         ));
            new None<List<TypeRef>>();
        ObjectDecl od =  ObjectDecl.make(defs, mods, name, staticParams, params, traits, throws_, where, contract);

        /*
           try {
            (new Printer()).dump(od, System.err);
           } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
           }

        Produces:

               (ObjectDecl @"",0:0
                contract=(Contract)
                name=(Id name="FlatStorageMaker")
                params=(Some val=[])
                traits=None
                // traits=(Some val=[
                //   (ParamType
                //    generic=(IdType name=(DottedId names=["Array1"]))
                //    args=[
                //     (TypeArg
                //      type=(IdType name=(DottedId names=["T"])))
                //     (BaseNatType)
                //     (TypeArg
                //      type=(IdType name=(DottedId names=["m"])))])])
                staticParams=(Some val=[
                  (SimpleTypeParam
                   name=(Id name="T"))
                  (NatParam
                   id=(Id name="m"))]))

        */
        return od;
    }

    public GenericFlatStorageMaker(BetterEnv e) {
        super(e,bogusObjectDecl());
        e.putType("FlatStorageMaker", new FTypeGeneric(e, getDef()));
    }

    /* (non-Javadoc)
     * @see com.sun.fortress.interpreter.evaluator.values.GenericConstructor#makeAConstructor(com.sun.fortress.interpreter.evaluator.Environment, com.sun.fortress.interpreter.evaluator.types.FTypeObject, java.util.List)
     */
    @Override
    protected Constructor makeAConstructor(
            BetterEnv clenv,
            FTypeObject objectType,
            List<Parameter> objectParams) {

        FType t = clenv.getType("T");
        Number n = clenv.getNat("m");

        FlatStorageMaker cl = new FlatStorageMaker(clenv, objectType, getDef(), t, n);
        cl.setParams(objectParams);

        BetterEnv bte = new BetterEnv(cl.getWithin(), cl.getAt());
        bte.putValue("put", setter(clenv, t));
        bte.putValue("get", getter(clenv, t));
        bte.noteName("put");
        bte.noteName("get");

        // Todo this is semi-hacky.
        cl.parameterNames.add("put");
        cl.parameterNames.add("get");
        cl.finishInitializing(bte);

        return cl;
    }

    PartiallyDefinedMethod getter(BetterEnv env, FType t) {
        return new GetterMethod(env, t);
    }

    PartiallyDefinedMethod setter(BetterEnv env, FType t) {
        return new SetterMethod(env, t);
    }

    static class GetterMethod extends NativeMethod {

         /* (non-Javadoc)
          * @see com.sun.fortress.interpreter.evaluator.values.MethodClosure#applyMethod(java.util.List, com.sun.fortress.interpreter.evaluator.values.FObject, com.sun.fortress.interpreter.useful.HasAt)
          */
         @Override
         public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc) {
             FlatStorage fs = (FlatStorage) selfValue;
             int i = ((HasIntValue)args.get(0)).getInt();
             FValue r = fs.a[i];
             if (r==null) {
                 throw new ProgramError(loc,fs.getLexicalEnv(),
                        "Access to uninitialized element "+i+" of array "+fs);
             }
             return r;
         }

         GetterMethod(BetterEnv env, FType t) {
             super(env, t, "get", Useful.<FType>list(FTypeInt.T), t);
         }
     }

     static class SetterMethod extends NativeMethod {

         /* (non-Javadoc)
          * @see com.sun.fortress.interpreter.evaluator.values.MethodClosure#applyMethod(java.util.List, com.sun.fortress.interpreter.evaluator.values.FObject, com.sun.fortress.interpreter.useful.HasAt)
          */
         @Override
         public FValue applyMethod(List<FValue> args, FObject selfValue, HasAt loc) {
             FlatStorage fs = (FlatStorage) selfValue;
             // TODO type test.
             fs.a[((HasIntValue)args.get(1)).getInt()] = args.get(0);
             return FVoid.V;
         }

         SetterMethod(BetterEnv env, FType t) {
             super(env, t, "put", Useful.list(t, FTypeInt.T), FTypeVoid.T);
         }
     }

    static class FlatStorageMaker extends Constructor {
        /* (non-Javadoc)
         * @see com.sun.fortress.interpreter.evaluator.values.Constructor#makeAnObject(com.sun.fortress.interpreter.evaluator.Environment)
         */
        @Override
        protected FObject makeAnObject(BetterEnv lex_env, BetterEnv self_env) {
            // TODO Auto-generated method stub
            return new FlatStorage(selfType, lex_env, self_env, t, n);
        }

        public FlatStorageMaker(BetterEnv clenv, FTypeObject objectType, GenericDefWithParams odef, FType t, Number n) {
            super(clenv, objectType, odef);
            this.t = t;
            this.n = n.longValue();
        }

        FType t;
        long n;
    }

    static class FlatStorage extends FObject {

        public FlatStorage(FTypeObject selfType, BetterEnv lex_env, BetterEnv self_env, FType t, long n) {
            super(selfType, lex_env, self_env);
            this.t = t;
            this.a = new FValue[(int) n];
        }

        FType t;
        FValue[] a;
    }


}
