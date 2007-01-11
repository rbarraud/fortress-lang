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

package com.sun.fortress.interpreter.drivers;

import java.io.File;
import java.net.URI;

public class ProjectProperties {
    /* This static field holds the absolute path of the project location, as
     * computed by reflectively finding the file location of the unnamed
     * package, and grabbing the parent directory.
     */
    public static final String BASEDIR =
        new File(URI.create(ProjectProperties.class.getProtectionDomain().
            getCodeSource().getLocation().toExternalForm())).
                getParent()
                    + File.separator;

    /** Creates a new instance of ProjectProperties */
    private ProjectProperties() {
    }
}
