package Trace;

// container with scrollbar control for trace

import java.awt.*;
import java.awt.event.*;
import java.applet.Applet;

public class TracePanel extends Applet implements ActionListener,AdjustmentListener {
  String filename;

  private TraceCanvas canvas;
  private boolean start_rc = false;
  private int start_position = 0;
  private TraceFile trace = null;
  private boolean is_applet = false;
  private Button center_button = null;
  private Panel manual_panel = null;
  private String trace_label = null;
  private Scrollbar hs;

  private int last_value = 0;
  private boolean have_last_value = false;
  
  public void init () {
    // applet initialization
    is_applet = true;
    this.filename = getParameter("filename");
    //    this.start_position = (new Integer(getParameter("start"))).intValue();
    setup();
  }

  public TracePanel () {
    // applet initialization?
  }

  public TracePanel(String filename) {
    this.filename = filename;
    setup();
  }

  public TracePanel(TraceFile t) {
    this.filename = t.name;
    trace = t;
    setup();
  }

  public TracePanel(String filename, int position) {
    this.filename = filename;
    this.start_position = position;
    setup();
  }

  public TracePanel(String filename, boolean reverse_complemented) {
    this.filename = filename;
    this.start_rc = reverse_complemented;
    setup();
  }

  public TracePanel(TraceFile t, Panel manual_panel) {
    this.filename = t.name;
    trace = t;
    this.manual_panel = manual_panel;
    setup();
  }

  public TracePanel(TraceFile t, String trace_label) {
    trace = t;
    this.filename = t.name;
    this.trace_label = trace_label;
    setup();
  }

  public TracePanel(String filename, int position, boolean reverse_complemented) {
    this.filename = filename;
    this.start_rc = reverse_complemented;
    this.start_position = position;
    setup();
  }

  void setup () {
    setLayout(new BorderLayout());

    Panel cp = null;
    if (manual_panel != null) {
      cp = manual_panel;
    } else if (trace_label == null && 
               (start_position > 0 || is_applet == false)) {
      // if a "control panel" is necessary...
      cp = new Panel();
      cp.setLayout(new BorderLayout());
      if (is_applet == false) {
	// button close this frame if not an applet
	cp.add("East", new Funk.CloseButton());
      }
      if (start_position > 0) {
	// button to recenter the view on the startup position
	center_button = new Button("Center");
	center_button.addActionListener(this);
	cp.add("West", center_button);
      }
    }
    if (cp != null) add("North", cp);

    //    hs = new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, 1);
    hs = new Scrollbar(Scrollbar.HORIZONTAL, 0, 0, 0, 0);
    hs.addAdjustmentListener(this);

    if (trace == null) {
      // start from scratch
      canvas = new TraceCanvas(filename, start_position, start_rc, hs);
    } else {
      // TraceFile data given
      //      canvas = new TraceCanvas(trace, start_position, hs);
      canvas = new TraceCanvas(trace, start_position, hs, trace_label);
    }
    add("Center", canvas);
    add("South", hs);
  }

  public void center_on (int i) {
    if (canvas != null) canvas.center_on(i);
  }

  public void center_on (int i, boolean auto_rc) {
    if (canvas != null) canvas.center_on(i, auto_rc);
  }

  public void go_to(TraceFile tf, int offset) {
    canvas.go_to(tf, offset);
  }

  public void setPhd (PhdFile p) {
    canvas.setPhd(p);
  }

  public boolean loaded () {
    // is the TraceFile finished loading yet?
    return canvas.loaded();
  }

  public boolean error () {
    // error loading
    return canvas.error();
  }

  public TraceFile get_trace () {
    // get the TraceFile object used by the viewer
    return canvas.get_trace();
  }

  // begin ActionListener stubs 
  public void actionPerformed(ActionEvent e) {
    center_on(start_position);
    // center position
  }
  // end ActionListener stubs 


  // begin AdjustmentListener stubs
  public void adjustmentValueChanged(AdjustmentEvent e) {
    // System.out.println("adj val changed");  // debug
    canvas.repaint();
  }
  // end AdjustmentListener stubs


  public Scrollbar get_scrollbar () {
    return hs;
  }

  public synchronized void mimic_AdjustmentEvent (AdjustmentEvent e, int delta) {
    //  public void mimic_AdjustmentEvent (AdjustmentEvent e, int delta) {
    // mimic the effects of an AdjustmentEvent from another
    // scrollbar.  Useful for chaining/locking multiple scrollbars.
    int type = e.getAdjustmentType();
    int direction = 1;
    int amount = 0;
    
    if (type == AdjustmentEvent.UNIT_INCREMENT) {
      direction = 1;
      amount = hs.getUnitIncrement();
    } else if (type == AdjustmentEvent.UNIT_DECREMENT) {
      direction = -1;
      amount = hs.getUnitIncrement();
    } else if (type == AdjustmentEvent.BLOCK_INCREMENT) {
      direction = 1;
      amount = hs.getBlockIncrement();
    } else if (type == AdjustmentEvent.BLOCK_DECREMENT) {
      direction = -1;
      amount = hs.getBlockIncrement();
    } else if (type == AdjustmentEvent.TRACK) {
      // absolute change; use provided delta
      System.err.println("using delta: " + delta);  // debug
      direction = delta >= 0 ? 1 : -1;
      amount = Math.abs(delta);
    } else {
      System.err.println("WARNING: unhandled AdjustmentEvent type: " + e);  // debug
    }

    if (amount > 0) {
      hs.setValue(hs.getValue() + (amount * direction));
      canvas.repaint();
    }
  }

  public int get_bases_delta_since_start () {
    return canvas.get_bases_delta_since_start();
  }

  public void apply_absolute_delta (int d) {
    canvas.apply_absolute_delta(d);
  }


}

