/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.tests.unit_tests;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.Shell;
import com.sun.fortress.interpreter.Driver;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.useful.StreamForwarder;
import com.sun.fortress.useful.StringMap;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.WireTappedPrintStream;

import edu.rice.cs.plt.iter.IterUtil;

public class FileTests {

    public static class BaseTest extends TestCase {
        /**
         * Directory-qualified file name
         */
        String f;
        /**
         * Just the directory.
         */
        String dir;
        /**
         * The name of the test.
         */
        String name;
        /**
         * The search path for analysis, compilation, etc.
         */
        String path;

        /**
         * If true, only print test output for unexpected results.
         */
        final boolean unexpectedOnly;
        final boolean knownFailure;
        final boolean shouldFail;

        final boolean printSuccess;
        final boolean printFailure;

        public BaseTest(String path, String d, String s, boolean unexpected_only,
                boolean knownFailure,
                boolean shouldFail) {
            super("testFile");
            this.f = d + "/" + s;
            this.dir = d;
            this.path = path;
            this.name = s;
            this.unexpectedOnly = unexpected_only;

            this.printSuccess = !unexpected_only || knownFailure;
            this.printFailure = !unexpected_only || !knownFailure;
            this.knownFailure = knownFailure;
            this.shouldFail = shouldFail;
        }

        public String getName() {
            return f;
        }

        /**
         * Returns true if this test should be regarded as a "Failure",
         * regardless of the XXX test name or not.  This can be used to
         * test that a particular exception was thrown, for example; not only
         * should (say) XXXbadNumber thrown an exception, it should throw
         * a NumberFormatException.  Thus, the exc string could be tested
         * to see that it contains "NumberFormatException".
         *
         *
         * @param out
         * @param err
         * @param exc
         * @return
         */
        public  String testFailed(String out, String err, String exc) {
            return null;
        }

        /**
         * Looks for properties of the form
         * compile/link/run_
         * out/err/exceptions_
         * contains/matches/WImatches/equals/WCIequals,
         * returns true if an expected condition fails.
         *
         * @param pfx
         * @param props
         * @param out
         * @param err
         * @param exc
         * @return
         */
        protected String generalTestFailed(String pfx, StringMap props, String out, String err, String exc) {
            String s = null;

            if (s == null)
                s = generalTestFailed(pfx, props, "out",  out);

            if (s == null)
                s = generalTestFailed(pfx, props, "err",  err);

            if (s == null)
                s = generalTestFailed(pfx, props, "exception",  exc) ;
            return s;

        }

        private String generalTestFailed(String pfx, StringMap props,
                String which, String contents) {
            String what;
            String test;

            boolean any_check = false;

            what = pfx+which+"_contains";
            test = props.get(what);
            test = ProjectProperties.get(test);
            if (test != null && test.length() > 0) {
                if (!contents.contains(test))
                    return what+"="+test;
                any_check = true;
            }

            what = pfx+which+"_matches";
            test = props.get(what);
            test = ProjectProperties.get(test);
            if (test != null && test.length() > 0) {
                if (!contents.matches(test))
                    return what+"="+test;
                any_check = true;
           }

            what = pfx+which+"_WImatches";
            test = props.get(what);
            test = ProjectProperties.get(test);
            if (test != null && test.length() > 0) {
                String wi_contents = contents.replaceAll("\\s+", " ").trim();
                if (!wi_contents.matches(test))
                    return what+"="+test;
                any_check = true;
           }

            what = pfx+which+"_WCIequals";
            test = props.get(what);
            test = props.getCompletely(test);
            if (test != null && test.length() > 0) {
                String wci_contents = contents.replaceAll("\\s+", " ").trim();
                String wci_test = test.replaceAll("\\s+", " ").trim();
                if (!wci_contents.equalsIgnoreCase(wci_test))
                    return what+"="+test;
                any_check = true;
            }

            what = pfx+which+"_equals";
            test = props.get(what);
            test = props.getCompletely(test);
            if (test != null && test.length() > 0) {

                String wi_contents = contents.replaceAll("[ \\\t]+", " ");
                String wi_test = test.replaceAll("[ \\\t]+", " ");
                // Convert Windows CRLF to UNIX LF
                wi_contents = wi_contents.replaceAll("\\\r\\\n","\n");
                wi_test = wi_test.replaceAll("\\\r\\\n","\n");
                // Convert Mac CR to UNIX LF
                wi_contents = wi_contents.replaceAll("\\\r","\n");
                wi_test = wi_test.replaceAll("\\\r","\n");

                if (!wi_contents.equals(wi_test)) {
                    if (wi_contents.trim().equals(wi_test.trim())) {
                        // It is a leading/trailing whitespace problem....
                        char c0 = wi_contents.charAt(0);
                        char t0 = wi_test.charAt(0);
                        char cN = wi_contents.charAt(wi_contents.length()-1);
                        char tN = wi_test.charAt(wi_test.length()-1);
                        String problem = "";
                        if (c0 == ' ' || c0 == '\n') {
                            if (t0 == ' ' || t0 == '\n') {

                            } else {
                                problem += "text began with unexpected whitespace";
                            }
                        } else {
                            if (t0 == ' ' || t0 == '\n') {
                                problem += "text began without expected whitespace";
                            } else {

                            }
                        }

                        String problemAnd = problem.length() > 0 ? problem + " and " : problem;

                        if (cN == ' ' || cN == '\n') {
                            if (tN == ' ' || tN == '\n') {

                            } else {
                                problem = problemAnd + "text ended with unexpected whitespace";
                            }
                        } else {
                            if (tN == ' ' || tN == '\n') {
                                problem = problemAnd + "text ended without expected whitespace";
                            } else {

                            }
                        }

                        return what + ": " + problem;
                    } else if (wi_contents.replaceAll("\\s+", " ").trim().equals(wi_test.replaceAll("\\s+", " ").trim())) {
                        String cL = wi_contents.replaceAll("\n[ \t]+", "\n").trim();
                        String cT = wi_contents.replaceAll("[ \t]+\n", "\n").trim();
                        String tL = wi_test.replaceAll("\n[ \t]+", "\n").trim();
                        String tT = wi_test.replaceAll("[ \t]+\n", "\n").trim();

                        if (cL.equals(tL))
                            return what + ": different LEADING whitespace on some line(s)";

                        else if (cT.equals(tT))
                            return what + ": different TRAILING whitespace on some line(s)";

                        else if (wi_contents.replaceAll("[ \\\t]*\\\n[ \\\t]*", "\n").trim().equals(wi_test.replaceAll("[ \\\t]*\\\n[ \\\t]*", "\n").trim()))
                            return what + ": different LEADING AND TRAILING whitespace on some line(s)";

                        return what + ": some sort of an internal whitespace problem (linebreaks?)";
                    }
                    return what+"="+test;
                }
                any_check = true;
           }

            if (!any_check && pfx.equals("run_") && which.equals("out")) {
                // If there is no check specified on run_out, demand that it
                // contain "pass" or "PASS".
                if (!(contents.contains("pass") || contents.contains("PASS"))) {
                    return "default check run_out_contains=PASS";
                }
            }

            return null;
        }

    }

    /**
     * For tests that are applied to a single source file (or are rooted at a
     * single source file), this takes care of checking for success/failure,
     * what is expected, printing, etc.
     */
    public abstract static class SourceFileTest extends BaseTest {
        public SourceFileTest(String path, String d, String s,
                boolean unexpected_only,
                boolean knownFailure,
                boolean shouldFail) {
            super(path, d, s, unexpected_only, knownFailure, shouldFail);
        }

        public abstract String tag();

         public void testFile() throws Throwable {
            // Useful when a test is running forever
            //            System.out.println(this.name);
            //            System.out.flush();

            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            WireTappedPrintStream wt_err =
                WireTappedPrintStream.make(System.err, unexpectedOnly);
            WireTappedPrintStream wt_out =
                WireTappedPrintStream.make(System.out, unexpectedOnly);
            System.setErr(wt_err);
            System.setOut(wt_out);


            long start = System.nanoTime();
            String fssFile = f + ".fss";
            int rc = 0;

            try {
                //BufferedReader in = null;
                try {
                    oldOut.print(" " + tag() + " ") ; oldOut.print(f); oldOut.print(" "); oldOut.flush();
                    //in = Useful.utf8BufferedFileReader(fssFile);

                    rc = justTheTest();
                }
                finally {
                    System.setErr(oldErr);
                    System.setOut(oldOut);
                    //if (in != null)
                    //    in.close();
                }
            }
            catch (Throwable ex) {
                String outs = wt_out.getString();
                String errs = wt_err.getString();
                String exFirstLine = ex.toString();
                String trueFailure = testFailed(outs, errs, exFirstLine);
                if (f.contains("XXX")) {
                    if (trueFailure != null) {
                        unexpectedExceptionBoilerplate(wt_err, wt_out, ex, " Did not satisfy " + trueFailure);
                        return;

                    } else {
                        // "Failed", but correctly
                        // !unexpectedOnly || expectFailure
                        wt_err.flush(printSuccess);
                        wt_out.flush(printSuccess);
                        int crLoc = exFirstLine.indexOf("\n");
                        if (crLoc == -1) crLoc = exFirstLine.length();
                        exFirstLine = exFirstLine.substring(0, crLoc);
                        if (printSuccess)
                            System.out.println(exFirstLine);
                        System.out.println(" OK Saw expected exception");
                        return;
                    }
                }
                else {
                    unexpectedExceptionBoilerplate(wt_err, wt_out, ex, " UNEXPECTED exception ");
                    // return;
                }
            } finally {
                            //fr.clear();
            }

            /* Come here IFF NO EXCEPTIONS, to analyze output */

            String outs = wt_out.getString();
            String errs = wt_err.getString();

            boolean anyFails = outs.contains("fail") || outs.contains("FAIL") ||
                      errs.contains("fail") || errs.contains("FAIL") || rc != 0;

            String trueFailure = testFailed(outs, errs, "");

            if (shouldFail) {
                // NOTE expect to see this on STANDARD OUTPUT, not ERROR.
                if (anyFails && trueFailure == null) {
                    wt_err.flush(printSuccess);
                    wt_out.flush(printSuccess);
                    // Saw a failure, that is good.
                    System.out.println(" Saw expected failure" );
                } else {
                    if (printFailure) System.out.println();
                    wt_err.flush(printFailure);
                    wt_out.flush(printFailure);
                    if (trueFailure != null) {
                        System.out.println(" Saw failure, but did not satisfy " + trueFailure);
                        // Expected exception, saw none.
                        fail("Saw wrong failure.");
                    } else {
                        System.out.println(" Missing expected failure " );
                        // Expected exception, saw none.
                        fail("Expected failure or exception, saw none.");
                    }
                }
            } else {
                // This logic is a little confusing.
                // Failure is failure.  TrueFailure contains the better message.
                if (anyFails && trueFailure == null)
                    trueFailure = "FAIL or fail should not appear in output";

                long duration = (System.nanoTime() - start) / 1000000;
                    System.out.println(trueFailure != null ? " FAIL" : " OK (time = " + duration + "ms)");
                    wt_err.flush(trueFailure != null ? printFailure : printSuccess);
                    wt_out.flush(trueFailure != null ? printFailure : printSuccess);

                    assertTrue("Must satisfy " + trueFailure, trueFailure == null);

            }
        }

        /**
         * @param wt_err
         * @param wt_out
         * @param ex
         * @param s
         * @throws IOException
         */
        private void unexpectedExceptionBoilerplate(
                WireTappedPrintStream wt_err, WireTappedPrintStream wt_out,
                Throwable ex, String s) throws IOException {
            if (printFailure) System.out.println();
            wt_err.flush(printFailure);
            wt_out.flush(printFailure);
            if (printFailure) {
                System.out.println(s);
                ex.printStackTrace();
                fail();
            } else {
                System.out.println(s);
                fail(ex.getMessage());
            }
        }

        abstract protected int justTheTest() throws FileNotFoundException, IOException, Throwable;

    }

    public static class TestTest extends BaseTest {

        private final StringMap props;

        public TestTest(StringMap props, String path, String d, String s,
                boolean unexpected_only, boolean knownFailure, boolean shouldFail) {
            super(path, d, s, unexpected_only, knownFailure, shouldFail);
            this.props = props;
        }

        public void testFile() throws Throwable {
            String scriptName = ProjectProperties.FORTRESS_AUTOHOME
                    + "/bin/run";
            Runtime runtime = Runtime.getRuntime();
            System.out.print(" run  ");
            System.out.print(f);
            System.out.print(" ");
            System.out.flush();

            ArrayList<String> commandAndArgs = new ArrayList<String>();
	    commandAndArgs.add(scriptName);
	    commandAndArgs.add(name);
            for (int i=1; i < Integer.MAX_VALUE; i++) {
		String s = props.get("arg"+String.valueOf(i));
		if (s == null) {
		    break;
		}
		commandAndArgs.add(s);
	    }


            ProcessBuilder pb = new ProcessBuilder(commandAndArgs);
            Map<String, String> env = pb.environment();
            if (!env.containsKey("FORTRESS_HOME")) {
                env.put("FORTRESS_HOME", ProjectProperties.FORTRESS_AUTOHOME);
            }
            Process p = pb.start();
            InputStream err = p.getErrorStream();
            InputStream out = p.getInputStream();
            OutputStream in = p.getOutputStream();
            in.close();

            ByteArrayOutputStream cached_err = new ByteArrayOutputStream();
            ByteArrayOutputStream cached_out = new ByteArrayOutputStream();

            StreamForwarder f_out = new StreamForwarder(out, cached_out, true);
            StreamForwarder f_err = new StreamForwarder(err, cached_err, true);

            f_out.join();
            f_err.join();
            int exitValue = p.waitFor();
            String s_out = cached_out.toString();
            String s_err = cached_err.toString();

            boolean fail_exit = exitValue != 0;
            // We've decided that exit codes are definitive.
            boolean fail_out = false && s_out.contains("FAIL");
            boolean fail_err = false && s_err.contains("FAIL");
            boolean failed = fail_exit || fail_out || fail_err;
            String fail_cause = !failed ? ""
                    : ((fail_exit ? "Exit code != 0" : "")
                            + (fail_out ? " Stdout contained FAIL" : "") + (fail_err ? " Stderr contained FAIL"
                            : "")).trim();

            String trueFailure = testFailed(s_out, s_err, "");

            if (trueFailure != null) {
                fail_cause = "Failed to satisfy " + trueFailure;
                failed = true;
            }

            // OY, pass/fail dsylexia here.

            if (failed && printFailure || !failed && printSuccess) {
                System.out.print(s_out);
                System.out.print(s_err);
            }

            if (trueFailure != null) {
                System.out.println("Failed to satisfy " + trueFailure);
                fail();

            } else if (shouldFail != failed) {
                System.out.println(failed ? "UNEXPECTED failure (" + fail_cause
                        + ")" : "Did not see expected failure");
                fail();
            } else {
                System.out.println(failed ? "Saw expected failure ("
                        + fail_cause + ")" : "Passed");
            }
        }

        public String testFailed(String out, String err, String exc) {
            return generalTestFailed("run_", props, out, err, exc);

        }
    }



    public static class ShellTest extends BaseTest {

        public ShellTest(String path, String d, String s, boolean unexpected_only, boolean knownFailure) {
            super(path, d, s, unexpected_only, knownFailure, s.startsWith("XXX") );
        }

        public void testFile() throws Throwable {
            String scriptName = f + ".sh";
            Runtime runtime = Runtime.getRuntime();
            System.out.print("  ") ; System.out.print(f); System.out.print(" "); System.out.flush();

            ProcessBuilder pb = new ProcessBuilder(scriptName);
            Map<String, String> env = pb.environment();
            if (! env.containsKey("FORTRESS_HOME")) {
                env.put("FORTRESS_HOME", ProjectProperties.FORTRESS_AUTOHOME);
            }
            Process p = pb.start();
            InputStream err = p.getErrorStream();
            InputStream out = p.getInputStream();
            OutputStream in = p.getOutputStream();
            in.close();

            ByteArrayOutputStream cached_err = new ByteArrayOutputStream();
            ByteArrayOutputStream cached_out = new ByteArrayOutputStream();

            StreamForwarder f_out = new StreamForwarder(out, cached_out, true);
            StreamForwarder f_err = new StreamForwarder(err, cached_err, true);

                f_out.join();
                f_err.join();
                int exitValue = p.waitFor();
                String s_out = cached_out.toString();
                String s_err = cached_err.toString();

                boolean fail_exit = exitValue != 0;
                // We've decided that exit codes are definitive.
                boolean fail_out =  false && s_out.contains("FAIL");
                boolean fail_err =  false && s_err.contains("FAIL");
                boolean failed = fail_exit || fail_out || fail_err;
                String fail_cause = !failed ? "" : (
                        (fail_exit ? "Exit code != 0" : "") +
                        (fail_out ? " Stdout contained FAIL" : "") +
                        (fail_err ? " Stderr contained FAIL" : "")
                ).trim();

                if (failed && printFailure || !failed && printSuccess) {
                    System.out.print(s_out);
                    System.out.print(s_err);
                }

                if (shouldFail != failed) {
                    System.out.println(failed ? "UNEXPECTED failure ("+ fail_cause+")" : "Did not see expected failure");
                    fail();
                } else {
                    System.out.println(failed ? "Saw expected failure ("+ fail_cause+")" : "Passed");
                }
        }
    }

    private static String makeTestFileName(String name) {
        if (name.endsWith(".fss") || name.endsWith(".fsi") ) {
            return name;
        } else return name+".fss";
    }

    public static class CompileTest extends SourceFileTest {

        private final StringMap props;
//        public CompileTest(StringMap props, String path, String d, String s,
//                boolean unexpected_only, boolean knownFailure) {
//            super(path, d, s, unexpected_only, knownFailure, s.startsWith("XXX") );
//            this.props = props;
//        }
        public CompileTest(StringMap props, String path, String d, String s,
                boolean unexpected_only, boolean knownFailure, boolean shouldFail) {
            super(path, d, s, unexpected_only, knownFailure, shouldFail );
            this.props = props;
        }

        @Override
        protected int justTheTest()
                throws FileNotFoundException, IOException, Throwable {
            String[] tokens = {"compile", dir+"/"+makeTestFileName(name)};
            int rc = com.sun.fortress.Shell.subMain(tokens);
            return rc;

        }

        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "compile";
        }

        public  String testFailed(String out, String err, String exc) {
            return generalTestFailed("compile_", props, out, err, exc);
        }

    }

    public static class TypeCheckTest extends SourceFileTest {

        private final StringMap props;
        public TypeCheckTest(StringMap props, String path, String d, String s,
                boolean unexpected_only, boolean knownFailure, boolean shouldFail) {
            super(path, d, s, unexpected_only, knownFailure, shouldFail );
            this.props = props;
        }

        @Override
        protected int justTheTest()
                throws FileNotFoundException, IOException, Throwable {
            String[] tokens = {"typecheck-scala", dir+"/"+makeTestFileName(name)};
            int rc = com.sun.fortress.Shell.subMain(tokens);
            return rc;

        }

        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "typecheck";
        }

        public  String testFailed(String out, String err, String exc) {
            return generalTestFailed("typecheck_", props, out, err, exc);
        }

    }

    public static class DesugarTest extends SourceFileTest {

        private final StringMap props;
        public DesugarTest(StringMap props, String path, String d, String s,
                           boolean unexpected_only, boolean knownFailure, boolean shouldFail) {
            super(path, d, s, unexpected_only, knownFailure, shouldFail );
            this.props = props;

        }

        @Override
        protected int justTheTest()
            throws FileNotFoundException, IOException, Throwable {
            // might need to strip the .fss off f "f".
            String[] tokens = {"desugar", dir+"/"+makeTestFileName(name)};
            int rc = com.sun.fortress.Shell.subMain(tokens);
            return rc;
        }

        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "desugar";
        }

        public  String testFailed(String out, String err, String exc) {
            return generalTestFailed("desugar_", props, out, err, exc);
        }

    }

    public static class LinkTest extends SourceFileTest {

        private final StringMap props;
       public LinkTest(StringMap props, String path, String d, String s,
                boolean unexpected_only, boolean knownFailure, boolean shouldFail) {
            super(path, d, s, unexpected_only, knownFailure, shouldFail );
            this.props = props;

        }

        @Override
        protected int justTheTest()
                throws FileNotFoundException, IOException, Throwable {
            // might need to strip the .fss off f "f".
            String[] tokens = {"link", dir+"/"+makeTestFileName(name)};
            int rc = com.sun.fortress.Shell.subMain(tokens);
            return rc;


        }

        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "link";
        }

        public  String testFailed(String out, String err, String exc) {
            return generalTestFailed("link_", props, out, err, exc);

        }

    }

    public static class InterpreterTest extends SourceFileTest {

        public InterpreterTest(String path, String d, String s, boolean unexpected_only, boolean knownFailure) {
            super(path, d, s, unexpected_only, knownFailure, s.startsWith("XXX"));
        }


        /**
         * @param repository
         * @param apiname
         * @throws FileNotFoundException
         * @throws IOException
         * @throws Throwable
         */
        protected int justTheTest()
                throws FileNotFoundException, IOException, Throwable {
            String s = f.replaceFirst(".*/", "");
            APIName apiname = NodeFactory.makeAPIName(NodeFactory.testSpan, s);
            GraphRepository repository = Shell.specificInterpreterRepository( ProjectProperties.SOURCE_PATH.prepend(path) );

            ComponentIndex ci = repository.getLinkedComponent(apiname);

            //Option<CompilationUnit> _p = ASTIO.parseToJavaAst(fssFile, in, false);

            {
                Component p = (Component) ci.ast();

                // oldOut.print(" RUNNING"); oldOut.flush();
                if (!unexpectedOnly) System.out.println();
                // A demo file requiring arguments
                if (name.equals("tennisRanking")) {
                    ArrayList<String> args = new ArrayList<String>();
                    args.add(dir + "/tennis050307");
                    args.add(dir + "/tennis051707");
                    args.add(dir + "/tennisGames");
                    Driver.runProgram(repository, ci, args);
                }
                // Test files requiring "test" command
                else if (name.equals("XXXTestTest") ||
                         name.equals("natInference0") ||
                         name.equals("testTest1") ||
                         name.equals("testTest2")) {
                    Driver.runTests(repository, ci, false);
                }
                else {
                    Driver.runProgram(repository, ci, new ArrayList<String>());
                }
            }
            return 0;
        }


        @Override
        public String tag() {
            // TODO Auto-generated method stub
            return "interpret";
        }

    }
    public static void main(String[] args) throws IOException {
        junit.textui.TestRunner.run(FileTests.interpreterSuite("shelltests", true, false));
        junit.textui.TestRunner.run(FileTests.interpreterSuite("tests", true, false));
        junit.textui.TestRunner.run(FileTests.interpreterSuite("not_passing_yet", false, true));
    }

    public static TestSuite interpreterSuite(
            String dir_name,
            boolean failsOnly,
            boolean expect_failure) throws IOException {

        TestSuite suite = new TestSuite("Runs all tests in " + dir_name) {
            public void run(TestResult result) {
                super.run(result);
                Init.allowForLeakChecks();
            }
        };

        String dirname = ProjectProperties.backslashToSlash(dir_name);
        File dir = directoryAsFile(dirname);
        System.err.println(dir);

        /* Many lines of random number generator seed nonsense. */

        Iterable<String> shuffled = shuffledFileList(dir, true);

        int testCount = testCount();
        int i = testCount;

        for(String s : shuffled){
              if (i <= 0) {
                  System.out.println("Early testing exit after " + testCount + " tests");
                  break;
              }
              boolean decrement = true;
              if (s.endsWith("Syntax.fss") || s.endsWith("DynamicSemantics.fss")) {
                  System.out.println("Not compiling file " + s);
                  decrement = false;
              } else if (!s.startsWith(".")) {
                  if (s.endsWith(".fss")) {
                      int l = s.lastIndexOf(".fss");
                      String testname = s.substring(0, l);
                      suite.addTest(new InterpreterTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else if (s.endsWith(".sh")) {
                      int l = s.lastIndexOf(".sh");
                      String testname = s.substring(0, l);
                      suite.addTest(new ShellTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else {
                      System.out.println("Not compiling file " + s);
                      decrement = false;
                  }
              }

              if (decrement) {
                  i--;
              }

        }
        return suite;
    }

    /**
     * Generates a suite of tests from directory dir_name, using "foo.test" files
     * to determine the content of the test.  failsOnly means to only print failing
     * tests (either normal tests that fail, or XXX tests that succeed).
     *
     * WARNING: expect_failure  is not treated consistently.
     *
     * It either means, "the test fails, and that is a good thing",
     * or it means, "the test fails, it is a bad thing, but we are working on it
     * and do not want to be bothered by something we already know is a problem".
     *
     * @param dir_name
     * @param failsOnly
     * @param expect_failure
     * @return
     * @throws IOException
     */
    public static TestSuite compilerSuite(
            String dir_name,
            boolean shuffle,
            boolean failsOnly,
            boolean expect_failure) throws IOException {

        TestSuite suite = new TestSuite("Runs all tests in " + dir_name) {
            public void run(TestResult result) {
                super.run(result);
                Init.allowForLeakChecks();
            }
        };

        String dirname = ProjectProperties.backslashToSlash(dir_name);
        File dir = directoryAsFile(dirname);
        System.err.println(dir);

        /* Many lines of random number generator seed nonsense. */

        Iterable<String> shuffled = shuffledFileList(dir, shuffle);

        int testCount = testCount();
        int i = testCount;

        List<Test> compileTests = new ArrayList<Test>();
        List<Test> typecheckTests = new ArrayList<Test>();
        List<Test> desugarTests = new ArrayList<Test>();
        List<Test> linkTests = new ArrayList<Test>();
        List<Test> runTests = new ArrayList<Test>();

        for(String s : shuffled){
              if (i <= 0) {
                  System.out.println("Early testing exit after " + testCount + " tests");
                  break;
              }
              boolean decrement = true;
              boolean shouldFail = s.startsWith("XXX");

              if (s.endsWith(".fss") || s.endsWith(".fsi") ) {
                  // do nothing
                  decrement = false;
              } else if (!s.startsWith(".")) {
                  if (s.endsWith(".sh")) {
                      int l = s.lastIndexOf(".sh");
                      String testname = s.substring(0, l);
                      suite.addTest(new ShellTest(dir.getCanonicalPath(), dirname, testname, failsOnly, expect_failure));
                  } else if (s.endsWith(".test")) { // need to define the test of tests.


                      StringMap props = new StringMap.FromFileProps(dirname+"/"+s);
                      props = ProjectProperties.composedWith(props);

                      int l = s.lastIndexOf(".test");
                      String testname = s.substring(0, l);

                      String testNames = props.get("tests");
                      if (testNames == null)
                          testNames = "";
                      else
                          testNames = testNames.trim();


                      if (testNames.length() > 0) {
                          StringTokenizer st = new StringTokenizer(testNames);
                          while  (st.hasMoreTokens()) {
                              String token = st.nextToken();
                              standardCompilerTests(props, dir, dirname, token,
                                                    expect_failure, shouldFail, failsOnly,
                                                    compileTests, typecheckTests,
                                                    desugarTests, linkTests, runTests);
                          }
                      }
                      else {

                          standardCompilerTests(props, dir, dirname, testname,
                                                expect_failure, shouldFail, failsOnly,
                                                compileTests, typecheckTests,
                                                desugarTests, linkTests, runTests);
                      }
                  } else {
                      System.out.println("Not compiling file " + s);
                      decrement = false;
                  }
              }

              if (decrement) {
                  i--;
              }

        }
        // Do all the larger tests
        if (i > 0) {
            for (Test test: compileTests)
                suite.addTest(test);
            
            for (Test test: typecheckTests)
              suite.addTest(test);

            for (Test test: desugarTests)
                suite.addTest(test);

            for (Test test: linkTests)
                suite.addTest(test);

            for (Test test: runTests)
                suite.addTest(test);
        }
        return suite;
    }

    /**
     * @param props
     * @param dir
     * @param dirname
     * @param testname
     * @param expect_not_passing Test does not pass yet
     * @param shouldFail Test passes, if it fails (e.g., is an error printed?)
     * @param failsOnly
     * @param compileTests
     * @param desugarTests
     * @param linkTests
     * @param runTests
     * @throws IOException
     */
    private static void standardCompilerTests(StringMap props, File dir,
                                              String dirname, String testname,
                                              boolean expect_not_passing,
                                              boolean shouldFail,
                                              boolean failsOnly,
                                              List<Test> compileTests,
                                              List<Test> typecheckTests,
                                              List<Test> desugarTests,
                                              List<Test> linkTests,
                                              List<Test> runTests) throws IOException {
        if (props.get("compile") != null)
            compileTests.add(new CompileTest(props, dir.getCanonicalPath(),
                                             dirname, testname, failsOnly,
                                             expect_not_passing, shouldFail));
        if (props.get("typecheck") != null)
            typecheckTests.add(new TypeCheckTest(props, dir.getCanonicalPath(),
                                                 dirname, testname, failsOnly,
                                                 expect_not_passing, shouldFail));
        if (props.get("desugar") != null)
            desugarTests.add(new DesugarTest(props, dir.getCanonicalPath(),
                                             dirname, testname, failsOnly,
                                             expect_not_passing, shouldFail));
        if (props.get("link") != null)
            linkTests.add(new LinkTest(props, dir.getCanonicalPath(),
                                       dirname, testname, failsOnly,
                                       expect_not_passing, shouldFail));
        if (props.get("run") != null)
            runTests.add(new TestTest(props, dir.getCanonicalPath(),
                                      dirname, testname, failsOnly,
                                      expect_not_passing, shouldFail));
    }

    /**
     * @param dirname
     * @return
     * @throws FileNotFoundException
     */
    private static File directoryAsFile(String dirname)
            throws FileNotFoundException {
        File dir = new File(dirname);
        if (!dir.exists()) {
            System.err.println(dirname + " does not exist");
            throw new FileNotFoundException(dirname);
        }
        if (!dir.isDirectory()) {
            System.err.println(dirname + " exists but is not a directory");
            throw new IllegalArgumentException(dirname);
        }
        return dir;
    }

    /**
     * @return
     */
    private static int testCount() {
        int testCount = Integer.MAX_VALUE;
        try {
            testCount = ProjectProperties.getInt("fortress.unittests.count",
                    Integer.MAX_VALUE);
        } catch (NumberFormatException ex) {
            System.err
                    .println("Failed to translate property fortress.unittests.count as an int, string = "
                            + ProjectProperties.get("fortress.unittests.count"));
            System.err.println("Expected a number in the form DIGITS (base 10) or DIGITS_BASE");
        }

        /* Limited testing count, such as it is. */
        if (testCount != Integer.MAX_VALUE)
            System.err.println("Test count = " + testCount);
        return testCount;
    }

    private static long default_seed = System.currentTimeMillis();

    /**
     * @param files
     * @return
     */
    private static Iterable<String> shuffledFileList(File dir, boolean shuffle) {
        String[] files = dir.list();

        long seed = default_seed;
        try {
            seed = ProjectProperties.getLong("fortress.unittests.seed",
                    default_seed);
        } catch (NumberFormatException ex) {
            System.err
                    .println("Failed to translate property fortress.unittests.seed as a long, string = "
                            + ProjectProperties.get("fortress.unittests.seed"));
            System.err.println("Expected a number in the form DIGITS (base 10) or DIGITS_BASE");
        }
        Random random = new java.util.Random(seed);
        
        Iterable<String> shuffled = shuffle ? IterUtil.shuffle(Arrays.asList(files),
                random) : Arrays.asList(files);
        
        System.err.println("Test shuffling seed env var = FORTRESS_UNITTESTS_SEED="
                + Long.toHexString(seed) + "_16");
        return shuffled;
    }
}
