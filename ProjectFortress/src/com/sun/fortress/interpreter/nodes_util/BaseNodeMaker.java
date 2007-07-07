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

package com.sun.fortress.interpreter.nodes_util;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BaseNodeMaker {
    private static final String INDENT = "    ";

    private static boolean commented(String line) {
        return line.matches("#.*");
    }

    private static String removeDots(String line) {
        return line.replace(".","");
    }

    public static void makeBaseClass(BufferedReader lines) throws IOException {
        System.out.println("/* THIS FILE WAS AUTOMATICALLY GENERATED BY BaseNodeMaker.java FROM leafclasses */");
        System.out.println("package com.sun.fortress.interpreter.nodes;");
        System.out.println("import com.sun.fortress.interpreter.nodes_util.WrappedFValue;");
        System.out.println("import com.sun.fortress.interpreter.nodes_util.NodeUtil;");
        System.out.println("public class NodeVisitor<T> {");
        System.out.println(
                INDENT + INDENT +"protected final T acceptNode(AbstractNode n) {\n" +
                INDENT + INDENT + INDENT +"return n.accept(this);\n" +
                INDENT + INDENT +"}\n" +
                "\n" +
                INDENT + INDENT +"protected final T acceptNode(DefOrDecl n) {\n" +
                INDENT + INDENT + INDENT +"return n.accept(this);\n" +
                INDENT + INDENT +"}\n" +
                "\n"
        );


        System.out.println(INDENT + "public T NI(com.sun.fortress.interpreter.useful.HasAt x, String s) {");

        System.out.println(INDENT + INDENT +
                            "throw new Error(this.getClass().getName() + \".\" + s + \" not implemented (@\" + NodeUtil.getAt(x)+ \")\");");
        System.out.println(INDENT + "}");

        String line = lines.readLine();
        while (line != null) {
            if (! commented(line)) {
                String dotlessLine = removeDots(line);

                System.out.println(INDENT + "public T for" + dotlessLine +"(" + line +" x) {");
                System.out.println(INDENT +   INDENT + "return NI(x, \"for" + dotlessLine + "\");");
                System.out.println(INDENT + "}");
            }
            line = lines.readLine();
        }
        System.out.println("}");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new RuntimeException("Expected exactly one filename as an argument");
        }
        makeBaseClass(new BufferedReader(new FileReader(args[0])));
    }

}
