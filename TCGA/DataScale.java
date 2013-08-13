package TCGA;

import java.util.*;
import java.text.ParseException;

public class DataScale {
  public static final String DATA_SCALE_TAG = "data_scale";
  int value;
  Float lower_bound, upper_bound;
  String lower_comparator, upper_comparator;

  public DataScale (String raw) throws ParseException {
    // #data_scale=-4:0<=v<0.3
    // #data_scale=-3:0.3<=v<0.8
    // #data_scale=-2:0.8<=v<1.3
    // #data_scale=-1:1.3<=v<1.8
    // #data_scale=0:1.8<=v<2.2
    // #data_scale=1:2.2<=v<2.7
    // #data_scale=2:2.7<=v<3.2
    // #data_scale=3:3.2<=v<3.7
    // #data_scale=4:3.7<=v<4.2
    // #data_scale=5:4.2<=v<6
    // #data_scale=6:6<=v<8
    // #data_scale=7:8<=v<10
    // #data_scale=8:10<=v<12
    // #data_scale=9:12<=v<14
    // #data_scale=10:14<=v<16
    // #data_scale=11:16<=v<18
    // #data_scale=12:18<=v<20
    // #data_scale=13:20<=v
    String error = null;
    try {
      String[] f = raw.split(":");
      if (f.length == 2) {
	value = Integer.parseInt(f[0]);

	String[] levels = f[1].split("[<>=]+");
	//
	// the numeric values (bounds), excluding comparators
	//
	if (levels.length >= 2 && levels.length <= 3) {
	  if (levels[1].equals("v")) {
	    lower_bound = Float.parseFloat(levels[0]);
	    if (levels.length == 3) upper_bound = Float.parseFloat(levels[2]);
	  } else {
	    error = "expected literal v in second list position";
	  }
	} else {
	  error = "array size error for bounds";
	}

	String[] comps = f[1].split("[\\-\\d\\.v]+");
	if (comps.length >= 2 && comps.length <= 3) {
	  if (comps[0].length() == 0) {
	    //	    lower_comparator = comps[1];

	    // hack: reverse direction of first comparator since
	    // "v" (value) is in the middle, after this 
	    if (comps[1].equals("<")) {
	      lower_comparator = ">";
	    } else if (comps[1].equals("<=")) {
	      lower_comparator = ">=";
	    } else {
	      lower_comparator = comps[1];
	    }
	    
	    if (comps.length == 3) upper_comparator = comps[2];
	  } else {
	    error = "first null expected";
	  }
	} else {
	  error = "array size error for comparators";
	}
      } else {
	error = "need 2 fields delimited by :";
      }
    } catch (Exception e) {
      error = "caught exception: " + e;
    }
    if (error == null) {
      // OK
      //System.err.println("raw="+raw+" => "+value + " " + lower_bound + " " + upper_bound + " " + lower_comparator + " " + upper_comparator);  // debug
    } else {
      throw new ParseException(error + " for " + raw, 0);
    }
  }

}

