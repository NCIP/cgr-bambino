package Ace2;

import java.util.Properties;
import java.io.*;
import java.net.*;
import java.util.*;

public class BambinoProperties {
  
  private static final String PROPERTIES_PATH = "Ace2/bambino.properties";
// private static final String DEFAULT_PROPERTIES_PATH = "c:/me/work/java2/" + PROPERTIES_PATH;

  private int DOS_FILESYSTEM=0;

  private Properties properties;
  
  private static final String K_BUILD_NUMBER = "build_number";
  private static final String K_BUILD_DATE = "build_date";

  public BambinoProperties() {
    load_properties();
  }

  private InputStream get_input_stream() {
    ClassLoader cl = this.getClass().getClassLoader();
    InputStream result = null;
    if (cl == null) {
      result = ClassLoader.getSystemResourceAsStream(PROPERTIES_PATH);
    } else {
      result = cl.getResourceAsStream(PROPERTIES_PATH);
    }
    return result;
  }

  public void load_properties() {
    properties = new Properties();
    try {
      InputStream is = get_input_stream();
      if (is != null) properties.load(is);
    } catch (Exception e) {
      System.err.println("ERROR loading properties: " + e);  // debug
    }
  }

  public void save_properties() {
    // private static final String DEFAULT_PROPERTIES_PATH = "unknown";
    String DEFAULT_PROPERTIES_PATH = new String("unknonw"); // String DEFAULT_PROPERTIES_PATH = "unknown";
    if (DOS_FILESYSTEM == 0) {
      DEFAULT_PROPERTIES_PATH = "/data/finneyr/ME/" + PROPERTIES_PATH;
    }
    else {
        DEFAULT_PROPERTIES_PATH = "c:/me/work/java2/" + PROPERTIES_PATH;
    }
    URL res = ClassLoader.getSystemResource(PROPERTIES_PATH);
    File output_file = null;
    if (res == null) {
      output_file = new File(DEFAULT_PROPERTIES_PATH);
      //      System.err.println("WARNING: can't find system file " + PROPERTIES_PATH + "; creating " + output_file);
    } else {
      output_file = new File(res.getFile());
    }
    try {
      FileOutputStream fos = new FileOutputStream(output_file);
      Enumeration e = properties.propertyNames();
      while (e.hasMoreElements()) {
	String key = (String) e.nextElement();
	properties.store(fos, key);
      }
      fos.close();
    } catch (Exception ex) {
      System.err.println("ERROR saving properties in save_properties: " + ex);  // debug
    }
  }

  public Object setProperty (String key, String value) {
    return properties.setProperty(key, value);
  }

  public void increment_build_number() {
    int bn = Integer.parseInt(properties.getProperty(K_BUILD_NUMBER, "0")) + 1;
    setProperty(K_BUILD_NUMBER, Integer.toString(bn));
  }

  public void set_build_date () {
    Date d = new Date();
    setProperty(K_BUILD_DATE, d.toString());
  }

  public String get_build_date() {
    return properties.getProperty(K_BUILD_DATE, "unknown");
  }

  public String get_build_number() {
    return properties.getProperty(K_BUILD_NUMBER, "unknown");
  }

  
  public static void main (String[] argv) {
    BambinoProperties bp = new BambinoProperties();
    boolean save = false;
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-increment-build")) {
	bp.increment_build_number();
	save = true;
      } else if (argv[i].equals("-set-build-date")) {
	bp.set_build_date();
	save = true;
      } else {
	System.err.println("ERROR: unknown param " + argv[i]);  // debug
	System.exit(1);
      }
    }
    if (save) {
      bp.save_properties();
    } else {
      System.err.println("nothing to do (not saving)");  // debug
    }
  }

}
