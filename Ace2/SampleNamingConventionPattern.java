package Ace2;

import java.util.*;

public class SampleNamingConventionPattern {
  String type_string;
  // string which matches isolated tumor/normal type string
  TumorNormal tumor_normal;
  // whether indicates a tumor or normal sequence
  String description;
  // optional more description of subtype, 
  // i.e. "matching normal", etc.
  boolean is_recurrent;

  public SampleNamingConventionPattern (String type_string, TumorNormal tumor_normal, String description) {
    this.type_string = type_string;
    this.tumor_normal = tumor_normal;
    this.description = description;
    setup();
  }

  public SampleNamingConventionPattern (String type_string, TumorNormal tumor_normal) {
    this.type_string = type_string;
    this.tumor_normal = tumor_normal;
    this.description = tumor_normal.toString();
    setup();
  }

  private void setup() {
    is_recurrent = false;
    if (tumor_normal.equals(TumorNormal.TUMOR) &&
	description.toLowerCase().indexOf("recurrent") > -1) {
      // hack
      is_recurrent = true;
    }
  }

}
