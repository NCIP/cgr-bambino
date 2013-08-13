package TCGA;

import java.util.*;
import java.awt.*;

public class DividerSet extends HashSet<Integer> {
  public Color color;
  // divider line color 
  public float min_weight;
  // minimum divider line thickness
  public float max_weight;
  // maximum divider line thickness
  public float full_weight_at_zoom;
  // zoom level where full line thickness is reached
  public Stroke stroke = null;
  // custom Stroke (dashed line, etc.)

}