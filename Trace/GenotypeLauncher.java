package Trace;

import java.net.URL;
import java.net.URI;
import java.util.HashSet;

public class GenotypeLauncher extends java.applet.Applet implements Runnable {
  // applet to launch genotype viewer, given basename of genotype.dat

  public void init() {
    // applet startup
    new Thread(this).start();
  }

  public void run() {
    String genotype_url = this.getParameter("genotype_dat_url");
    TraceServerClient.set_applet_base(this);

    String preferred_window_width = this.getParameter("window_width_percent");
    if (preferred_window_width != null) {
      System.err.println("WIDTH:" + preferred_window_width);  // debug
      GenotypeViewer.set_window_width_percent(Integer.parseInt(preferred_window_width));
      JGenotypeViewer.set_window_width_percent(Integer.parseInt(preferred_window_width));
    }

    String preferred_window_height = this.getParameter("window_height_percent");
    if (preferred_window_height != null) {
      System.err.println("HEIGHT:" + preferred_window_height);  // debug
      Funk.MultiScrollPanel.set_window_height_percent(Integer.parseInt(preferred_window_height));
      Funk.JMultiScrollPanel.set_window_height_percent(Integer.parseInt(preferred_window_height));
    }

    try { 
      System.out.println("genotype url: " + genotype_url);
      URL url;

      try {
	url = new URL(genotype_url);
      } catch (java.net.MalformedURLException mfe) {
	System.err.println("malformed URL " + genotype_url + "; trying relative");  // debug
	url = new URL(getCodeBase(), genotype_url);
	System.err.println("revised: " + url);  // debug
      }
      //      System.err.println("got url");  // debug
      GenotypeParser gp = new GenotypeParser(url);
      //      System.err.println("got gp");  // debug

      if (true) {
	//
	// Swing version: security problems launching from applet???
	//
	System.err.println("launching Swing version");  // debug
	JGenotypeViewer gv = new JGenotypeViewer(gp, this);
      } else {
	// original version: OK in applet
	System.err.println("launching AWT version");  // debug
	GenotypeViewer gv = new GenotypeViewer(gp, this);
      }

      //      System.err.println("got gv");  // debug
    } catch (Exception e) {
      System.out.println("applet exception: " + e);
      e.printStackTrace();
    }
  }

  public static void main (String [] argv) {
    try {
      TraceServerClient tsc = new TraceServerClient();

      //      String genotype_url = "http://lpgws501.nci.nih.gov/genotypedat.1729967873.29240";
      String genotype_url = "file://c:/me/work/java2/Trace/launch.txt";
      HashSet trace_list = new HashSet<String>();
      trace_list.add("1");
      trace_list.add("2");
      trace_list.add("3");

      URL thingy = tsc.get_download_url(trace_list, genotype_url);
      System.err.println(thingy.toString());
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }

}
