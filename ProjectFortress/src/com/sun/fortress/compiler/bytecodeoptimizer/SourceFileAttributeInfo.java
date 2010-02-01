/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.bytecodeoptimizer;

class SourceFileAttributeInfo extends AttributeInfo {
  String sourceFileName;

  SourceFileAttributeInfo(ClassToBeOptimized cls, String name, int length) {
    attributeName = name;
    attributeLength = length;
    int sourceFileIndex = cls.reader.read2Bytes();
    sourceFileName = cls.cp[sourceFileIndex].getUtf8String();
  }

  public void Print() {
    System.out.println("Source File Name = " + sourceFileName);
  }
}


