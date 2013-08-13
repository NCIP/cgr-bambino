source code for
  - Bambino (https://cgwb.nci.nih.gov/cgi-bin/bambino)
  - CGWB heatmap viewer (https://cgwb.nci.nih.gov/cgi-bin/heatmap)
  - chromatogram viewer
  - miscellaneous stuff

- author: Michael Edmonson 
  - edmonson@nih.gov [obsolete]
  - Michael.Edmonson@stjude.org [current]
  - mnedmonson@gmail.com [personal]

- technical github hosting issues: Richard Finney
  - finneyr@mail.nih.gov

Apologies for the various unsightly and half-baked spectacles within.

Notes:

  - java 1.5+ is required for the heatmap viewer, 1.6+ is
    required for Bambino (due to Picard library).

  - The heatmap viewer uses a few additional small pieces of third-party code:
     - Sun's SpringUtilities.java
     - David Wallace Croft's JnlpProxy.java

  - to compile, CLASSPATH must be set to include:

    1. this unpacked source directory 
    2. the unpacked third-party code directory

    so i.e.:

      set CLASSPATH=c:\whatever\;c:\whatever\third_party\unpacked\

    If compiling Bambino, CLASSPATH must also include:

    3. Picard library (http://picard.sourceforge.net/) sam-XXXX.jar,
       as of this writing latest version is sam-1.09.jar

    4. MySQL Connector-J (JDBC driver for mysql)
       http://dev.mysql.com/downloads/connector/j/, as of this 
       writing latest version is 5.1.10

so in total (Windows path example):

CLASSPATH=c:\me\work\java2;c:\me\work\java2\third_party\sam-1.09.jar;c:\me\work\java2\third_party\mysql-connector-java-5.1.10-bin.jar;c:\me\work\java2\third_party\unpacked\;.

  - enter "make hmjar" to build heatmap.jar
  - enter "make avjar" to build av.jar (bambino core)

