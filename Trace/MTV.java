package Trace;
// view multiple traces in a single window

import java.awt.*;
import java.awt.event.*;

import javax.swing.BoxLayout;

import java.util.Vector;
import java.util.Iterator;

public class MTV extends Funk.CloseFrame implements ComponentListener,AdjustmentListener {
    private Vector<TracePanel> tps;
    private Scrollbar scrollbar;
    private Panel trace_holder;

    private int vertical_overhead = -1;
    private int preferred_panel_height;

  public MTV(Vector<String> traces) {
      //      addComponentListener(this);
      setup(traces);
  }

  public static void main (String [] argv) {
    // when run as a standalone app
    // ...how will the application end "naturally"?
    // ie if window is closed, program is still running...why?
      // notification??
    
    StreamDelegator.guess_compression();
    StreamDelegator.set_local(true);
    
    Vector<String> v = new Vector<String>();
    v.addElement("ye50d01.s1");
    v.addElement("ye50d01.r1");
    v.addElement("ye50d01.s1");

    new MTV(v);
  }

    void setup (Vector<String> traces) {
	MultiScrollPanel msp = new MultiScrollPanel();

	Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
	int preferred_width = (int) (ss.width * 0.65);
	int preferred_height = (int) (ss.height * 0.85);

	preferred_panel_height = (int) (ss.height * 0.24);

	Vector<TracePanel> tps = new Vector<TracePanel>();

	for (String s : traces) {
	    System.err.println("DEBUG: " + s);  // debug
	    TracePanel tp = new TracePanel(s);
	    
	    tp.setPreferredSize(new Dimension(preferred_width, preferred_panel_height));
	    tp.setMinimumSize(new Dimension(preferred_width, preferred_panel_height));
	    tp.setMaximumSize(new Dimension(ss.width, preferred_panel_height));

	    msp.add(tp);
	}

	setLayout(new BorderLayout());
	add("Center", msp);

	pack();

	//	setSize((int) (ss.width * 0.65), (int) (ss.height * 0.75));
	setVisible(true);
	//	handle_resize();
    }

    public void handle_resize () {
	Dimension d;
	
	System.err.println("**RESIZE**");  // debug

	int total_panels = tps.size();
	
	int panel_heights = 0;
	// height of all panels (on and offscreen)
	int panel_height = 0;

	for (TracePanel tp : tps) {
	    d = tp.getSize();
	    System.err.println("  panel size:" + d);  // debug
	    panel_heights += d.height;
	    panel_height = d.height;
	}

	Dimension screen = getSize();

	System.err.println("panel h=" + panel_heights);  // debug

	int holder_height = trace_holder.getSize().height;
	// height of visible container

	System.err.println("hh:" + holder_height + " ph:" +panel_heights);  // debug
	int visible = ((holder_height * total_panels) / panel_heights);
	System.err.println("visible:" + visible);  // debug

	System.err.println("screen size:" + screen);  // debug
	System.err.println("container:" + holder_height);  // debug

	//	scrollbar.setValues(0, visible, 0, total_panels - 1);
	int value = 0;
	int minimum = 0;
	int maximum = total_panels;

	scrollbar.setValues(value, visible, minimum, maximum);
	System.err.println("setting scrollbar to: " +
			   value + "," +
			   visible + "," +
			   minimum + "," +
			   maximum);
	// value, visible amount, minimum, maximum

	if (vertical_overhead == -1) {
	    vertical_overhead = screen.height - panel_heights;
	    System.err.println("OVERHEAD=" + vertical_overhead);  // debug
	}

	int sh = screen.height - vertical_overhead;
	int fit = (int) (sh / panel_height);
	if (fit < 1) fit = 1;
	if (fit > total_panels) fit = total_panels;

	// minimum snap
	int leftover = sh % panel_height;
	System.err.println("overhead:" + vertical_overhead + " panel:" + panel_height + " fit:" + fit + " left:" + leftover);  // debug
	if (leftover > 0) {
	    // snap required
	    System.err.println("RESIZING");  // debug
	    int new_h = (fit * panel_height) + vertical_overhead;
	    setSize(screen.width, new_h);
	}
    }

  // begin AdjustmentListener stubs
  public void adjustmentValueChanged(AdjustmentEvent e) {
      //
      //  scrollbar updated: change components in view
      //
      System.err.println("scroll value=" + scrollbar.getValue());  // debug

      int start_visible = scrollbar.getValue();
      int end_visible = start_visible + scrollbar.getVisibleAmount();
      
      int i = 0;
      trace_holder.removeAll();
      // only include components which are currently visible in the view.
      // tried setting visibility of individual components, but this
      // didn't have the desired effect (their space reserved even though
      // invisible?)
      for (TracePanel tp : tps) {
	  if (i >= start_visible && i <= end_visible) {
	      trace_holder.add(tp);
	  }
	  i++;
      }
      trace_holder.validate();
      // reset layout

      System.err.println("vis start:" + start_visible + " end:" + end_visible);  // debug

  }
  // end AdjustmentListener stubs


// begin ComponentListener stubs
public void componentResized(ComponentEvent e) {
    Dimension d = getSize();
    System.err.println("resize: w=" + d.width + " h=" + d.height);  // debug
    handle_resize();
}

public void componentHidden(ComponentEvent e) {}
public void componentMoved(ComponentEvent e) {}
public void componentShown(ComponentEvent e) {}
// end ComponentListener stubs
  

}
