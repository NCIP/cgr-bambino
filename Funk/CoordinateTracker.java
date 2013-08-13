package Funk;

import java.awt.Rectangle;
import java.util.Vector;

public class CoordinateTracker {
  private Vector areas;
  private Vector objects;
  private int default_width;

  public CoordinateTracker () {
    areas = new Vector();
    objects = new Vector();
  }

  public void add (Rectangle r, Object o) {
    areas.addElement(r);
    objects.addElement(o);
  }

  public void add (int x, int y, int width, int height, Object o) {
    areas.addElement(new Rectangle(x,y,width,height));
    objects.addElement(o);
  }

  public void set_default_width (int i) {
    default_width = i;
  }

  public void reset () {
    areas.setSize(0);
    objects.setSize(0);
  }

  public Object find (int x, int y) {
    int end = areas.size();
    int i;
    Object result = null;
    for (i=0; i < end; i++) {
      if (((Rectangle) areas.elementAt(i)).contains(x,y)) {
	result = objects.elementAt(i);
	break;
      }
    }
    return result;
  }

}
