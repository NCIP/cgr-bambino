package TCGA;

import java.util.*;

import java.awt.Color;

public class AnnotationGridData {
  AnnotationFlatfile2 af;
  AnnotationColorMapper2 acm;
  ArrayList<String> visible_fields;
  HashMap<Integer,String> index2field;

  public AnnotationGridData (AnnotationFlatfile2 af, Color annotation_color) {
    this.af = af;
    acm = new AnnotationColorMapper2(af, annotation_color);
  }
  
  public AnnotationFlatfile2 get_af() {
    return af;
  }

}
