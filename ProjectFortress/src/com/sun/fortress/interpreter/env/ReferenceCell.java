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

package com.sun.fortress.interpreter.env;

import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import dstm2.ContentionManager;
import dstm2.exceptions.AbortedException;
import dstm2.exceptions.PanicException;
import dstm2.factory.shadow.ReadSet;
import dstm2.factory.shadow.Recoverable;
import dstm2.Transaction;
import java.util.Set;


/**
 * What the interpreter stores mutable things (fields, variables)
 * in.  It will eventually acquire transactional semantics.
 */
public class ReferenceCell extends IndirectionCell {
    private FType theType;
    protected FNode node;
    Recoverable rnode;
    ContentionManager manager;
    Transaction writer;
    ReadSet readers;

    private final static String FORMAT = "Unexpected transaction state: %s";
    /**
     * A transaction switches to exclusive mode after being aborted this many times.
     */
    public static final int CONFLICT_THRESHOLD = 8;

    ReferenceCell(FType t, FValue v) {
        super();
        theType = t;
	node = new FNode(v);
	rnode = (Recoverable)node;
	manager = FortressTaskRunner.getContentionManager();
	writer = Transaction.COMMITTED;
	readers = new ReadSet();
    }

    ReferenceCell(FType t) {
        super();
        theType = t;
	node = new FNode();
	rnode = (Recoverable)node;
	manager = FortressTaskRunner.getContentionManager();
	writer = Transaction.COMMITTED;
	readers = new ReadSet();
    }

    ReferenceCell() {
        super();
	node = new FNode();
	rnode = (Recoverable)node;
	manager = FortressTaskRunner.getContentionManager();
	writer = Transaction.COMMITTED;
	readers = new ReadSet();
    }

    public void assignValue(FValue f2) {
	Transaction me  = FortressTaskRunner.getTransaction();
	Transaction other = null;
	Set<Transaction> others = null;
	while (true) {
	    synchronized (this) {
		others = readWriteConflict(me);
		if (others == null) {
		    other = openWrite(me);
		    if (other == null) {
			node.setValue(f2);
			return;
		    }
		}
	    }
	    if (others != null) {
		manager.resolveConflict(me, others);
	    } else if (other != null) {
		manager.resolveConflict(me, other);
	    }
	}
    }

    public FType getType() {
        return theType;
    }

    class FNode implements Recoverable {
	FValue val;
	FValue oldVal;

	void setValue(FValue value) { val = value;};
	FNode(FValue value) {val = value;}
	FNode() {}
	FValue getValue() { return val;}

	public void backup() { oldVal = val; }
	public void recover() { val = oldVal; }
    }

    public void storeValue(FValue f2) {
        if (node.getValue() != null)
            throw new InterpreterError("Internal error, second store of indirection cell");
	assignValue(f2);
    }

    public void storeType(FType f2) {
        if (theType != null)
            throw new InterpreterError("Internal error, second store of type");
        theType = f2;
    }

    public boolean isInitialized() {
        return node.getValue() != null;
    }

    public FValue getValue() {
	Transaction me  = FortressTaskRunner.getTransaction();
	Transaction other = null;
	while (true) {
	    synchronized (this) {
		other = openRead(me);
		//other = openWrite(me);
		if (other == null) {
		    FValue theValue = node.getValue();
		    if (theValue == null) {
			throw new ProgramError("Attempt to read uninitialized variable");
		    }
		    return theValue;
		}
	    }
	    manager.resolveConflict(me, other);
	}
    }

  /**
   * Tries to open object for reading. Returns reference to conflicting transaction, if one exists
   **/
  public Transaction openRead(Transaction me) {
    // don't try read sharing if contention seems high
    if (me == null) {	// restore object if latest writer aborted
      if (writer.isAborted()) {
        rnode.recover();
        writer = Transaction.COMMITTED;
      }
      return null;
    }
    if (me.attempts > CONFLICT_THRESHOLD) {
      return openWrite(me);
    }
    // Am I still active?
    if (!me.isActive()) {
      throw new AbortedException();
    }
    // Have I already opened this object?
    if (writer == me) {
      return null;
    }
    switch (writer.getStatus()) {
      case ACTIVE:
        return writer;
      case COMMITTED:
        break;
      case ABORTED:
        rnode.recover();
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = Transaction.COMMITTED;
    readers.add(me);
    manager.openSucceeded();
    return null;
  }
  
  /**
   * Tries to open object for reading.
   * Returns reference to conflicting transaction, if one exists
   **/
  Transaction openWrite(Transaction me) {
    boolean cacheHit = false;  // already open for read?
    // not in a transaction
    if (me == null) {	// restore object if latest writer aborted
      if (writer.isAborted()) {
        rnode.recover();
        writer = Transaction.COMMITTED;
      }
      return null;
    }
    if (!me.isActive()) {
      throw new AbortedException();
    }
    if (me == writer) {
      return null;
    }
    switch (writer.getStatus()) {
      case ACTIVE:
        return writer;
      case COMMITTED:
        rnode.backup();
        break;
      case ABORTED:
        rnode.recover();
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = me;
    if (!cacheHit) {
      me.memRefs++;
      manager.openSucceeded();
    }
    return null;
  }

  public Set<Transaction> readWriteConflict(Transaction me) {
    for (Transaction reader : readers) {
      if (reader.isActive() && reader != me) {
        return readers;
      }
    }
    readers.clear();
    return null;
  }

}
