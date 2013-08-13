package TCGA;

import java.util.*;

public class CommentSection {
  public String section_name;
  public HashSet<String> labels;

  public CommentSection (String[] stuff) {
    labels = new HashSet<String>();
    for (int i = 0; i < stuff.length; i++) {
      if (i == 0) {
	section_name = new String(stuff[i]);
      } else {
	labels.add(new String(stuff[i]));
      }
    }
    //    System.err.println("new section: " + section_name + " " + labels);  // debug
  }

}