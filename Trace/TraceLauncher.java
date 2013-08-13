package Trace;

import java.awt.*;
import java.applet.Applet;

public class TraceLauncher extends Applet implements Runnable {

  private TraceViewer tv;

  public TraceLauncher() {
    // applet constructor
  }

  public TraceLauncher(String fn) {
    // application-mode constructor
    Trace.StreamDelegator.set_mod_perl(true);
    setup(fn);
  }

  public void init () {
    // applet startup
    Trace.StreamDelegator.setup(this);
    Trace.StreamDelegator.set_mod_perl(true);
    setup(getParameter("file"));
  }

  public void setup (String file) {
    System.out.println("hey now");  // debug
    tv = new TraceViewer(file);
    new Thread(this).start();
  }

  public void run () {
    while (true) {
      try {
	Thread.sleep(1000);
	if (!tv.isValid() || !tv.isVisible()) {
	  repaint();
	  break;
	}
      } catch (InterruptedException e) { }
    }
  }

  public void paint (Graphics g) {
    if (tv != null && tv.isValid() && tv.isVisible()) {
      Funk.Gr.centerText(this, "Trace viewer started.");
    } else {
      Funk.Gr.centerText(this, "Done; click your browser's \"Back\" button to return.");
    }
  }

  public static void main (String [] argv) {
    if (argv.length == 0) {
      System.out.println("trace name required");  // debug
    } else {
      Frame f = new Funk.DieFrame();
      f.setTitle("Trace launcher");
      f.setLayout(new BorderLayout());
      TraceLauncher tl = new TraceLauncher(argv[0]);
      f.add("Center", tl);
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      f.pack();
      f.setSize((int) (d.width * 0.65), (int) (d.height * 0.35));
      f.setVisible(true);
    }
  }

}
