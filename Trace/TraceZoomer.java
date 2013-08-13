package Trace;

import java.awt.*;
import java.awt.event.*;

public class TraceZoomer implements MouseWheelListener {

  private static double RANGE_MAX = 7.00;
  //  private static double RANGE_MIN = 0.50;
  private static double RANGE_MIN = 1.00;

  private static double STEP_SIZE = 0.65;
  private double zoom_level = TraceCanvas.DEFAULT_HEIGHT_TO_WIDTH_RATIO;
  
  // begin MouseWheelListener stubs
  public void mouseWheelMoved(MouseWheelEvent e) {
    int rotation = e.getWheelRotation();
    //    System.err.println("WHEEL: " + rotation);  // debug
    zoom(rotation);
  }

  public void zoom (int zoom) {
    zoom_level += STEP_SIZE * (double) zoom;
    set_zoom_level(zoom_level);
  }
  
  public void zoom_to_maximum() {
    set_zoom_level(RANGE_MIN);
  }

  public void zoom_to_minimum() {
    set_zoom_level(RANGE_MAX);
  }
  
  private void set_zoom_level (double zl) {
    // bounds checking
    zoom_level = zl;
    if (zoom_level < RANGE_MIN) zoom_level = RANGE_MIN;
    if (zoom_level > RANGE_MAX) zoom_level = RANGE_MAX;
    TraceCanvas.set_width_to_height_ratio(zoom_level);
    JTraceCanvas.set_width_to_height_ratio(zoom_level);
  }

  public void reset () {
    set_zoom_level(TraceCanvas.DEFAULT_HEIGHT_TO_WIDTH_RATIO);
  }

}
