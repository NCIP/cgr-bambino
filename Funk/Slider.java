package Funk;

import java.awt.*;
import java.util.Observer;
import java.awt.event.*;

public class Slider extends Canvas implements MouseMotionListener,MouseListener {

  private static final int BAR_DELTA_Y = 1;
  private static final int GADGET_WIDTH = (BAR_DELTA_Y * 2) * 4;
  private static final int BORDER = 2;
  // border pixels

  private double min,max,value,value_range;
  private int slide_range, slide_left_border, slide_right_border;
  private Image offscreen;
  private Rectangle line,gadget;
  private Dimension size;

  private DoWhatISayObservable notifier;
  private Observer o;

  public Slider (int width, int height, double min, double max) {
    notifier = new DoWhatISayObservable();
    size = new Dimension(width,height);

    addMouseMotionListener(this);
    addMouseListener(this);

    this.value = this.min = min;
    this.max = max;
    value_range = max - min;

    int y_center = size.height / 2;
    slide_left_border = BORDER;
    int right = size.width - (BORDER * 2);
    line = new Rectangle(slide_left_border, y_center - BAR_DELTA_Y,
				   right - slide_left_border, (BAR_DELTA_Y * 2) + 1);
    slide_right_border = right - GADGET_WIDTH;
    slide_range = slide_right_border - slide_left_border;

    gadget = new Rectangle(0, BORDER, GADGET_WIDTH, size.height - (BORDER * 2));
    setSize(size.width, size.height);
  }

  public void addObserver (Observer o) {
    notifier.addObserver(o);
  }

  public double getValue() {
    return value;
  }

  public void setValue (double i) {
    if (i < min) i = min;
    if (i > max) i = max;
    value = i;
    repaint();
  }

  public void update (Graphics g) {
    paint(g);
  }

  public void paint (Graphics g) {
    // double-buffered paint/update
    if (offscreen == null) offscreen = createImage(size.width,size.height);
    Dimension d = getSize();

    // convert the value to a position on the slider:
    double fract = (double) (value - min) / (double) (value_range);
    gadget.x = slide_left_border + (int) (slide_range * fract);

    Graphics og = offscreen.getGraphics();
    og.setColor(getBackground());
    og.fillRect(0,0,size.width,size.height);
    draw_rect(line, og, Color.white);
    draw_rect(gadget, og, Color.white);
    g.drawImage(offscreen,0,0,this);
  }

  private void mousevalue (int x) {
    if (x < slide_left_border) x = slide_left_border;
    if (x > slide_right_border) x = slide_right_border;
    //    System.out.println("x:" +x + " lb:" + slide_left_border + " rb:"+slide_right_border);
    double mult = (double) (x - slide_left_border) / (double) slide_range;
    value = min + (value_range * mult);
    //    System.out.println("value:" + value);  // debug

    notifier.setChanged();
    notifier.notifyObservers(new Double(value));
    repaint();
  }

  //  public boolean mouseDrag (Event e, int x, int y) {
  //    mousevalue(x);
  //    return true;
  //  }

  //  public boolean mouseDown (Event e, int x, int y) {
  //    mousevalue(x);
  // return true;
  //}

  private void draw_rect (Rectangle r, Graphics g, Color c) {
    g.setColor(Color.black);
    g.drawRect(r.x, r.y, r.width, r.height);
    g.setColor(c);
    g.fillRect(r.x + 1, r.y + 1, r.width - 1, r.height - 1);
  }

  public void mouseDragged(MouseEvent e) {
    mousevalue(e.getX());
  }

  public void mousePressed(MouseEvent e) {
    mousevalue(e.getX());
  }

  public void mouseMoved(MouseEvent e) {};
  public void mouseClicked(MouseEvent e) {};
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
}
