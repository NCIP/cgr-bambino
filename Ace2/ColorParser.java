package Ace2;

import java.awt.*;
import java.lang.reflect.Field;

public class ColorParser {
  Color c = null;

  public ColorParser (String cspec) {
    try {
      Field field = Class.forName("java.awt.Color").getField(cspec);
      // try to look up given name as predefined Color field from Color class.
      // choices are (CASE SENSITIVE):
      //   black
      //   blue
      //   cyan
      //   darkGray
      //   gray
      //   green
      //   lightGray
      //   magenta
      //   orange
      //   pink
      //   red
      //   white
      //   yellow
      c = (Color) field.get(null);
    } catch (Exception e) {}

    if (c == null && cspec.indexOf(",") != -1) {
      String[] rgb = cspec.split(",");
      if (rgb.length == 3) {
	System.err.println("ok");  // debug
	c = new Color(
		      Integer.parseInt(rgb[0]),
		      Integer.parseInt(rgb[1]),
		      Integer.parseInt(rgb[2])
		      );
      } else {
	System.err.println("error: specify color as R,G,B");  // debug
	System.exit(1);
	// feh
      }
    }
  }

  public Color get_color() {
    return c;
  }

}

