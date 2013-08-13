package Ace2;

import java.util.*;
import java.util.regex.*;
import net.sf.samtools.*;
import net.sf.samtools.SAMRecord.*;

import Funk.NumberComparator;
import Funk.LogicalComparator;

public class SAMTagFilter {

  private HashMap<String,LogicalComparator> required_tags;
  private static String SET_DELIMITER = "/";
  // value used to specify a set of acceptable values
  private boolean all_tags_required;

  public SAMTagFilter (boolean all_tags_required) {
    this.all_tags_required = all_tags_required;
  }

  public HashMap<String,LogicalComparator> parse (String s) {
    required_tags = new HashMap<String,LogicalComparator>();
    String[] pairs = s.split(",");
    Pattern p = Pattern.compile("^(\\w\\w)(<=|!=|>=|=|<|>)(.*)", Pattern.CASE_INSENSITIVE);
    // 1. tag
    // 2. operator
    // 3. value
    for (int li=0; li < pairs.length; li++) {
      Matcher m = p.matcher(pairs[li]);
      if (m.find()) {
	String tag_name = m.group(1);
	String tag_operator = m.group(2);
	String tag_value = m.group(3);

	LogicalComparator lc = null;
	try {
	  // required value is a number
	  Double d = Double.parseDouble(tag_value);
	  lc = NumberComparator.get_comparator(tag_operator, d);
	  System.err.println("value="+d  + " comp="+lc);  // debug
	} catch (NumberFormatException e) {
	  // required value is a String
	  if (tag_operator.equals("=")) {
	    if (tag_value.indexOf(SET_DELIMITER) != -1) {
	      // set of acceptable values
	      lc = new Funk.StringComparatorEQSet(tag_value.split(SET_DELIMITER));
	    } else {
	      // single value
	      lc = new Funk.StringComparatorEQ(tag_value);
	    }
	  } else if (tag_operator.equals("!=")) {
	    if (tag_value.indexOf(SET_DELIMITER) != -1) {
	      // set of unacceptable values
	      lc = new Funk.StringComparatorNESet(tag_value.split(SET_DELIMITER));
	    } else {
	      // single unacceptable value
	      lc = new Funk.StringComparatorNE(tag_value);
	    }
	  } else {
	    System.err.println("ERROR: string operators may only be = or !=");
	    System.exit(1);
	  }
	}

	if (lc == null) {
	  System.err.println("ERROR: no SAM tag comparator");  // debug
	  System.exit(1);
	} else {
	  required_tags.put(tag_name, lc);
	}
      } else {
	System.err.println("ERROR: -require-tags format is TAG=VALUE[,TAG=VALUE...] where operators are <,<=,=,>=,>");  // debug
	System.exit(1);
      }
    }

    return required_tags;
  }

  public boolean check (SAMRecord sr, Counter counter) {
    HashSet<String> ok_tags = new HashSet<String>();
    boolean failed = false;
    for (SAMTagAndValue tav : sr.getAttributes()) {
      //	  System.err.println("tag="+tav.tag + " = " + tav.value);  // debug
      if (required_tags.containsKey(tav.tag)) {
	// read has a required tag
	LogicalComparator lc  = required_tags.get(tav.tag);

	if (lc.compare(tav.value)) {
	  // read's value matches required value
	  //	      System.err.println("HIT for " + tav.tag + " = " + tav.value);  // debug
	  ok_tags.add(tav.tag);
	} else {
	  //	  System.err.println("MISS for " + tav.tag + " = " + tav.value);  // debug
	  if (counter != null) counter.increment("tag " + tav.tag);
	  failed = true;
	}
      }
    }
    return !failed && (all_tags_required ? ok_tags.size() == required_tags.size() : true);
  }

 
}