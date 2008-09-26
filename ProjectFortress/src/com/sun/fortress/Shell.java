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

package com.sun.fortress;

import com.sun.fortress.repository.CacheBasedRepository;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;
import java.io.*;
import java.util.*;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;
import edu.rice.cs.plt.iter.IterUtil;

import com.sun.fortress.compiler.*;
import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.phases.PhaseOrder;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.syntax_abstractions.parser.PreParser;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.tools.FortressAstToConcrete;
import com.sun.fortress.nodes_util.ApiMaker;

public final class Shell {
    static boolean test;
    
    /* This object groups in one place all the public static flags that 
     * we had lying around before.  To set a flag on / off, one must 
     * invoke its corresonding static method in Shell to do so.  
     * This approach works better with JUnit.  We have seen weird behaviors 
     * when using public static variables with JUnit tests.
     */
    private static CompileProperties compileProperties = new CompileProperties();

    /* set this statically if you only want to run up to a certain phase */
    private static PhaseOrder finalPhase = PhaseOrder.CODEGEN;

    private final FortressRepository _repository;
    private static final CacheBasedRepository defaultRepository =
        new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR);
    public static FortressRepository CURRENT_INTERPRETER_REPOSITORY = null;

    public Shell(FortressRepository repository) { _repository = repository; }

    public FortressRepository getRepository() {
        return _repository;
    }

    private static void setPhase( PhaseOrder phase ){
        finalPhase = phase;
    }

    /**
     * This is used to communicate, clumsily, with parsers generated by syntax expansion.
     * The interface should be improved.
     */
    public static void setCurrentInterpreterRepository( FortressRepository g ){
        CURRENT_INTERPRETER_REPOSITORY = g;
    }

    private static GraphRepository specificRepository(Path p, CacheBasedRepository cache ){
        GraphRepository fr = new GraphRepository( p, cache );
        CURRENT_INTERPRETER_REPOSITORY = fr;
        return fr;
    }

    public static GraphRepository specificRepository(Path p) {
        return specificRepository( p, defaultRepository );
    }

    /* Helper method to print usage message.*/
    private static void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println(" compile [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" [run] [-debug [type]* [#]] somefile.fss arg...");
        System.err.println(" test [-debug [type]* [#]] somefile.fss...");
        System.err.println(" api [-out file] [-debug [type]* [#]] somefile.fss");
        System.err.println(" compare [-debug [type]* [#]] somefile.fss anotherfile.fss");
        System.err.println(" parse [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" unparse [-unqualified] [-unmangle] [-out file] [-debug [type]* [#]] somefile.tf{s,i}");
        System.err.println(" disambiguate [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" desugar [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" grammar [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" typecheck [-out file] [-debug [type]* [#]] somefile.fs{s,i}");
        System.err.println(" help");
    }

    private static void printHelpMessage() {
        System.err.println
        ("Invoked as script: fortress args\n"+
         "Invoked by java: java ... com.sun.fortress.Shell args\n"+
         "\n"+
         "fortress compile [-out file] [-debug [type]* [#]] somefile.fs{s,i}\n"+
         "  Compiles somefile. If compilation succeeds no message will be printed.\n"+
         "\n"+
         "fortress [run] [-debug [type]* [#]] somefile.fss arg ...\n"+
         "  Runs somefile.fss through the Fortress interpreter, passing arg ... to the\n"+
         "  run method of somefile.fss.\n"+
         "\n"+
         "fortress test [-debug [type]* [#]] somefile.fss ...\n"+
         "  Runs the functions with the test modifier in the specified components \n"+
         "\n"+
         "fortress api [-out file] [-debug [type]* [#]] somefile.fss\n"+
         "  Automatically generate an API from a component.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress compare [-debug [type]* [#]] somefile.fss anotherfile.fss\n"+
         "  Compare results of two components.\n"+
         "\n"+
         "fortress parse [-out file] [-debug [type]* [#]] somefile.fs{i,s}\n"+
         "  Parses a file. If parsing succeeds the message \"Ok\" will be printed.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress unparse [-unqualified] [-unmangle] [-out file] [-debug [type]* [#]] somefile.tf{i,s}\n"+
         "  Convert a parsed file back to Fortress source code. The output will be dumped to stdout if -out is not given.\n"+
         "  If -unqualified is given, identifiers are dumped without their API prefixes.\n"+
         "  If -unmangle is given, internally mangled identifiers are unmangled.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress disambiguate [-out file] [-debug [type]* [#]] somefile.fs{i,s}\n"+
         "  Disambiguates a file.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress desugar [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Desugars a file.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress grammar [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Rewrites syntax grammars in a file.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "\n"+
         "fortress typecheck [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Typechecks a file. If type checking succeeds no message will be printed.\n"+
         "\n"+
         "More details on each flag:\n"+
         "   -out file : dumps the processed abstract syntax tree to a file.\n"+
         "   -debug : enables debugging to the maximum level (99) for all \n"+
         "            debugging types and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number, \n"+
         "            and sets all debugging types on.\n"+
         "   -debug types : sets debugging types to the specified types with \n"+
         "            the maximum debugging level. \n" +
         "   -debug types # : sets debugging to the specified level, where # is a number, \n"+
         "            and the debugging types to the specified types. \n" +
         "   The acceptable debugging types are:\n"+
         "            " + Debug.typeStrings() + "\n"+
         "\n"
        );
    }

    public static boolean getPreparse() {
        return compileProperties.preparse;
    }

    public static boolean getTypeChecking() {
        return compileProperties.type_check;
    }

    public static boolean getObjExprDesugaring(){
        return compileProperties.objExpr_desugar;
    }

    public static boolean getGetterSetterDesugaring(){
        return compileProperties.getter_setter_desugar;
    }

    public static void setPreparse(boolean preparse) {
        compileProperties.preparse = preparse;
    }

    public static void setTypeChecking(boolean type_check) {
        compileProperties.type_check = type_check;
    }

    public static void setObjExprDesugaring(boolean desugar){
        compileProperties.objExpr_desugar = desugar;
    }

    public static void setGetterSetterDesugaring(boolean desugar){
        compileProperties.getter_setter_desugar = desugar;
    }

    /* Main entry point for the fortress shell.*/
    public static void main(String[] tokens) throws InterruptedException, Throwable {
        if (tokens.length == 0) {
            printUsageMessage();
            System.exit(-1);
        }

        // Now match the assembled string.
        try {
            String what = tokens[0];
            List<String> args = Arrays.asList(tokens).subList(1, tokens.length);
            if (what.equals("compile")) {
                setPhase( PhaseOrder.CODEGEN );
                compile(args, Option.<String>none());
            } else if (what.equals("run")) {
                setPhase( PhaseOrder.CODEGEN );
                run(args);
            } else if ( what.equals("api" ) ){
                api(args, Option.<String>none());
            } else if ( what.equals("compare" ) ){
                compare(args);
            } else if ( what.equals("parse" ) ){
                parse(args, Option.<String>none());
            } else if ( what.equals("unparse" ) ){
                unparse(args, Option.<String>none(), false, false);
            } else if ( what.equals( "disambiguate" ) ){
                setPhase( PhaseOrder.DISAMBIGUATE );
                compile(args, Option.<String>none());
            } else if ( what.equals( "desugar" ) ){
                setTypeChecking(true);
                setObjExprDesugaring(true);
                setPhase( PhaseOrder.DESUGAR );
                compile(args, Option.<String>none());
            } else if ( what.equals( "grammar" ) ){
                setPhase( PhaseOrder.GRAMMAR );
                compile(args, Option.<String>none());
            } else if (what.equals("typecheck")) {
                /* TODO: remove the next line once type checking is permanently turned on */
                setTypeChecking(true);
                setPhase( PhaseOrder.TYPECHECK );
                compile(args, Option.<String>none());
            } else if (what.equals("test")) {
                runTests(args);
            } else if (what.contains(ProjectProperties.COMP_SOURCE_SUFFIX)
                       || (what.startsWith("-") && tokens.length > 1)) {
                // no "run" command.
                setPhase( PhaseOrder.CODEGEN );
                run(Arrays.asList(tokens));
            } else if (what.equals("help")) {
                printHelpMessage();

            } else { printUsageMessage(); }
        }
        catch (UserError error) {
            System.err.println(error.getMessage());
        }
        catch (IOException error) {
            System.err.println(error.getMessage());
        }
    }

    /**
     * Automatically generate an API from a component.
     */
    private static void api(List<String> args, Option<String> out)
        throws UserError, InterruptedException, IOException {
        if (args.size() == 0) {
            throw new UserError("Need a file to generatoe an API.");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
                rest = Debug.parseOptions(rest);
            }
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList( 1, rest.size() );
            }
            if (s.equals("-noPreparse")) setPreparse(false);

            api( rest, out );
        } else {
            api( s, out );
        }
    }

    private static void api( String file, Option<String> out ){
        try{
            if ( isApi(file) ) {
                System.out.println( "Need a component file " +
                                    "instead of an API file." );
            } else if (isComponent(file)) {
                Component c = (Component) Parser.parseFile(cuName(file), new File(file));
                Api a = (Api) c.accept( ApiMaker.ONLY );
                String code = a.accept( new FortressAstToConcrete() );
                if ( out.isSome() ){
                    try{
                        BufferedWriter writer = Useful.filenameToBufferedWriter(out.unwrap());
                        writer.write(code);
                        writer.close();
                        System.out.println( "Dumped code to " + out.unwrap() );
                    } catch ( IOException e ){
                        System.err.println( "Error while writing " + out.unwrap() );
                    }
                } else {
                    System.out.println( code );
                }
            } else {
                System.out.println( "Don't know what kind of file " + file +
                                    " is. Append .fsi or .fss." );
            }
        } catch ( IOException i ){
            i.printStackTrace();
        } catch ( OptionUnwrapException o ){
            o.printStackTrace();
        }
    }

    /**
     * Evaluate a given component and returns its value.
     */
    public static FValue eval( String file )
        throws Throwable {
        return eval( file, new ArrayList<String>() );
    }

    public static FValue eval( String file, List<String> args )
        throws Throwable {
        setPhase( PhaseOrder.CODEGEN );
        if ( ! isComponent(file) )
            throw new UserError("A component file is expected to evaluate.");
        APIName name = trueApiName( file );
        Path path = sourcePath( file, name );
        GraphRepository bcr = specificRepository( path, defaultRepository );
        CompilationUnit cu = bcr.getLinkedComponent(name).ast();
        FValue result = Driver.runProgram(bcr, cu, args);
        bcr.deleteComponent(cu.getName());
        return result;
    }

    /**
     * Compare results of two components.
     */
    private static void compare(List<String> args)
        throws UserError, InterruptedException, IOException, Throwable {
        if (args.size() == 0) {
            throw new UserError("Need files to compare the results.");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
                rest = Debug.parseOptions(rest);
            }
            if (s.equals("-noPreparse")) setPreparse(false);;

            compare( rest );
        } else {
            if (args.size() == 1)
                throw new UserError("Need one more file to compare the results.");
            compare( s, args.get(1) );
        }
    }

    private static void compare( String first, String second )
        throws Throwable {
        try{
            if ( isApi(first) || isApi(second) ) {
                System.out.println( "Need component files " +
                                    "instead of API files." );
            } else if (isComponent(first) && isComponent(second)) {
                FValue value1 = eval( first );
                FValue value2 = eval( second );
                if (value1 == value2)
                    System.out.println( "Ok" );
                else System.out.println( "Failed: " +
                                         first + " evaluated to " + value1 +
                                         " but " +
                                         second + " evaluated to " + value2 );
            } else {
                System.out.println( "Don't know what kind of file is." +
                                    "Append .fsi or .fss." );
            }
        } catch ( IOException i ){
            i.printStackTrace();
        } catch ( OptionUnwrapException o ){
            o.printStackTrace();
        }
    }

    /**
     * Parse a file. If the file parses ok it will say "Ok".
     * If you want a dump then give -out somefile.
     */
    private static void parse(List<String> args, Option<String> out)
        throws UserError, InterruptedException, IOException {
        if (args.size() == 0) {
            throw new UserError("Need a file to parse");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
                rest = Debug.parseOptions(rest);
            }
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList( 1, rest.size() );
            }
            if (s.equals("-noPreparse")) setPreparse(false);

            parse( rest, out );
        } else {
            parse( s, out );
        }
    }

    private static void parse( String file, Option<String> out){
        try{
            CompilationUnit unit = Parser.parseFile(cuName(file), new File(file));
            System.out.println( "Ok" );
            if ( out.isSome() ){
                try{
                    ASTIO.writeJavaAst(unit, out.unwrap());
                    System.out.println( "Dumped parse tree to " + out.unwrap() );
                } catch ( IOException e ){
                    System.err.println( "Error while writing " + out.unwrap() );
                }
            }
        } catch (ProgramError e) {
            System.err.println(e.getMessage());
            e.printInterpreterStackTrace(System.err);
            if (Debug.isOnMax()) {
                e.printStackTrace();
            } else {
                System.err.println("Turn on -debug for Java-level error dump.");
            }
            System.exit(1);
        } catch ( FileNotFoundException f ){
            System.err.println( file + " not found" );
        } catch ( IOException ie ){
            System.err.println( "Error while reading " + file );
        } catch ( StaticError s ){
            System.err.println(s);
        }
    }

    /**
     * UnParse a file.
     * If you want a dump then give -out somefile.
     */
    private static void unparse(List<String> args,
                                Option<String> out,
                                boolean _unqualified,
                                boolean _unmangle)
        throws UserError, InterruptedException, IOException {
        if (args.size() == 0) {
            throw new UserError("Need a file to unparse");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());
        boolean unqualified = _unqualified;
        boolean unmangle = _unmangle;

        if (s.startsWith("-")) {
            if (s.equals("-unqualified")){
                unqualified = true;
            }
            if (s.equals("-unmangle")){
                unmangle = true;
            }
            if (s.equals("-debug")){
                rest = Debug.parseOptions(rest);
            }
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList( 1, rest.size() );
            }
            if (s.equals("-noPreparse")) setPreparse(false);

            unparse( rest, out, unqualified, unmangle );
        } else {
            unparse( s, out, unqualified, unmangle );
        }
    }

    private static void unparse( String file,
                                 Option<String> out,
                                 boolean unqualified,
                                 boolean unmangle ){
        try{
            String code = ASTIO.readJavaAst( file ).unwrap().accept( new FortressAstToConcrete(unqualified,unmangle) );
            if ( out.isSome() ){
                try{
                    BufferedWriter writer = Useful.filenameToBufferedWriter(out.unwrap());
                    writer.write(code);
                    writer.close();
                } catch ( IOException e ){
                    System.err.println( "Error while writing " + out.unwrap() );
                }
            } else {
                System.out.println( code );
            }
        } catch ( IOException i ){
            i.printStackTrace();
        } catch ( OptionUnwrapException o ){
            o.printStackTrace();
        }
    }

    private static boolean isApi(String file){
        return file.endsWith(ProjectProperties.API_SOURCE_SUFFIX);
    }

    private static boolean isComponent(String file){
        return file.endsWith(ProjectProperties.COMP_SOURCE_SUFFIX);
    }

    private static APIName cuName( String file ){
        if ( file.endsWith( ProjectProperties.COMP_SOURCE_SUFFIX ) ||
             file.endsWith( ProjectProperties.API_SOURCE_SUFFIX ) ){
            return NodeFactory.makeAPIName(file.substring( 0, file.lastIndexOf(".") ));
        }
        return NodeFactory.makeAPIName(file);
    }

    public static boolean checkCompilationUnitName(String filename,
                                                   String cuname) {
        String file = filename.substring( 0, filename.lastIndexOf(".") );
        file = file.replace('/','.');
        file = file.replace('\\','.');
        String regex = "(.*\\.)?" + cuname.replaceAll("\\.", "\\.") + "$";
        Debug.debug( Debug.Type.REPOSITORY, 3, "Checking file name " + file + " vs cuname " + regex );
        return file.matches( regex );
    }

    /**
     * Compile a file.
     * If you want a dump then give -out somefile.
     */
    private static void compile(List<String> args, Option<String> out)
        throws UserError, InterruptedException, IOException {
        if (args.size() == 0) {
            throw new UserError("Need a file to compile");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
            	rest = Debug.parseOptions(rest);
            }
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList( 1, rest.size() );
            }
            if (s.equals("-noPreparse")) setPreparse(false);
            compile(rest, out);
        } else {
            try {
                APIName name = trueApiName( s );
                Path path = sourcePath( s, name );

                String file_name = name.toString() + (s.endsWith(".fss") ? ".fss" : ".fsi");
                Iterable<? extends StaticError> errors = compile(path, file_name, out );
                int num_errors = IterUtil.sizeOf(errors);
                if ( !IterUtil.isEmpty(errors) ) {
                    for (StaticError error: errors) {
                        System.err.println(error);
                    }
                    String err_string = "File " + file_name + " compiled with " + num_errors + " error" +
                        (num_errors == 1 ? "." : "s.");
                    System.err.println(err_string);
                }

            } catch (RepositoryError error) {
                System.err.println(error);
            }
        }
    }

    private static APIName trueApiName( String path ) throws IOException {
        return PreParser.apiName( NodeFactory.makeAPIName(path), new File(path).getCanonicalFile() );
    }

    /**
     * Compile a file.
     */
    public static Iterable<? extends StaticError> compile(Path path, String file) {
        return compile(path, file, Option.<String>none());
    }

    private static Iterable<? extends StaticError> compile(Path path, String file,
                                                           Option<String> out) {
        GraphRepository bcr = specificRepository( path, defaultRepository );

        Debug.debug( Debug.Type.FORTRESS, 2, "Compiling file ", file );
        APIName name = cuName(file);
        try {
            if ( isApi(file) ) {
                Api a = (Api) bcr.getApi(name).ast();
                if ( out.isSome() )
                    ASTIO.writeJavaAst(defaultRepository.getApi(name).ast(), out.unwrap());
            } else if (isComponent(file)) {
                Component c = (Component) bcr.getComponent(name).ast();
                if ( out.isSome() ) {
                    ASTIO.writeJavaAst(defaultRepository.getComponent(name).ast(), out.unwrap());
                    ASTIO.deleteJavaAst( CacheBasedRepository.cachedCompFileName(ProjectProperties.ANALYZED_CACHE_DIR, name) );
                    bcr.deleteComponent( name );
                }
            } else {
                System.out.println( "Don't know what kind of file " + file +
                                    " is. Append .fsi or .fss." );
            }
        } catch (ProgramError pe) {
            Iterable<? extends StaticError> se = pe.getStaticErrors();
            if (se == null) {
                return IterUtil.singleton(new WrappedException(pe, Debug.isOnMax()));
            }
            else {
                return se;
            }
        } catch (RepositoryError ex) {
            throw ex;
        } catch ( FileNotFoundException ex ){
            throw new WrappedException(ex);
        } catch ( IOException e ){
            throw new WrappedException(e);
        }
        catch ( MultipleStaticError ex) {
            List<StaticError> result = new LinkedList<StaticError>();
            for( StaticError err : ex ) {
                result.add(new WrappedException(err, Debug.isOnMax()));
            }
            return result;
        }
        catch (StaticError ex) {
             return IterUtil.singleton(new WrappedException(ex, Debug.isOnMax()));
        } finally {
             bcr.deleteComponent(name);
        }

        if (bcr.verbose())
            System.err.println("Compiling done.");

        return IterUtil.empty();
    }

    /**
     * Run a file.
     */
    private static void run(List<String> args)
        throws UserError, IOException, Throwable {
        if (args.size() == 0) {
            throw new UserError("Need a file to run");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
            	rest = Debug.parseOptions(rest);
            }
            if (s.equals("-noPreparse")) setPreparse(false);

            run(rest);
        } else {
            run(s, rest);
        }
    }

    private static void run(String fileName, List<String> args)
        throws UserError, Throwable {
        try {
            Iterable<? extends StaticError> errors = IterUtil.empty();
            try {
                eval( fileName, args );
            } catch (Throwable th) {
                // TODO FIXME what is the proper treatment of errors/exceptions etc.?
                if (th instanceof FortressException) {
                    FortressException pe = (FortressException) th;
                    if (pe.getStaticErrors() != null)
                        errors = pe.getStaticErrors();
                }
                if (th instanceof RuntimeException)
                    throw (RuntimeException) th;
                if (th instanceof Error)
                    throw (Error) th;
                throw new WrappedException(th, Debug.isOnMax());
            }

            for (StaticError error: errors) {
                System.err.println(error);
            }
            // If there are no errors,
            // all components will have been written to disk
            // by the CacheBasedRepository.
        } catch ( StaticError e ){
            System.err.println(e);
            if ( Debug.isOnMax() ){
                e.printStackTrace();
            }
        } catch (RepositoryError e) {
            System.err.println(e.getMessage());
        } catch (FortressException e) {
            System.err.println(e.getMessage());
            e.printInterpreterStackTrace(System.err);
            if (Debug.isOnMax()) {
                e.printStackTrace();
            } else {
                System.err.println("Turn on -debug for Java-level error dump.");
            }
            System.exit(1);
        }
    }

    /* find the api name for a file and chop it off the source path.
     * what remains from the source path is the directory that contains
     * the file( including sub-directories )
     */
    private static Path sourcePath( String file, APIName name ) throws IOException {
        Debug.debug( Debug.Type.REPOSITORY, 2, "True api name is " + name );
        String fullPath = new File(file).getCanonicalPath();
        Debug.debug( Debug.Type.REPOSITORY, 2, "Path is " + fullPath );
        Path path = ProjectProperties.SOURCE_PATH;
        /* the path to the file is /absolute/path/a/b/c/foo.fss and the apiname is
         * a.b.c.foo, so we need to take off the apiname plus four more characters,
         * ".fss" or ".fsi"
         */
        String source = fullPath.substring( 0, fullPath.length() - (name.toString().length() + 4) );
        path = path.prepend( source );
        Debug.debug( Debug.Type.REPOSITORY, 2, "Source path is " + source );
        Debug.debug( Debug.Type.REPOSITORY, 2, "Lookup path is " + path );
        return path;
    }

    private static void runTests(List<String> files) 
        throws UserError, Throwable {
        setPhase( PhaseOrder.CODEGEN );
        for (String file : files) {
            if ( ! isComponent(file) )
                throw new UserError("A component file is expected to evaluate.");
            APIName name = trueApiName( file );
            Path path = sourcePath( file, name );
            GraphRepository bcr = specificRepository( path, defaultRepository );
            CompilationUnit cu = bcr.getLinkedComponent(name).ast();
            Driver.runTests(bcr, cu);
        }
    }

    /* run all the analysis available */
    public static AnalyzeResult analyze(final FortressRepository repository,
                                        final GlobalEnvironment env,
                                        List<Api> apis,
                                        List<Component> components,
                                        final long lastModified) throws StaticError {
    	AnalyzeResult result = finalPhase.makePhase(repository,env,apis,components,lastModified).run();
    	return result;
    }


    /**
     * The fields of this class serve as temporary switches used for testing.
     */
    private static class CompileProperties {
        boolean preparse = true;    // run syntax abstraction or not
        boolean type_check = false; // run type checker or not
        boolean objExpr_desugar = false; // run obj expression desugaring or not
        boolean getter_setter_desugar = true; // run getter/setter desugaring or not
    }

}
