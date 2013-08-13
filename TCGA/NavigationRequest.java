package TCGA;
import java.awt.Rectangle;
import java.util.*;

public class NavigationRequest {
  // a request to navigate the viewer
  public String marker, sample;
  public Pathway pathway;
  public Integer bin_index;
  public Rectangle selection;
  public ArrayList<Integer> bin_index_list;

  public boolean wants_zoom = false;
  public boolean wants_sort = false;
}
