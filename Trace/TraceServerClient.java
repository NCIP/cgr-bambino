package Trace;
// client access to trace server data

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import java.applet.Applet;

import Funk.ZipTools;


// import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.*;


public class TraceServerClient extends Observable implements Observer {
  private String compression_type;

  public static final int TYPE_TRACE = 0;
  public static final String COMPRESS_BZIP2 = "bz2";
  public static final String COMPRESS_GZIP = "gz";
  
  //  private static String CGI_BASE = "http://lpgws.nci.nih.gov/perl/ts";
  private static String CGI_BASE = "http://cgwb.nci.nih.gov/cgi-bin/ts";
  //  private static String CGI_BASE = "https://cgwb-stage.nci.nih.gov:8443/cgi-bin/ts";
  // HACK
  
  public TraceServerClient () {
    setup();
  }

  private void setup () {
    compression_type = COMPRESS_BZIP2;
  }

  public void set_compression (String ct) {
    if (ct.equals(COMPRESS_BZIP2) || ct.equals(COMPRESS_GZIP)) {
      compression_type = ct;
    } else {
      System.err.println("ERROR: invalid compression type (bz2/gz)");  // debug
      System.exit(1);
    }
  }
  
  public TraceFile get_trace (int ti) {
    // load trace synchronously
    TraceFile result = null;
    try {
      DataInputStream dis = getStream(ti, TYPE_TRACE);
      if (dis == null) {
        System.err.println("WTF, null stream");  // debug
      } else {
        result = new TraceFile(Integer.toString(ti), dis);
      }
    } catch (Exception e) {
      System.err.println("ERROR opening trace stream: " + e);  // debug
    }
    return result;
  }

  public void get_trace (int ti, Observer o) {
    // load trace asynchronously
    int max_tries = 3;
    for (int attempt = 0; attempt < max_tries; attempt++) {
      //
      // FIX ME: can we make this wrapper code generic somehow?
      // It'd be nice to be able to wrap some small code snippet in 
      // an exception-catching, retrying wrapper.
      //
      try {
        //        if (attempt == 0) throw new Exception("crash test");
        DataInputStream dis = getStream(ti, TYPE_TRACE);
        //      System.err.println("stream = " + dis);  // debug
        if (dis == null) {
          throw new Exception("ERROR: no stream!");
        } else {
          // OK
          new TraceFile(Integer.toString(ti), dis, o);
          break;
        }
      } catch (Exception e) {
        // if connection times out, etc. retry a few times.
        System.err.println("ERROR opening trace stream: " + e);  // debug
        if (attempt < max_tries) {
          System.err.println("Retrying.");  // debug
        } else {
          System.err.println("giving up after " + max_tries + " attempts");  // debug
        }
      }
    }
  }

  public void get_traces (Collection<String> tid_list, Observer o) throws IOException {
    //
    // fetch a set of traces in a zipfile
    // FIX ME: do this in a new thread?
    //
    addObserver(o);
    Hashtable<String,String> params = new Hashtable<String,String>();

    HashSet wanted = new HashSet(tid_list);

    String list = Funk.Str.join(",", tid_list.iterator());
    params.put("ti", list);
    // request a list of trace IDs
    params.put("zip", "1");
    // request all traces be returned in a zipfile
    if (compression_type != null) params.put("compress", compression_type);
    // request traces be compressed within zipfile
    InputStream result;
    if (false) {
      System.err.println("LOCAL DEBUG!!");  // debug
      //      result = new FileInputStream("zip_broken.zip");
      //      result = new FileInputStream("junk_new.zip");
      result = new FileInputStream("junk_v16.zip");
      //      result = new FileInputStream("zip_workaround.zip");
    } else {
      result = open_url(CGI_BASE, params);
    }

    ZipInputStream zis = new ZipInputStream(result);
    // only works if Archive::Zip file was written to a seekable stream.
    ZipTools zt = new ZipTools(zis);
    while (zt.next()) {
      String trace_name = zt.get_name();
      //      System.err.println("entry: " + trace_name);
      byte[] data_buf = zt.get_bytearray();
      InputStream data_stream = new ByteArrayInputStream(data_buf);
      // get data blob for zipfile entry and convert to stream
      data_stream = compression_filter(data_stream);
      // add decompression filter if necessary, stream will
      // now return desired data, uncompressed
      TraceFile t = new TraceFile(trace_name,
				  new DataInputStream(data_stream));
      // parse TraceFile from data

      int lio = t.name.lastIndexOf('.');
      // any remove compression suffix in name
      if (lio != -1) {
	t.name = t.name.substring(0,lio);
	//	System.err.println("trimmed name to " + t.name);  // debug
      }
      
      setChanged();
      notifyObservers(t);

      //      wanted.remove(trace_name);
      wanted.remove(t.name);
      System.err.println("received " + t.name);

    }

    if (wanted.size() > 0) {
      // failed to retrieve one or more traces
      //      System.err.println("zip finished; missing = " + wanted.size());  // debug
      Object[] missing_list = wanted.toArray();
      String msg = "Couldn't retrieve " + wanted.size() + " traces (trace ID: " +
	missing_list[0] + ")";
      throw new FileNotFoundException(msg);
    }

  }

  public DataInputStream getStream(int ti, int type) throws java.io.IOException {
    //
    // only TYPE_TRACE implemented
    //

    // try POSTing
    Hashtable<String,String> params = new Hashtable<String,String>();
    params.put("ti", Integer.toString(ti));
    //    params.put("type", "" + type);

    if (compression_type != null) params.put("compress", compression_type);
    InputStream result = open_url(CGI_BASE, params);
    result = compression_filter(result);
    return new DataInputStream(result);
  }

  private InputStream compression_filter (InputStream result) throws IOException {
    if (compression_type != null) {
      if (compression_type.equals(COMPRESS_GZIP)) {
        //        result = GZIPOpen.open(result);
        result = new GZIPInputStream(result);
      } else if (compression_type.equals(COMPRESS_BZIP2)) {
        boolean error = false;
        if (result.read() != 'B') error = true;
        if (result.read() != 'Z') error = true;
        // trim two-byte "BZ" header; CBZip2InputStream chokes otherwise
        if (error) throw new IOException("bzip2 BZ header not found!");
        //        System.err.println("bzip stream header ok?: " + !error);  // debug
        result = new CBZip2InputStream(result);
      } else {
        System.err.println("ack!");  // debug
        System.exit(1);
      }
    }
    return result;
  }


  public InputStream open_url (String u, Hashtable params) throws java.io.IOException {
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
    //    DataInputStream result = new DataInputStream (new BufferedInputStream(urlConn.getInputStream()));
    // return result;

    return urlConn.getInputStream();
  }

  public static void main (String [] argv) {
    try {

      if (true) {
	System.err.println("can't");  // debug

	HashSet<String> allt = new HashSet<String>();
	allt.add("1");
	allt.add("2");
	allt.add("3");
	TraceServerClient tsc = new TraceServerClient();
	// tsc.get_traces(allt, this);
	// can't
      } else {
        FileInputStream fis = new FileInputStream("t4_bz.zip");
        ZipInputStream zis = new ZipInputStream(fis);
        ZipTools zt = new ZipTools(zis);
        while (zt.next()) {
          ByteArrayInputStream bis = zt.get_bytearrayinputstream();
          bis.read();
          bis.read();

          InputStream str = new CBZip2InputStream(bis);
          TraceFile t = new TraceFile(zt.get_name(),
                                      new DataInputStream(str));
          new Trace.TraceViewer(t);
        }
      }

    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }

  public void update (Observable o, Object arg) {
    // debug
    if (o instanceof TraceFile) {
      // trace has finished loading
      TraceFile t = (TraceFile) o;
      new TraceViewer(t);
    }
  }

  public static void set_applet_base (Applet ap) {
    URL codebase = ap.getCodeBase();
    String spec = null;
    if (codebase.getHost().equals("lpgws.nci.nih.gov")) {
      // server application uses mod_perl (fast)
      //      CGI_BASE = "http://" + hostname + "/perl/ts";
      spec = "/perl/ts";
    } else {
      // server application uses traditional cgi-bin (slow)
      spec = "/cgi-bin/ts";
    }
    try {
      URL url = new URL(codebase, spec);
      CGI_BASE = url.toString();
    } catch (Exception e) {
      System.err.println("ERROR constructing URL for " + spec);  // debug
    }
    System.err.println("set CGI base from applet to: " + CGI_BASE);  // debug
  }

  public static void set_cgi_base (String base) {
    CGI_BASE = base;
  }

  public static URI get_uri () {
    URI uri = null;
    try {
      uri = new URI(CGI_BASE);
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
    return uri;
  }

  public URL get_download_url (Collection<String> ti_list, String genotype_url) {
    // return GET-style URL to download a set of traces
    // FIX ME: given that we're passing genotype.dat URL, can we skip
    // sending trace list??  (have server parse out ID list)
    URI base_uri = get_uri();

    URL result = null;

    Hashtable<String,String> params = new Hashtable<String,String>();

    params.put("ti", Funk.Str.join(",", ti_list.iterator()));
    // list of trace IDs
    params.put("zip", "1");
    params.put("gd", genotype_url);
    
    try {
      URI uri = new URI("http",
			// scheme
			base_uri.getAuthority(),
			base_uri.getPath(),
			Funk.Str.url_query_string(params),
			// query
			null
			// fragment
			);
      result = uri.toURL();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }

    return(result);
  }


}
