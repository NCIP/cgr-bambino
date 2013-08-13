package Funk;

import java.util.*;
import java.awt.*;

// Grid:
// Widget of gridded cells of uniform size.
//
// Needs work!: if GridItems are wider than cells...
//
// mne 12/1997

public class Grid extends Canvas {
  private Vector x_labels, y_labels;
  private FontMetrics fm;
  private int x_cell_width, y_cell_width, y_cell_height;
  private int x_cell_origin, y_cell_origin;
  private int x_cell_count, y_cell_count;
  private int font_height, x_label_border;
  private GridItem [][] grid_items = null;
  private static Color BACKGROUND_COLOR = new Color(238, 203, 173);
  // X11 "peachpuff2"
  
  public Grid (Vector x_labels, Vector y_labels, Font font) {
    setFont(font);
    fm = getFontMetrics(font);
    font_height = fm.getHeight();
    y_cell_height = (int) (font_height * 2);
    y_cell_origin = y_cell_height;

    this.x_labels = x_labels;
    this.y_labels = y_labels;
    x_cell_width = (int) (get_max_stringlen(x_labels) * 1.2);
    y_cell_width = (int) (get_max_stringlen(y_labels) * 1.8);
    x_label_border = (int) (y_cell_width * 0.10);
    x_cell_origin = (int) (y_cell_width + x_label_border);
    x_cell_count = x_labels.size();
    y_cell_count = y_labels.size();

    int max_x = x_cell_origin + (x_cell_width * (x_cell_count + 1));
    int max_y = y_cell_origin + (y_cell_height * (y_cell_count + 1));
    setSize(max_x, max_y);
    // cell sizes based on labels only: 
    // will break if cell data is "wider" than that...
  }

  public void paint (Graphics g) {
    //    g.setColor(Color.white);
    g.setColor(BACKGROUND_COLOR);
    Dimension d = getSize();
    g.fillRect(0,0,d.width,d.height);

    g.setColor(Color.black);
    int i;
    for (i=0; i < x_cell_count; i++) {
      // draw labels for X cells, centered over each cell
      String label = (String) x_labels.elementAt(i);
      g.drawString(label,
		   center_x_point(i, label), y_cell_origin - (font_height / 2));
    }

    for (i=0; i < y_cell_count; i++) {
      // draw labels for Y cells, right-aligned
      String label = (String) y_labels.elementAt(i);
      int x = x_cell_origin - fm.stringWidth(label) - x_label_border;
      //      int y = y_cell_origin + (y_cell_height * i) + (y_cell_height / 2);
      int y = center_y_point(i);
	
      g.drawString(label, x, y);
      //      g.drawLine(x - 5, y - 5, x+5, y+5);
      //      g.drawLine(x + 5, y - 5, x -5, y + 5);
    }


    // draw vertical lines delimiting cells on X axis
    for (i=0; i <= x_cell_count; i++) {
      int x = x_cell_origin + (x_cell_width * i);
      g.drawLine(x, y_cell_origin,
		 x, y_cell_origin + (y_cell_count * y_cell_height));
    }

    // draw horizontal lines delimiting cells on Y axis
    for (i=0; i <= y_cell_count; i++) {
      int y = y_cell_origin + (y_cell_height * i);
      g.drawLine(x_cell_origin, y,
		 x_cell_origin + (x_cell_width * x_cell_count), y);
    }

    // paint items
    int j;
    GridItem gi;
    if (grid_items != null) {
      for (i=0; i < x_cell_count; i++) {
	for (j=0; j < y_cell_count; j++) {
	  gi = grid_items[i][j];
	  g.setColor(gi.color);
	  g.drawString(gi.data, center_x_point(i, gi.data),
		       center_y_point(j));
	}
      }
    }
    
  }

  public void set_grid_items (GridItem [][] grid_items) {
    this.grid_items = grid_items;
    repaint();
  }

  private int get_max_stringlen (Vector v) {
    int i, max;
    max = 0;
    Enumeration e = v.elements();
    while (e.hasMoreElements()) {
      String s = (String) e.nextElement();
      i = fm.stringWidth(s);
      //      System.out.println(s + "=" + i);  // debug
      if (i > max) max = i;
    }
    return max;
  }

  private int center_x_point (int index, String s) {
    // given a cell X index and a string, return X offset
    // to draw the string centered for that cell.
    return x_cell_origin +
      (x_cell_width * index) +
      (x_cell_width / 2) -
      (fm.stringWidth(s) / 2);
  }

  private int center_y_point (int index) {
    // given a cell Y index, return Y offset to center text in that cell.
    return(y_cell_origin + (y_cell_height * index) + (y_cell_height / 2));
  }

}
