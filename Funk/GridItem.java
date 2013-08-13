package Funk;

import java.awt.*;

public class GridItem {
  public String data = null;
  public Color color = Color.black;

  public GridItem(String s) {
    data = s;
  }

  public GridItem(char c) {
    data = (new Character(c)).toString();
  }

  public GridItem(int i) {
    data = Integer.toString(i);
  }

  public GridItem(String s, Color c) {
    data = s;
    color = c;
  }
  
  public void set_color(Color c) {
    color = c;
  }
}
