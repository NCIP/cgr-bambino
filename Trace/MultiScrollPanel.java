package Trace;
// FIX ME: move to Funk














//      DELETE ME, DO NOT EDIT!!!




































import java.awt.*;
import java.awt.event.*;
import javax.swing.BoxLayout;

import java.util.Vector;
import java.util.Iterator;

public class MultiScrollPanel extends Panel implements ComponentListener,AdjustmentListener {
    private Panel component_holder;
    private Scrollbar scrollbar;
    private Vector<Component> components = new Vector<Component>();

    public MultiScrollPanel () {
      System.err.println("Trace.MultiScrollPanel OBSOLETE!!!");  // debug
      System.exit(1);
	setup();
    }

    public Component add (Component c) {
	component_holder.add(c);
	// add component to the internal panel
	components.add(c);
	// save to internal list.  We can't just use Component.getComponents()
	// because scrollbar operations dynamically add/remove components
	// to the internal panel.
	return c;
    }

    private void setup () {
	addComponentListener(this);
	component_holder = new Panel();
	component_holder.setLayout(new BoxLayout(component_holder, BoxLayout.Y_AXIS));
	// internal panel containing all added components

	setLayout(new BorderLayout());
	add("Center", component_holder);
	scrollbar = new Scrollbar(Scrollbar.VERTICAL, 1, 1, 1, 1);
	scrollbar.addAdjustmentListener(this);
	add("East", scrollbar);
    }

  public void validate () {
    System.err.println("VALIDATE");  // debug
    handle_resize();
    super.validate();
  }

    public void handle_resize () {
	Dimension d;
	
	System.err.println("**RESIZE**");  // debug

	int total_components = components.size();
	
	int panel_heights = 0;
	// height of all panels (on and offscreen)
	int panel_height = 0;
	
	for (Component c : components) {
	    d = c.getSize();
	    System.err.println("  panel size:" + d);  // debug
	    panel_heights += d.height;
	    panel_height = d.height;
	}
	
	if (panel_heights == 0) return;

	System.err.println("panel h=" + panel_heights);  // debug

	int holder_height = component_holder.getSize().height;
	// height of visible container

	System.err.println("hh:" + holder_height + " ph:" +panel_heights);  // debug
	int visible = ((holder_height * total_components) / panel_heights);
	System.err.println("visible:" + visible);  // debug

	System.err.println("container:" + holder_height);  // debug

	//	scrollbar.setValues(0, visible, 0, total_panels - 1);
	int value = 0;
	int minimum = 0;
	int maximum = total_components;

	scrollbar.setValues(value, visible, minimum, maximum);
	System.err.println("SB set: val=" + value + " vis:" + visible + " min:" + minimum + " max:" + maximum);  // debug

	System.err.println("setting scrollbar to: " +
			   value + "," +
			   visible + "," +
			   minimum + "," +
			   maximum);
	// value, visible amount, minimum, maximum

	int fit = (int) (holder_height / panel_height);
	if (fit < 1) fit = 1;
	if (fit > total_components) fit = total_components;

	// minimum snap
	int leftover = holder_height % panel_height;
	if (leftover > 0) {
	    // snap required
	    System.err.println("SNAPPING");  // debug
	    int new_h = (fit * panel_height);
	    component_holder.setSize(component_holder.getWidth(), new_h);
	    validate();
	}
    }

    // begin ComponentListener stubs
    public void componentResized(ComponentEvent e) {
	Dimension d = getSize();
	System.err.println("RESIZE: w=" + d.width + " h=" + d.height);  // debug
	handle_resize();
    }
    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    // end ComponentListener stubs

    // begin AdjustmentListener stubs
    public void adjustmentValueChanged(AdjustmentEvent e) {
	//
	//  scrollbar updated: change components in view
	//
	System.err.println("scroll value=" + scrollbar.getValue());  // debug

	int start_visible = scrollbar.getValue();
	int end_visible = start_visible + scrollbar.getVisibleAmount();
      
	int i = 0;
	component_holder.removeAll();
	// only include components which are currently visible in the view.
	// tried setting visibility of individual components, but this
	// didn't have the desired effect (their space reserved even though
	// invisible?)
	for (Component c : components) {
	    if (i >= start_visible && i <= end_visible) {
		component_holder.add(c);
	    }
	    i++;
	}
	component_holder.validate();
	// reset layout

	System.err.println("vis start:" + start_visible + " end:" + end_visible);  // debug

    }
    // end AdjustmentListener stubs


}
