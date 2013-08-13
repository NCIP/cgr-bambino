package Funk;
// my utility functions, mostly static

import java.awt.*;
import javax.swing.JFrame;

public class Gr {
  // static graphics-related functions
  
  public static void centerText (Component c, String message) {
    Graphics g = c.getGraphics();
    Dimension size = c.getSize();
    FontMetrics fm = g.getFontMetrics();
    //    Rectangle r = g.getClipRect();
    g.setColor(Color.black);
    g.fillRect(0, 0, size.width, size.height);
    g.setColor(Color.white);
    g.drawString(message,
		 (size.width / 2) - (fm.stringWidth(message) / 2),
		 (size.height / 2));
  }

  public static void centerText (Component c, Graphics g, String message) {
    Dimension size = c.getSize();
    FontMetrics fm = g.getFontMetrics();
    //    Rectangle r = g.getClipRect();
    g.setColor(Color.black);
    g.fillRect(0, 0, size.width, size.height);
    g.setColor(Color.white);
    g.drawString(message,
		 (size.width / 2) - (fm.stringWidth(message) / 2),
		 (size.height / 2));
  }

  public static Frame getFrame (Component c) {
    // get toplevel frame
    while (!(c instanceof Frame) && c != null) {
      c = c.getParent();
    }
    return (Frame) c;
  }

  public static JFrame getJFrame (Component c) {
    // get toplevel jframe
    while (!(c instanceof JFrame) && c != null) {
      c = c.getParent();
    }
    return (JFrame) c;
  }

  public static void anchor(Frame f, String orientation) {
    // move a Frame to a compass position on the screen.
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension d = f.getSize();
    Insets insets = f.getInsets();
    // window decoration 

    Borders b = new Borders();
    b.center_x = (screen.width / 2) - (d.width / 2);
    b.center_y = (screen.height / 2) - (d.height / 2);
    b.left = insets.left;
    b.right = screen.width - d.width - insets.right;
    b.top = insets.top;
    b.bottom = screen.height - d.height - insets.bottom;

    if (orientation.equals("n")) {
      f.setLocation(b.center_x, b.top);
    } else if (orientation.equals("ne")) {
      f.setLocation(b.right, b.top);
    } else if (orientation.equals("e")) {
      f.setLocation(b.right, b.center_y);
    } else if (orientation.equals("se")) {
      f.setLocation(b.right, b.bottom);
    } else if (orientation.equals("s")) {
      f.setLocation(b.center_x, b.bottom);
    } else if (orientation.equals("sw")) {
      f.setLocation(b.left, b.bottom);
    } else if (orientation.equals("w")) {
      f.setLocation(b.left, b.center_y);
    } else if (orientation.equals("nw")) {
      f.setLocation(b.left, b.top);
    } else {
      System.out.println("unimplemented orientation:" + orientation);  // debug
    }
  }

  public static Point get_root_xy(Component c, int x, int y) {
    // Translate an x,y coordinate in a component to its coordinates
    // in the top-level window.  Useful for converting a mouse-click
    // in a component to the root-window coordinates of position clicked.

    while (true) {
      Point p = c.getLocation();
      //      System.out.println("x=" + x + " y=" + y + " p=" + p + " c:" + c);  // debug
      x += p.x;
      y += p.y;
      //      System.out.println(c + " " + p);  // debug
      if (c instanceof Frame) {
	//	Insets i = ((Frame) c).getInsets();
	//	x += i.left;
	//	y += i.top;
	// don't need (?!?)
	break;
      } else {
	c = c.getParent();
      }
    }
    Point p = new Point(x, y); 

    return(p);
  }

  public static Point get_frame_border(Component c, String dir) {
    // given a component, return the screen coordinates of a 
    // specified "anchor" of that frame -- ie "ne" (northeast)
    // would return right-top point of the frame.
    Frame f = (c instanceof Frame) ? (Frame) c : getFrame(c);
    Borders b = get_frame_borders(f);
    
    Point result = null;
    if (dir.equals("n")) {
      result = new Point(b.center_x, b.top);
    } else if (dir.equals("ne")) {
      result = new Point(b.right, b.top);
    } else if (dir.equals("e")) {
      result = new Point(b.right, b.center_y);
    } else if (dir.equals("se")) {
      result = new Point(b.right, b.bottom);
    } else if (dir.equals("s")) {
      result = new Point(b.center_x, b.bottom);
    } else if (dir.equals("sw")) {
      result = new Point(b.left, b.bottom);
    } else if (dir.equals("w")) {
      result = new Point(b.left, b.center_y);
    } else if (dir.equals("nw")) {
      result = new Point(b.left, b.top);
    } else {
      System.out.println("bogus anchor " + dir + "!");
    }
    return result;
  }

  public static Borders get_frame_borders (Frame f) {
    // given a Frame, return a Borders object which contains 
    // the coordinates of the Frame on the screen.
    Dimension d = f.getSize();
    Point loc = f.getLocation();
    Insets insets = f.getInsets();

    Borders b = new Borders();
    b.left = loc.x - insets.left;
    b.right = loc.x + d.width + insets.right;
    b.top = loc.y - insets.top;
    b.bottom = loc.y + d.height + insets.bottom;
    b.center_x = loc.x + (d.width / 2);
    b.center_y = loc.y + (d.height / 2);
    return b;
  }

  public static void respectful_resize (Component c, double width, double height) {
    Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
    int min_width = (int) (d.width * width);
    if (min_width < c.getMinimumSize().width) min_width = c.getMinimumSize().width;
    // respect the minimum width taken by packed widgets
    c.setSize(min_width, (int) (d.height * height));
  }

}

class Borders {
  public int left,right,top,bottom,center_x,center_y;
}
