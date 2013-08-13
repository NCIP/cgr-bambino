package Ace2;

import java.awt.*;

public class ColorIntensifier {
  int min_intensity_value = 0;
  int max_intensity_value;
  // range of input values; e.g. MAPQ ranges from 0 - 99

  //  float minimum_intensity_percent = 0.33f;
  public static float MINIMUM_INTENSITY_PERCENT = 0.50f;
  
  public void set_max_intensity_value(int v) {
    max_intensity_value = v;
  }

  public Color get_shaded_color (Color c_src, int value) {
    Color result = null;

    int range = max_intensity_value - min_intensity_value;

    float fraction = ((float) (value - min_intensity_value)) / range;
    // where this value falls in the dynamic range between
    // min_intensity_value and max_intensity_value

    float intensity_range = 1.0f - MINIMUM_INTENSITY_PERCENT;
    // dynamic range of available intensity

    float final_fraction = MINIMUM_INTENSITY_PERCENT + (intensity_range * fraction);

    if (final_fraction > 1f) final_fraction = 1.0f;
    // "that's unpossible!"

    int r = (int) (c_src.getRed() * final_fraction);
    int g = (int) (c_src.getGreen() * final_fraction);
    int b = (int) (c_src.getBlue() * final_fraction);

    //    System.err.println("v=" + value + " ff=" + final_fraction + " r="+r + " g="+g + " b="+b);  // debug

    result = new Color(r,g,b);

    return result;
  }

}
