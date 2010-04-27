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
package com.sun.fortress.compiler.asmbytecodeoptimizer;

import com.sun.fortress.repository.ProjectProperties;

import java.io.*;
import java.util.ArrayList;
import java.util.jar.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.*;

class ByteCodeOptimizer {

    HashMap classes = new HashMap();
    HashMap files = new HashMap();

    void readInClassFile(String name) {
        System.out.println("readClassFile " + name);
        // Remove the .class and replace / with .
        String nname = name.substring(0, name.length() - 6).replace("/", ".");
        System.out.println("readClassFile " + name + " new = " + nname);
        try {
            ClassReader cr = new ClassReader(nname);
            ByteCodeVisitor bcv = new ByteCodeVisitor();
            cr.accept(bcv, 0);
            classes.put(name, bcv);
        } catch (Exception e) {
            System.out.println("ClassFormatError:"  + e );
            e.printStackTrace();
        }
    }

    void readInJarFile(String name) {
        try {
            JarInputStream jario = new JarInputStream(new FileInputStream(name));
            // How big can classes get?  10,000 is our limit for now.
            int bufsize = 100000;
            int bufchunk = 1000;
            JarEntry entry;
            int bytesread;

            while ((entry = jario.getNextJarEntry()) != null) {
                byte buf[] = new byte[bufsize];
                byte tempbuf[] = new byte[bufchunk];
                int bytepos = 0;
                int lastread = 0;

                while ((bytesread = jario.read(tempbuf, lastread, bufchunk)) > 0) {
                    System.arraycopy(tempbuf, 0, buf, bytepos, bytesread);
                    bytepos = bytepos + bytesread;
                }

                if (entry.getName().endsWith(".class")) {
                    ClassReader cr = new ClassReader(buf);
                    ByteCodeVisitor bcv = new ByteCodeVisitor();
                    cr.accept(bcv, 0);
                    classes.put(entry.getName(), bcv);
                } else {
                    byte rsbuf[] = new byte[bytepos];
                    System.arraycopy(buf, 0, rsbuf, 0, bytepos);
                    files.put(entry.getName(), rsbuf);
                }
            }
        } catch (Exception e) {
            System.out.println("ClassFormatError:"  + e );
            e.printStackTrace();
        }
    }


    void writeOutClassFile(String arg) {
        System.out.println("NYI");
    }

    void writeOutJarFile(String arg) {
        try {
            String outputFile = arg.replace("bytecode_cache", "optimizedbytecode_cache");
            String directoryName = outputFile.substring(0, outputFile.lastIndexOf("/"));
            ProjectProperties.ensureDirectoryExists(directoryName);
            System.out.println("Writing out jar file" + outputFile);

            JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            Iterator it = classes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                ByteCodeVisitor bcv = (ByteCodeVisitor) pairs.getValue();
                bcv.toAsm(jos);
            }

            it = files.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                byte[] buf = (byte[]) pairs.getValue();
                JarEntry entry = new JarEntry((java.lang.String) pairs.getKey());
                jos.putNextEntry(entry);
                jos.write(buf);
            }
            
            jos.close();
        } catch (Exception e) {
            System.out.println("ClassFormatError:"  + e );
            e.printStackTrace();
        }
    }

    void optimize() {
        Iterator it = classes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            ByteCodeVisitor bcv = (ByteCodeVisitor) pairs.getValue();
            AddOptimizeString.Optimize(bcv);
            BasicBlockGenerator.Optimize(bcv);
            // RemoveLiteralCoercions.Optimize(bcv);
        }
    }
        
    public static void main(String[] args) throws Exception {
        ByteCodeOptimizer bco = new ByteCodeOptimizer();

        for (String arg : args) {
            System.out.println("Reading in jar file " + arg);
            if (arg.endsWith(".jar")) {
                bco.readInJarFile(arg);
                bco.optimize();
                bco.writeOutJarFile(arg);
            } else if (arg.endsWith(".class")) {
                bco.readInClassFile(arg);
                bco.optimize();
                bco.writeOutClassFile(arg);
            }
            else throw new RuntimeException("Undefined File Type " + arg);
        }
    }
}


