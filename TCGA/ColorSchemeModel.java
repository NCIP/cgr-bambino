package TCGA;

import java.awt.Color;

public class ColorSchemeModel {
  public Color up_color, down_color;
  public int[] gradients;
  public int min_intensity_percent;
  public boolean white_mode;

  public ColorSchemeModel() {
  }

  public ColorSchemeModel(Color up, Color down) {
    up_color = up;
    down_color = down;
    set_defaults();
  }
  
  public void set_defaults() {
    white_mode = !Options.DEFAULT_BACKGROUND_BLACK;
    // broken: should be from HeatmapConfiguration...
    gradients = Options.DEFAULT_COPYNUMBER_GRADIENTS;
    min_intensity_percent = Options.DEFAULT_MIN_COLOR_INTENSITY_PERCENT;
  }

}
