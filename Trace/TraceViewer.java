package Trace;
// Trace viewer in a toplevel window

import java.awt.*;
import java.util.*;

public class TraceViewer extends Funk.CloseFrame implements Observer{
  TracePanel tp;

  private double initial_w_ratio = 0.65;
  private double initial_h_ratio = 0.35;

  public TraceViewer(String filename) {
    tp = new TracePanel(filename);
    setup();
  }

  public TraceViewer (TraceFile t) {
    tp = new TracePanel(t);
    setup();
  }

  public TraceViewer (TraceFile t, double w, double h) {
    initial_w_ratio = w;
    initial_h_ratio = h;
    tp = new TracePanel(t);
    setup();
  }

  public TraceViewer(String filename, int position) {
    tp = new TracePanel(filename, position);
    setup();
  }

  public TraceViewer(String filename, int position, boolean reverse_complemented) {
    tp = new TracePanel(filename, position, reverse_complemented);
    setup();
  }

  public TraceViewer(String filename, boolean reverse_complemented) {
    tp = new TracePanel(filename, reverse_complemented);
    setup();
  }

  public void setPhd (PhdFile p) {
    tp.setPhd(p);
  }

  public static void main (String [] argv) {
    // when run as a standalone app
    // ...how will the application end "naturally"?
    // ie if window is closed, program is still running...why?
    
    StreamDelegator.guess_compression();
    int i;
    String trace = null;
    boolean rc = false;
    int offset = 0;
    boolean use_trace_server = false;
    for (i=0; i < argv.length; i++) {
      if (argv[i].equals("-local")) {
	StreamDelegator.set_local(true);
      } else if (argv[i].equals("-rc")) {
	rc = true;
      } else if (argv[i].equals("-ts")) {
	use_trace_server = true;
      } else if (argv[i].equals("-sbb")) {
	System.err.println("scrolling by bases");  // debug
	TraceCanvas.set_scroll_by_bases(true);
	TraceCanvas.set_fixed_bases_scroll(true);
      } else if (argv[i].equals("-offset")) {
	offset = Integer.parseInt(argv[++i]);
      } else if (argv[i].equals("-h")) {
	StreamDelegator.set_host(argv[++i]);
      } else if (argv[i].equals("-mp")) {
	StreamDelegator.set_mod_perl(true);
      } else {
	trace = argv[i];
      }
    }
    if (trace == null) {
      System.out.println("Usage: TraceViewer tracefile [options]");  // debug
      System.exit(1);
    } else if (use_trace_server) {
      System.err.println("DISABLED (so applet JDK4 code can compile)");  // debug
//       System.err.println("trace server: " + trace);  // debug
//       TraceServerClient tsc = new TraceServerClient();
//       if (true) {
// 	System.err.println("** synchronous load **");  // debug
// 	TraceFile tf = tsc.get_trace(Integer.parseInt(trace));
// 	new TraceViewer(tf);
//       } else {
// 	System.err.println("** asynchronous load **");  // debug
// 	// TraceFile tf = tsc.get_trace(Integer.parseInt(trace), this);
//       }
    } else {
      new TraceViewer(trace,offset,rc);
    }
  }

  void setup () {
    setTitle(tp.filename);

    add("Center", tp);
    //    System.out.println("before pack");  // debug
    pack();
    //    System.out.println("after " + System.currentTimeMillis());
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((int) (d.width * initial_w_ratio), (int) (d.height * initial_h_ratio));
    setVisible(true);
  }
  
  public void center_in_screen (Point p) {
    if (p != null) {
      setLocation(p.x - (getSize().width / 2), p.y);
    }
  }

  public void center_on (int i) {
    // center the viewer on the specified point in the trace
    tp.center_on(i);
  }

  public void center_on (int i, boolean auto_rc) {
    // center the viewer on the specified point in the trace
    tp.center_on(i, auto_rc);
  }

  public void go_to(TraceFile tf, int offset) {
    setTitle(Funk.Str.basename(tf.name));
    tp.go_to(tf, offset);
    setVisible(true);
  }

  public boolean loaded () {
    // is the TraceFile finished loading yet?
    return tp.loaded();
  }

  public boolean error () {
    // error loading?
    return tp.error();
  }

  public TraceFile get_trace () {
    // get the TraceFile object used by the viewer
    return tp.get_trace();
  }

  public void update (Observable o, Object arg) {
    System.err.println("UPDATE");  // debug
  }


}
