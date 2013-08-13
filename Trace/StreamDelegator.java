package Trace;

import java.io.*;
import java.net.*;
import java.applet.*;
import java.util.*;

// Return a DataInput stream to a data file.
// Intended to centralize local/remote checking code.
// incredibly gross, but at least it's limited to here  :P


// FIX ME: rather than TRY_LOCAL hack, try a local open and catch 
// security exception...

public class StreamDelegator {
  private static boolean TRY_LOCAL = false;
  private static String hint = "";

  //  private static String hostname = "chlcfs.fccc.edu";
  private static String hostname = "lpgws.nci.nih.gov";
  // default hostname for CGI transfers.

  private static boolean compression = false;
  // whether or not to request gzip compression (and transparently decompress).

  private static boolean use_mod_perl = false;
  // whether or not to request data from the apache/mod_perl server

  public static void setup (Applet a) {
    // should be called by applets when they start up:
    // - sets codebase
    // - determines whether data compression is available on this JVM
    URL cb = a.getCodeBase();
    //    String hm = a.getCodeBase().getHost(); 
    String hm = cb.getHost();
    if (hm.length() > 0) {
      System.out.println("using host:" + hm);  // debug
      hostname = hm;
    }
    
    if (cb.getPort() != cb.getDefaultPort() &&
	cb.getPort() != -1) {
      hostname = hostname.concat(":" + cb.getPort());
      // handle server running on non-standard port
    }

    guess_compression();
  }

  public static void set_host (String s) {
    hostname = s;
  }

  public static void set_local (boolean status) {
    TRY_LOCAL = status;
  }
  
  public static String get_cgi_base () {
    return("http://" + 
	   hostname +
	   (use_mod_perl ? "/perl/" : "/cgi-bin/"));
  }

  public static void set_mod_perl (boolean status) {
    System.out.println("set_mod_perl:" + status);  // debug
    use_mod_perl = status;
  }

  public static void try_compression (boolean status) {
    compression = status;
  }

  public static void guess_compression () {
    // determine if user is running is a 1.1 JVM implementing
    // GZIPInputStream
    try { 
      Class t = Class.forName("java.util.zip.GZIPInputStream");
      System.err.println("using data compression!");  // debug
      try_compression(true);
    } catch (ClassNotFoundException e) {
      System.err.println("data compression not available");
      try_compression(false);
    }
  }
  
  public static DataInputStream getStream(String s, int type) throws java.io.IOException {
    // type 0 = trace
    // type 1 = .phd file
    // type 2 = literal file, in test directory [DEFUNCT]
    // type 3 = literal file, full path on server provided; Bad Idea (tm)
    // type 4 = given trace id, return description and library info from database
    // type 5 = get dbEST report for this id from NCBI
    // type 6 = get filename, contig, offset for the provided location ID, plus SNPs
    DataInputStream result = null;

    if (s.equals("stdin")) {
      System.out.println("reading from stdin!");
      result = new DataInputStream(new BufferedInputStream(System.in));
    } else if (s.length() >= 7 && s.substring(0,7).equals("http://")) {
      // looks like a URL; try that
      result = open_url(s);
    } else if (TRY_LOCAL) {
      result = try_file(s);
      // try current directory first
      if (result == null) {
	// 
	//  Attempt #2: assume we're in phredphrap-style "edit_dir",
	//  try alternate directories
	//
	switch (type) {
	case 0: result = try_file("../chromat_dir/" + s); break;
	case 1: result = try_file("../phd_dir/" + s + ".phd.1"); break;
	case 2: result = try_file("../poly_dir/" + s + ".poly"); break;
	}

	if (result == null) {
	  //
	  //  Attempt #3: try extracting from shared .zip archive
	  //

	  try { 
	    Class t = Class.forName("java.util.zip.ZipInputStream");
	    String xname = "";
	    switch (type) {
	    case 0: xname = s; break;
	    case 1: xname = s + ".phd.1"; break;
	    default: System.out.println("uh-oh!!");  // debug
	    }
	    String zipfile = "/usr/chlc/SNP/traces/" + s.substring(0,2) + "/" +
	      s.substring(0,4) + "/" + s + ".zip";
	    ByteArrayInputStream bs = Funk.Stream.get_zip_stream(zipfile, xname);
	    if (bs != null) result = new DataInputStream(bs);
	  } catch (ClassNotFoundException e) {
	    System.out.println("Can't extract from zip on this JVM.");  // debug
	  }
	}
      }
      if (result == null) {
	System.err.println("Local file " + s + " does not exist");  // debug
      }
    }

    if (result == null) {
      // It's not local, and doesn't have a URL header.
      // Try to get via the gettrace.pl script.
      if (type == 3) {
	// This is a TOTAL HACK:

	// For type 3 requests (usually .ace files), save the filename
	// in a static variable.  This is passed to the CGI script as
	// a "hint" as to where to locate resources that are outside of
	// the /usr/chlc/SNP/traces directory.  For example, lab
	// result trace files that are stored with rebuilt assemblies,
	// (ie demi-glace schuler result directories).  It would have
	// been "cleaner" to store these store these hints with the
	// object being requested; ie Trace, PhdFile, etc. would all
	// pass the location hint as they opened their streams.
	// However, as this is all a hack to get these programs to work
	// with CGI in the first place, I didn't want to clutter
	// up the implementations of Trace, PhdFile, etc.  This level
	// of detail is not something those implementations should
	// need to bother with.
	//
	// This will TOTALLY BREAK if you have 2 assembly windows
	// up at the same time; the hint will point to the wrong
	// assembly directory for the older assembly window.
	hint = s;
      }
      System.err.println("loading " + s + " via gettrace.pl, type " + type + "(hint=" + hint + ")");

      //      System.out.println(s + " " + URLEncoder.encode(s));
      long start = System.currentTimeMillis();

      if (true) { 
	// try POSTing
	Hashtable params = new Hashtable();
	params.put("trace", s);
	params.put("type", "" + type);
	params.put("hint", hint);
	boolean do_comp = compression && type <= 3;
	if (do_comp) params.put("comp", "yes");
	result = open_url(get_cgi_base() + "gettrace.pl", params);
	if (do_comp) result = GZIPOpen.open(result);
      } else {
	String url = get_cgi_base() +
	  "gettrace.pl?trace=" + 
	  URLEncoder.encode(s, "UTF-8") +
	  "&type=" + type + 
	  "&hint=" + URLEncoder.encode(hint, "UTF-8");
	if (compression && type <= 3) {
	  // only files can be compressed: assemblies, traces, derived trace
	  // data, etc.  The other types are generally very small amounts
	  // of data.
	  url = url.concat("&comp=1");
	  result = GZIPOpen.open(open_url(url, false));
	} else {
	  result = open_url(url, false);
	}
      }

      System.out.println("start lag time: " + (System.currentTimeMillis() - start) + " ms");  // debug
    }
    return(result);
  }

  static DataInputStream try_file (String filename) {
    // see if the specified file exists; if it does, open a stream to it.
    File f = new File(filename);
    DataInputStream result = null;
    if (f.exists()) {
      try {
	//	System.out.println("Reading from file " + filename); 
	result = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
      } catch (java.io.FileNotFoundException e) {
	System.err.println(e);
      }
    } else {
      System.err.println("File " + filename + " doesn't exist");  // debug
    }
    return result;
  }

  public static DataInputStream open_url (String s) throws java.io.IOException {
    //    new Exception().printStackTrace();
    System.out.println("URL: " + s);  // debug
    DataInputStream result = null;
    try {
      URL url = new URL(s);
      //      try {
	result = new DataInputStream(new BufferedInputStream(url.openStream()));
	//      } catch (java.io.IOException e) {
	//	System.out.println(e);
	//      }
    } catch (java.net.MalformedURLException e) {
      System.err.println("bad url: " + e);
    }
    return(result);
  }

  public static DataInputStream open_url (String s, boolean cacheable) throws java.io.IOException {
    // Open a URL, specifying whether the connection is allowed to be cached.
    //
    // Netscape appears to want to cache all net connections, so if a
    // program requires the latest, greatest data it must disable caching
    // to be sure it gets it.
    //
    System.out.println("URL: " + s);
    System.out.println("   caching allowed: " + cacheable);
    DataInputStream result = null;
    try {
      URL url = new URL(s);
      URLConnection uc = url.openConnection();
      uc.setUseCaches(cacheable);
      result = new DataInputStream(new BufferedInputStream(uc.getInputStream()));
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad url: " + e);
    }
    return(result);
  }

  public static BufferedReader open_url_br (String s, boolean cacheable) throws java.io.IOException {
    // Open a URL, specifying whether the connection is allowed to be cached.
    //
    // Netscape appears to want to cache all net connections, so if a
    // program requires the latest, greatest data it must disable caching
    // to be sure it gets it.
    //
    System.err.println("URL: " + s);
    System.err.println("   caching allowed: " + cacheable);
    BufferedReader result = null;
    try {
      URL url = new URL(s);
      URLConnection uc = url.openConnection();
      uc.setUseCaches(cacheable);
      result = new BufferedReader(new InputStreamReader(uc.getInputStream()));
    } catch (java.net.MalformedURLException e) {
      System.err.println("bad url: " + e);
    }
    return(result);
  }

  public static DataInputStream open_url (String u, Hashtable params) throws java.io.IOException {
    // POST-style open
    // thanks to http://www.javaworld.com/javaworld/javatips/jw-javatip34.html
    URL url = new URL(get_cgi_base() + "gettrace.pl");

    URLConnection urlConn = url.openConnection();
    // URL connection channel.

    urlConn.setDoInput (true);
    // Let the run-time system (RTS) know that we want input.

    urlConn.setDoOutput (true);
    // Let the RTS know that we want to do output.

    urlConn.setUseCaches (false);
    // No caching, we want the real thing.

    urlConn.setRequestProperty
      ("Content-Type", "application/x-www-form-urlencoded");
    // Specify the content type.

    DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ());
    // Send POST output.


    int count=0;
    StringBuffer sb = new StringBuffer();
    Enumeration e = params.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      String value = (String) params.get(key);
      if (count++ > 0) sb.append("&");
      // parameter separator
      sb.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
    }
    
    System.out.println("POST: " + u + "?" + sb.toString());  // debug
    printout.writeBytes (sb.toString());
    printout.flush ();
    printout.close ();

    // Get response data.
    DataInputStream result = new DataInputStream (new BufferedInputStream(urlConn.getInputStream ()));

    return result;
  }

}
