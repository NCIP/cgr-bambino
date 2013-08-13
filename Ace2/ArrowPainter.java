package Ace2;

import java.awt.*;

class ArrowPainter {
  protected int total_width;  // ? how to make read-only ?
  int left_margin = 0;

  private int buffer, width, height, x, baseline, y_modifier;
  private int [] arrow_x = new int[3];
  private int [] arrow_y = new int[3];
  private Color arrow_color;
  
  ArrowPainter (int font_width, int line_height, int font_ascent,
		int font_descent, Color arrow_color) {
    this.arrow_color = arrow_color;
    buffer = (int) (font_width * 0.40);
    width = font_width * 2;
    height = (int) ((line_height * 0.60) / 2);
    // delta-y in each direction from the baseline (half the true height)
    y_modifier =  0 - font_ascent + ((font_ascent + font_descent) / 2);
    total_width = width + (buffer * 4);
  }

  void draw (Graphics gr, boolean rc, int y, boolean cancel_mode) {
    Graphics2D g = (Graphics2D) gr;
    Color old_color = g.getColor();
    Stroke old_stroke = g.getStroke();

    Object last_hint = g.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);
    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

    g.setColor(arrow_color);
    x = left_margin + buffer;
    //    baseline = (y - font_ascent) + ((font_ascent + font_descent) / 2);
    baseline = y + y_modifier;
    if (rc) {
      arrow_x[0] = x + width;
      arrow_y[0] = baseline - height;
      arrow_x[1] = x + width;
      arrow_y[1] = baseline + height;
      arrow_x[2] = x;
      arrow_y[2] = baseline;
    } else {
      arrow_x[0] = x;
      arrow_y[0] = baseline - height;
      arrow_x[1] = x;
      arrow_y[1] = baseline + height;
      arrow_x[2] = x + width;
      arrow_y[2] = baseline;
    }
    g.setStroke(new BasicStroke(1.25f));
    if (cancel_mode) {
      g.drawPolygon(arrow_x, arrow_y, 3);
    } else {
      g.fillPolygon(arrow_x, arrow_y, 3);
    }

    //    if (cancel_mode) {
    //      g.setColor(Color.red);
    //      g.drawPolygon(arrow_x, arrow_y, 3);
    //    }

    g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, last_hint);
    g.setColor(old_color);
    g.setStroke(old_stroke);
  }

}
