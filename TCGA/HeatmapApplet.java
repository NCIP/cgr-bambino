package TCGA;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

//import java.applet.*;

public class HeatmapApplet extends JApplet implements Runnable {
  //public class HeatmapApplet extends Applet implements Runnable {
  // FIX ME: Swing/thread-safety problems:
  // http://java.sun.com/products/jfc/tsc/articles/threads/threads1.html
  // 
  // "...applets that use Swing components must be implemented
  // as subclasses of JApplet, and components should be added to the
  // JApplet content pane, rather than directly to the JApplet."

  public String dataset,auth;
  public boolean has_annotations = false;
  public boolean has_bins = false;

  private HeatmapConfiguration config;

  public HeatmapApplet() {
    // contructor for applet/application mode
  }
  
  public void init() {
    // applet mode startup
    config = new HeatmapConfiguration();
    URLLauncher.set_applet(this);
    Options.IS_APPLET = true;
    dataset = getParameter("dataset");
    auth = getParameter("auth");
    if (getParameter("annotations") != null) has_annotations = true;
    if (getParameter("bin") != null) has_bins = true;
    config.start_bin = getParameter("start_bin");
    if (getParameter("start_pos") != null) config.start_pos = new Integer(getParameter("start_pos"));
    if (getParameter("end_pos") != null) config.end_pos = new Integer(getParameter("end_pos"));
    config.start_marker = getParameter("start_marker");
    config.title = getParameter("title");

    new Thread(this).start();
  }

  public void run() {
    setLayout(new BorderLayout());
    add("Center", new JLabel("applet launched."));

    System.err.println("running");
    URL url = URLLauncher.get_modified_url("/cgi-bin/heatmap");
    System.err.println(url);  // debug

    try {
      boolean use_binary = Options.DEFAULT_USE_BINARY_SAMPLE_FILE;

      Hashtable p = new Hashtable();
      p.clear();
      p.put("ds", dataset);
      p.put("serve", "data");
      if (auth != null) p.put("auth", auth);
      if (use_binary) p.put("binary", "1");

      //      config.gm = new GenomicMeasurement(new GZIPInputStream(open_url(url, p)));
      // WTF, seems to hang??

      config.gm = new GenomicMeasurement(new GZIPInputStream(open_url(url, p)), true, use_binary);

      if (has_bins) {
	p.clear();
	p.put("ds", dataset);
	p.put("serve", "bin");
	config.gs = new GenomicSet(new GZIPInputStream(open_url(url, p)));
	//	System.err.println("bins:" + gs.get_bins().size());  // debug
      } else {
	System.err.println("synthetic genomic bins");  // debug
	config.gs = new GenomicSet(config.gm, GenomicSet.STYLE_GENOMIC, null);
      }
      
      if (has_annotations) {
	p.clear();
	p.put("ds", dataset);
	p.put("serve", "annotations");
	if (auth != null) p.put("auth", auth);
	config.af = new AnnotationFlatfile2(new GZIPInputStream(open_url(url, p)));
      }
      Heatmap6 hm = new Heatmap6(config);
      config.gs.addObserver(hm);
    } catch (Exception e) {
      report_exception(e);
    }
  }

  private void report_exception (Exception e) {
    System.err.println("ERROR: " + e);  // debug
    e.printStackTrace();  // debug
    System.exit(1);
  }

  public InputStream open_url (URL url, Hashtable params) throws java.io.IOException {
    // POST-style open
    // thanks to http://www.javaworld.com/javaworld/javatips/jw-javatip34.html

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
    
    System.err.println("POST: " + url + "?" + sb.toString());  // debug
    printout.writeBytes (sb.toString());
    printout.flush();
    printout.close();

    // Get response data.
    //    DataInputStream result = new DataInputStream (new BufferedInputStream(urlConn.getInputStream()));
    // return result;

    return urlConn.getInputStream();
  }


  public static void main (String [] argv) {
    JFrame jf = new JFrame();
    HeatmapApplet hma = new HeatmapApplet();
    hma.dataset = "11";
    
    jf.add(hma);
    jf.pack();
    jf.setVisible(true);
    new Thread(hma).start();
  }

}
