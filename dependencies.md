This file documents COMETS dependencies, as of version 2.9.1. Packaged
versions of COMETS will come with these dependencies, except for Gurobi.
Developers cloning COMETS from github will need to install these
dependencies independently. Here we list the dependencies, state where
COMETS expects specific .jars, and give a link to a download url (with
the caveat that we do not control these links and therefore they may die
unexpectedly).

At the bottom of this document is a line which should be set as the java
library path in eclipse or wherever you are developing COMETS.

\$VERSION is your COMETS directory.

1.  **High-performance computing libraries colt and concurrent:**

<https://dst.lbl.gov/ACSSoftware/colt/colt-download/releases/>

\$VERSION/lib/colt/lib/colt.jar

\$VERSION/lib/colt/lib/concurrent.jar

2.  **JDistlib (Java Statistical Distribution Library**

<https://sourceforge.net/projects/jdistlib/>

\$VERSION/lib/jdistlib-0.4.5-bin.jar

3.  **Apache commons libraries: lang3, rng, math3**

<https://commons.apache.org/proper/commons-lang/download_lang.cgi>

<https://commons.apache.org/proper/commons-math/download_math.cgi>

<https://commons.apache.org/proper/commons-rng/download_rng.cgi>

\$VERSION/lib/commons-lang3-3.9/commons-lang3-3.9-sources.jar

\$VERSION/lib/commons-lang3-3.9/commons-lang3-3.9.jar

\$VERSION/lib/commons-rng-1.0/commons-rng-simple-1.0.jar

\$VERSION/lib/commons-rng-1.0/commons-rng-sampling-1.0.jar

\$VERSION/lib/commons-rng-1.0/commons-rng-jmh-1.0.jar

\$VERSION/lib/commons-rng-1.0/commons-rng-core-1.0.jar

\$VERSION/lib/commons-rng-1.0/commons-rng-client-api-1.0.jar

\$VERSION/lib/commons-math3-3.6.1/commons-math3.6.1.jar

\$VERSION/lib/commons- math3-3.6.1/commons-math3.6.1-tools.jar

4.  **Junit (for unit testing)**

<https://junit.org/junit4/>

\$VERSION/lib/junit/junit-4.12.jar

\$VERSION//lib/junit/hamcrest-core-1.3.jar

5.  **JOGL (for opengl bindings for GUI)**

<https://jogamp.org/wiki/index.php?title=Downloading_and_installing_JOGL>

\$VERSION/lib/jogl/jogamp-all-platforms/jar/jogl-all.jar

\$VERSION/lib/jogl/jogamp-all-platforms/jar/gluegen-rt.jar

\$VERSION/lib/jogl/jogamp-all-platforms/jar/gluegen.jar

\$VERSION/lib/jogl/jogamp-all-platforms/jar/gluegen-rt-natives-linux-amd64.jar

\$VERSION/lib/jogl/jogamp-all-platforms/jar/jogl-all-natives-linux-amd64.jar

6.  **JMatIO for writing / reading matlab files in java**

<https://sourceforge.net/projects/jmatio/>

\$VERSION/lib/JMatIO/lib/jmatio.jar

\$VERSION/lib/JMatIO/JMatIO-041212/lib/jmatio.jar

7.  **GLPK (linear optimization solver, open-source). Use this or Gurobi
    (Gurobi is faster if you are academic and can get a license.)**

<http://glpk-java.sourceforge.net/>

\$VERSION/lib/glpk-java.jar

8.  **Gurobi 9.0.0 (linear optimization solver, closed-source, requires
    license). Be sure to validate your license. **

<https://www.gurobi.com/>

/gurobi/9.0.0/install/lib/gurobi.jar

Libraries:

-Djava.library.path=gurobi/9.0.0/install/lib/:\$VERSION/lib/jogl/jogamp-all-platforms/lib
