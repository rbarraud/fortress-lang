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

package com.sun.fortress.ant_tasks;

import com.sun.fortress.interpreter.drivers.fs;

import java.io.File;
import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;

public class FortressTask extends Task {
    private Vector<FileSet> filesets = new Vector<FileSet>();

    private boolean ast = false;
    private boolean keep = false;
    private boolean pause = false;
    private boolean parseOnly = false;
    private boolean libraryTest = false;
    private boolean verbose = false;
    private boolean test = false;

    public void setAst(boolean val) { ast = val; }
    public void setKeep(boolean val) { keep = val; }
    public void setPause(boolean val) { pause = val; }
    public void setParseOnly(boolean val) { parseOnly = val; }
    public void setLibraryTest(boolean val) { libraryTest = val; }
    public void setVerbose(boolean val) { verbose = val; }
    public void setTest(boolean val) { test = val; }

    private StringBuffer buildOptions() {
        StringBuffer result = new StringBuffer();

        if (ast) { result.append(" -ast "); }
        if (keep) { result.append(" -keep "); }
        if (pause) { result.append(" -pause "); }
        if (parseOnly) { result.append(" -parseOnly "); }
        if (libraryTest) { result.append(" -libraryTest "); }
        if (verbose) { result.append("-v"); }
        if (test) { result.append("-t"); }

        return result;
    }

    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    public void execute() {
        try {
            boolean failures = false; 
            
            for (FileSet fileSet : filesets) {
                DirectoryScanner dirScanner = fileSet.getDirectoryScanner(getProject());
                String[] includedFiles = dirScanner.getIncludedFiles();
                for (String fileName : includedFiles) {
                    StringBuffer options = buildOptions();
                    options.append(dirScanner.getBasedir() + File.separator + fileName);
                    
                    log("fortress" + options.toString());
                    
                    Process fortressProcess = Runtime.getRuntime().exec (
                        "fortress" + 
                        options.toString()
                    );
                    int exitValue = fortressProcess.waitFor();
                    if (exitValue != 0) {
                        failures = true;
                        InputStream errors = fortressProcess.getErrorStream();
                        Writer out = new BufferedWriter(new OutputStreamWriter(System.err));
                        while (errors.available() != 0) {
                            out.write(errors.read());
                        }
                        out.flush();
                    }
                }
            }
            if (failures) { 
                throw new RuntimeException("FORTRESS FAILED ON SOME FILES. SEE ABOVE ERROR MESSAGES FOR DETAILS."); 
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
