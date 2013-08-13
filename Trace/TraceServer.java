package Trace;
// access to trace data from trace server

import java.io.*;
import java.net.*;
import java.util.*;

public class TraceServer {
  private boolean use_compression;

  public static final int TYPE_TRACE = 0;

  private static final String CGI_BASE = "http://lpgws.nci.nih.gov/perl/ts";
  
  public TraceServer () {
    setup();
  }

  private void setup () {
    use_compression = false;
  }

  public void set_compression (boolean compression) {
    use_compression = compression;
  }
  
  public TraceFile get_trace (int ti) {
    // load trace synchronously
    TraceFile result = null;
    try {
      DataInputStream dis = getStream(ti, TYPE_TRACE);
      result = new TraceFile(Integer.toString(ti), dis);
    } catch (Exception e) {
      System.err.println("ERROR opening trace stream: " + e);  // debug
    }
    return result;
  }

  public TraceFile get_trace (int ti, Observer o) {
    // load trace asynchronously
    TraceFile result = null;
    try {
      DataInputStream dis = getStream(ti, TYPE_TRACE);
      result = new TraceFile(Integer.toString(ti), dis, o);
    } catch (Exception e) {
      System.err.println("ERROR opening trace stream: " + e);  // debug
    }
    return result;
  }


  public DataInputStream getStream(int ti, int type) throws java.io.IOException {
    // only TYPE_TRACE implemented

    DataInputStream result = null;

    // try POSTing
    Hashtable<String,String> params = new Hashtable<String,String>();
    params.put("ti", Integer.toString(ti));
    //    params.put("type", "" + type);
    if (use_compression) params.put("compress", "yes");
    result = open_url(CGI_BASE, params);
    if (use_compression) result = GZIPOpen.open(result);
    return result;
  }

  public DataInputStream open_url (String u, Hashtable params) throws java.io.IOException {
    // POST-style open
    // thanks to http://www.javaworld.com/javaworld/javatips/jw-javatip34.html
    URL url = new URL(u);

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
    
    System.err.println("POST: " + u + "?" + sb.toString());  // debug
    printout.writeBytes (sb.toString());
    printout.flush();
    printout.close();

    // Get response data.
    DataInputStream result = new DataInputStream (new BufferedInputStream(urlConn.getInputStream ()));

    return result;
  }


}
