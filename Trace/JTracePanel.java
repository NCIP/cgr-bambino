package Trace;

// container with scrollbar control for trace
// Swing version

import javax.swing.*;
//import java.awt.*;

import java.awt.event.*;
import java.awt.BorderLayout;

public class JTracePanel extends JPanel implements ActionListener,AdjustmentListener {
  String filename;

  private JTraceCanvas canvas;
  private boolean start_rc = false;
  private int start_position = 0;
  private TraceFile trace = null;
  private JButton center_button = null;
  private JPanel manual_panel = null;
  private String trace_label = null;
  private JScrollBar hs;

  private int last_value = 0;
  private boolean have_last_value = false;

  private static boolean is_applet = false;
  // obsolete: from AWT applet version

  public JTracePanel(String filename) {
    this.filename = filename;
    setup();
  }

  public JTracePanel(TraceFile t) {
    this.filename = t.name;
    trace = t;
    setup();
  }

  public JTracePanel(String filename, int position) {
    this.filename = filename;
    this.start_position = position;
    setup();
  }

  public JTracePanel(String filename, boolean reverse_complemented) {
    this.filename = filename;
    this.start_rc = reverse_complemented;
    setup();
  }

  public JTracePanel(TraceFile t, JPanel manual_panel) {
    this.filename = t.name;
    trace = t;
    this.manual_panel = manual_panel;
    setup();
  }

  public JTracePanel(TraceFile t, String trace_label) {
    trace = t;
    this.filename = t.name;
    this.trace_label = trace_label;
    setup();
  }

  public JTracePanel(String filename, int position, boolean reverse_complemented) {
    this.filename = filename;
    this.start_rc = reverse_complemented;
    this.start_position = position;
    setup();
  }

  void setup () {
    setLayout(new BorderLayout());

    JPanel cp = null;
    if (manual_panel != null) {
      cp = manual_panel;
    } else if (trace_label == null && 
               (start_position > 0 || is_applet == false)) {
      // if a "control panel" is necessary...
      cp = new JPanel();
      cp.setLayout(new BorderLayout());
      if (is_applet == false) {
	// button close this frame if not an applet
	cp.add("East", new Funk.JCloseButton());
      }
      if (start_position > 0) {
	// button to recenter the view on the startup position
	center_button = new JButton("Center");
	center_button.addActionListener(this);
	cp.add("West", center_button);
      }
    }
    if (cp != null) add("North", cp);

    //    hs = new JScrollBar(JScrollBar.HORIZONTAL, 1, 1, 1, 1);
    hs = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, 0);
    hs.addAdjustmentListener(this);

    if (trace == null) {
      // start from scratch
      canvas = new JTraceCanvas(filename, start_position, start_rc, hs);
    } else {
      // TraceFile data given
      canvas = new JTraceCanvas(trace, start_position, hs, trace_label);
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


  public JScrollBar get_scrollbar () {
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

