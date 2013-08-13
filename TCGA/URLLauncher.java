package TCGA;
// convenience/utility class:
// various methods of launching a URL in a web browser depending on context
// to do: use java.awt.Desktop API if available?

import java.applet.*;
import java.net.URL;
// test comment out 2013 import com.croftsoft.core.jnlp.JnlpProxy;
import java.util.Observable;
import javax.swing.*;

public class URLLauncher extends Observable implements Runnable {
  private URL url;
  private String target;

  private static URL access_url = null;
  private static Applet applet = null;

  private static String DEFAULT_URL_STRING = "https://cgwb.nci.nih.gov/cgi-bin/heatmap";
  private static String DELAY_WARNING = "It seems to be taking a while to launch the web page; this can happen if the remote server is busy.  Please wait, the page should start eventually.";

  private static int DELAY_MS_BEFORE_WARNING = 5000;

  public URLLauncher (URL url, String target) {
    this.url = url;
    this.target = target;
    new Thread(this).start();
  }

  public URLLauncher (String url, String target) {
    try {
      this.url = new URL(url);
      this.target = target;
      new Thread(this).start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void run() {
    // attempt to launch the URL in a new thread, as this process seems to
    // hang for a while if the remote URL takes a long time to load.
    // (observed using JNLP and w/Desktop).
    //
    // ...maybe Java code is opening the URL first to see if it's valid?
    // This would compound the problem if the server is overloaded to begin with.
    //
    long start_time = System.currentTimeMillis();
    System.err.println("attempting to launch URL: " + url);  // debug

    boolean launched = false;
    addObserver(new DelayAlerter(DELAY_MS_BEFORE_WARNING, DELAY_WARNING));

    if (false) {
      System.err.println("DEBUG: Desktop API disabled");  // debug
    } else {
      try {
	// try java.awt.Desktop API if we have it (JDK 6+ required)
	Class t = Class.forName("java.awt.Desktop");
	launched = URLLauncherDesktop.launch_url(url);
      } catch (ClassNotFoundException e) {
	// ignore, continue w/older approaches that don't require JDK 6
      }
      //      System.err.println("launched via java.awt.Desktop?:" + launched);
    }

    if (!launched) {
      if (applet != null) {
	System.err.println("launching URL via applet");  // debug
	AppletContext ac = applet.getAppletContext();
	ac.showDocument(url, target);

      } else {
	//	ServiceManager.setServiceManagerStub(new ServiceManagerStub());
	// WTF: abstract, and:
	// "This method should be called exactly once by the JNLP Client, and never be called by a launched application."

// rpf 2013
//	if (JnlpProxy.showDocument(url)) {
//	  System.err.println("launched via JNLP!");  // debug
//	} else if (false) {
//	  System.err.println("JNLP unavailable; attempting horrible Windows-only hack.");  // debug
//	  Runtime runtime = Runtime.getRuntime();
//	  try {
//	    runtime.exec("cmd /c start " + url.toString());
//	    // HORRIBLE: works on Windows XP but nowhere else
//	    // this can be addressed by using java.awt.Desktop, but this
//	    // requires JDK 6, unavailable on remoteapps.nci.nih.gov
//	  } catch (Exception ex) {
//	    System.err.println("exec error: " + ex);  // debug
//	  }
//	} else {
//	  // error message
//	  String msg = "Unable to launch URL on your system.  Please enter the following link into your web browser: " + url.toString();
//	  System.err.println("can't launch URL: " + url.toString());  // debug
//	  setChanged();
//	  notifyObservers();
//	  // hack to stop delay alerter
//	  JOptionPane.showMessageDialog(null,
//					msg,
//					"Warning",
//					JOptionPane.WARNING_MESSAGE);
//
//	}
//	rpf 2013

      }
    }
    long elapsed = System.currentTimeMillis() - start_time;
    //    System.err.println("URL launch time: " + elapsed);
    setChanged();
    notifyObservers();
  }

  public static void launch_url(String url_string, String target) {
    try {
      URL url = new URL(url_string);
      launch_url(url, target);
    } catch (Exception e) {
      System.err.println("ERROR:"+e);  // debug
    }
  }

  public static void launch_url(URL url, String target) {
    new URLLauncher(url, target);
  }

  public static void set_applet (Applet ap) {
    applet = ap;
    access_url = applet.getCodeBase();
  }

  public static void set_url (String url) {
    try {
      access_url = new URL(url);
    } catch (Exception e) {
      System.err.println("invalid URL:"+e);  // debug
      access_url = null;
    }
  }

  public static URL get_url () {
    if (access_url == null) set_url(DEFAULT_URL_STRING);
    return access_url;
  }

  public static URL get_modified_url (String spec) {
    URL result = null;
    try {
      result = new URL(get_url(), spec);
    } catch (Exception e) {
      System.err.println("ERROR:"+e);  // debug
    }
    return result;
  }

  public static void launch_modified_url (String spec, String target) {
    URL url = get_modified_url(spec);
    if (url != null) launch_url(url, target);
  }

}
