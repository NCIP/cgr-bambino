package Trace;
// Trace viewer in a toplevel window
// Swing version

import javax.swing.*;
import java.util.*;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Toolkit;

public class JTraceViewer extends JFrame implements Observer{
  JTracePanel tp;

  public JTraceViewer(String filename) {
    tp = new JTracePanel(filename);
    setup();
  }

  public JTraceViewer (TraceFile t) {
    tp = new JTracePanel(t);
    setup();
  }

  public JTraceViewer(String filename, int position) {
    tp = new JTracePanel(filename, position);
    setup();
  }

  public JTraceViewer(String filename, int position, boolean reverse_complemented) {
    tp = new JTracePanel(filename, position, reverse_complemented);
    setup();
  }

  public JTraceViewer(String filename, boolean reverse_complemented) {
    tp = new JTracePanel(filename, reverse_complemented);
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
	JTraceCanvas.set_scroll_by_bases(true);
	JTraceCanvas.set_fixed_bases_scroll(true);
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
      System.out.println("Usage: JTraceViewer tracefile [options]");  // debug
      System.exit(1);
    } else if (use_trace_server) {
      System.err.println("DISABLED (so applet JDK4 code can compile)");  // debug
//       System.err.println("trace server: " + trace);  // debug

//       TraceServerClient tsc = new TraceServerClient();
//       if (true) {
// 	System.err.println("** synchronous load **");  // debug
// 	TraceFile tf = tsc.get_trace(Integer.parseInt(trace));
// 	new JTraceViewer(tf);
//       } else {
// 	System.err.println("** asynchronous load **");  // debug
// 	// TraceFile tf = tsc.get_trace(Integer.parseInt(trace), this);
//      }
    } else {
      new JTraceViewer(trace,offset,rc);
    }
  }

  void setup () {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setTitle(tp.filename);

    add("Center", tp);
    //    System.out.println("before pack");  // debug
    pack();
    //    System.out.println("after " + System.currentTimeMillis());
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    setSize((int) (d.width * 0.65), (int) (d.height * 0.35));
    //    show();
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

  public void go_to(TraceFile tf, int offset) {
    setTitle(Funk.Str.basename(tf.name));
    tp.go_to(tf, offset);
    //show();
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
