package Funk;
// FIX ME:
//  - possible to implement as layout manager?
//  - test for locked component height sizes!

import java.awt.*;
import java.awt.event.*;
import javax.swing.BoxLayout;

import java.util.Vector;
import java.util.Iterator;

public class MultiScrollPanel extends Panel implements ComponentListener,AdjustmentListener,MouseWheelListener {
  private Vector<Component> components = new Vector<Component>();
  // set of managed components: we can't just use Component.getComponents()
  // because scrollbar operations will dynamically add/remove components
  // from the internal panel.
  private Panel component_holder;
  // managed components will be added to this internal panel.
  private Scrollbar scrollbar;
  // navigation of managed components

  private boolean ENABLE_SNAP = false;
  private static int PREFERRED_HEIGHT_PERCENT = 100;

  public MultiScrollPanel () {
    setup();
  }

  public static void set_window_height_percent (int h) {
    PREFERRED_HEIGHT_PERCENT = h;
  }

  public Component add (Component c) {
    component_holder.add(c);
    // add to internal panel
    components.add(c);
    // should we validate() here?  call super()?
    return c;
  }

  public void removeAll() {
    // reset
    component_holder.removeAll();
    components.removeAllElements();
    // should we validate() here?  call super()?
  }

  public void validate() {
    handle_resize();
    super.validate();
  }

  private void setup () {
    addMouseWheelListener(this);
    addComponentListener(this);
    component_holder = new Panel();
    component_holder.setLayout(new BoxLayout(component_holder, BoxLayout.Y_AXIS));
    // internal panel containing all added components

    setLayout(new BorderLayout());
    add("Center", component_holder);
    //    scrollbar = new Scrollbar(Scrollbar.VERTICAL, 1, 1, 1, 1);
    scrollbar = new Scrollbar(Scrollbar.VERTICAL, 0, 0, 0, 0);
    scrollbar.addAdjustmentListener(this);
    add("East", scrollbar);
  }

  public synchronized void handle_resize () {
    Dimension d;
	
    int total_components = components.size();
	
    int panel_heights = 0;
    // height of all panels (on and offscreen)
    int panel_height = 0;
	
    for (Component c : components) {
      d = c.getSize();
      //      System.err.println("  panel size:" + d);  // debug
      panel_heights += d.height;
      panel_height = d.height;
    }
	
    if (panel_heights == 0) return;

    int holder_height = component_holder.getSize().height;
    // height of visible container

    //    System.err.println("hh:" + holder_height + " ph:" +panel_heights);  // debug
    int visible = ((holder_height * total_components) / panel_heights);
    // count of managed components which will fit onscreen

    //    System.err.println("holder h:" + holder_height + " panel h:" + panel_heights + " vis:" + visible);  // debug

    //    int value = 0;
    int value = scrollbar.getValue();
    // FIX ME: preserve/scale existing position!
    int minimum = 0;
    int maximum = total_components;
    //    int maximum = total_components - 1;

    if (value > maximum) value = maximum;

    scrollbar.setValues(value, visible, minimum, maximum);
    scrollbar.setBlockIncrement(visible);

    //    System.err.println("setting scrollbar to: " + value + "," + visible + "," + minimum + "," + maximum + "; block=" + scrollbar.getBlockIncrement());

    lay_out_components();

    if (ENABLE_SNAP) snap(false);

  }

  public void snap (boolean resize_to_max) {
    int total_components = components.size();
    int holder_height = component_holder.getSize().height;
    int panel_height = components.elementAt(0).getSize().height;

    int fit = (int) (holder_height / panel_height);

    if (resize_to_max) {
      //
      // try to fit as many components as possible onscreen,
      // in discrete units
      //
      Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
      Frame toplevel = Funk.Gr.getFrame(component_holder);

      int overhead = ss.height - (int) ((ss.height * PREFERRED_HEIGHT_PERCENT) / 100);
      // allow for reserving some vertical space, e.g. to avoid
      // scaling the window over the task bar in Windows

      if (toplevel != null) {
        int toplevel_height = toplevel.getSize().height;
        // height of toplevel frame component
        //        System.err.println("top height=" + toplevel_height);  // debug
        //        System.err.println("holder height=" + holder_height);  // debug
        //        System.err.println("panel height=" + panel_height);  // debug
        overhead += toplevel_height - holder_height;
        // approximate vertical overhead of components in the rest of
        // the frame.
      }
      
      fit = (int) ((ss.height - overhead) / panel_height);
      //      System.err.println("FIT=" + fit);  // debug
    }

    if (fit < 1) fit = 1;
    if (fit > total_components) fit = total_components;
    // don't allow container to be resized beyond the space needed
    // to display all components

    int leftover = holder_height % panel_height;
    
    if (leftover > 0) {
      // snap required
      Frame f = Funk.Gr.getFrame(this);
      int new_h = (fit * panel_height);
      component_holder.setPreferredSize(new Dimension(component_holder.getWidth(), new_h));
      f.pack();
      // rearrange frame based on new preferred size

    }
  }

  // begin ComponentListener stubs
  public void componentResized(ComponentEvent e) {
    Dimension d = getSize();
    handle_resize();
  }
  public void componentHidden(ComponentEvent e) {}
  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  // end ComponentListener stubs

  private synchronized void lay_out_components () {
    //    System.err.println("scroll value=" + scrollbar.getValue());  // debug

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

    //    System.err.println("vis start:" + start_visible + " end:" + end_visible);  // debug
  }

  // begin AdjustmentListener stubs
  public void adjustmentValueChanged(AdjustmentEvent e) {
    //
    //  scrollbar updated: change components in view
    //
    //    System.err.println("adj:" + e);  // debug
    lay_out_components();
  }
  // end AdjustmentListener stubs

  // begin MouseWheelListener stubs
  public void mouseWheelMoved(MouseWheelEvent e) {
    int rotation = e.getWheelRotation();
    int current_value = scrollbar.getValue();
    int new_value = scrollbar.getValue() + rotation;
    int min = scrollbar.getMinimum();
    int max = scrollbar.getMaximum();
    int vis = scrollbar.getVisibleAmount();
    int block = scrollbar.getBlockIncrement();
    
    boolean everything_visible = (current_value == min) && (current_value + vis >= max);
    if (new_value < min) new_value = min;
    if (new_value > max) new_value = max;
    //    System.err.println("new:" + new_value + " current:" + current_value + " max:" + max + " block:" + block);  // debug

    if (new_value != current_value && everything_visible == false) {
      scrollbar.setValue(new_value);
      lay_out_components();
    }
  }
  // end MouseWheelListener stubs

}
