// attempt to use java.awt.Desktop to launch a URL.
// requires JDK 6+!

package TCGA;

import java.net.URI;
import java.net.URL;
import java.awt.Desktop;

public class URLLauncherDesktop {

  public static boolean launch_url(URL url) {
    boolean ok = false;

    try {
      if (Desktop.isDesktopSupported()) {
	Desktop d = Desktop.getDesktop();
	//	System.err.println("invoking URL via Desktop");  // debug
	d.browse(url.toURI());
	ok = true;
      } else {
	System.err.println("Desktop not supported");  // debug
      }
    } catch (Throwable t) {
      System.err.println("can't launch URL via Desktop: " + t);  // debug
    }

    return ok;
  }

  public static void main (String[] argv) {
    try {
      URL u = new URL("http://www.cnn.com/");
      URLLauncherDesktop.launch_url(u);
    } catch (Exception e) {
      System.err.println("error:"+e);  // debug
    }
  }

}
