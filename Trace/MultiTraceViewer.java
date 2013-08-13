package Trace;
// view multiple traces in a single window

import java.awt.*;
import java.awt.event.*;

import javax.swing.BoxLayout;

import java.util.Vector;
import java.util.Iterator;

import Funk.MultiScrollPanel;

public class MultiTraceViewer extends Funk.CloseFrame {

  public MultiTraceViewer(Vector<String> traces) {
    Vector<TracePanel> tps = new Vector<TracePanel>();
    for (String s : traces) {
      tps.add(new TracePanel(s));
    }
    setup(tps);
  }

  public static void main (String [] argv) {
    StreamDelegator.guess_compression();
    StreamDelegator.set_local(true);

    Vector<String> v = new Vector<String>();
    v.add("ye50d01.s1");
    v.add("ye50d01.r1");
    new MultiTraceViewer(v);
  }

  void setup (Vector<TracePanel> tps) {
    Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
    int preferred_width = (int) (ss.width * 0.5);
    int preferred_panel_height = (int) (ss.height * 0.24);

    MultiScrollPanel msp = new MultiScrollPanel();
    for (TracePanel tp : tps) {
      tp.setPreferredSize(new Dimension(preferred_width, preferred_panel_height));
      tp.setMinimumSize(new Dimension(preferred_width, preferred_panel_height));
      tp.setMaximumSize(new Dimension(ss.width, preferred_panel_height));

      msp.add(tp);
    }

    setLayout(new BorderLayout());
    add("Center", msp);
    pack();

    setVisible(true);
  }
}

