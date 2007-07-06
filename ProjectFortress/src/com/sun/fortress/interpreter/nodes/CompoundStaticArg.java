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

public abstract class CompoundStaticArg extends StaticArg {
  private final List<StaticArg> _values;

  /**
   * Constructs a CompoundStaticArg.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public CompoundStaticArg(Span in_span, List<StaticArg> in_values) {
    super(in_span);

    if (in_values == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'values' to the CompoundStaticArg constructor was null");
    }
    _values = in_values;
  }

    public CompoundStaticArg(Span span) {
        super(span);
        _values = null;
    }

  public List<StaticArg> getValues() { return _values; }

    /*
  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  protected abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  protected abstract int generateHashCode();
    */
}
