package Ace2;
// FIX ME: move to Funk
// MNE 3/2010

import java.util.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SuffixFileFilter extends javax.swing.filechooser.FileFilter {
  private ArrayList<String> suffixes;

  public SuffixFileFilter() {
    suffixes = new ArrayList<String>();
  }

  public SuffixFileFilter(String s) {
    suffixes = new ArrayList<String>();
    add_suffix(s);
  }

  public SuffixFileFilter(ArrayList<String> suffixes) {
    this.suffixes = suffixes;
  }

  public void add_suffix (String s) {
    suffixes.add(s);
  }

  public boolean accept (File f) {
    boolean ok = false;
    if (f.isDirectory()) {
      ok = true;
    } else {
      //	    Pattern pattern = Pattern.compile("\\.[sb]am$", Pattern.CASE_INSENSITIVE);
      for (String suffix : suffixes) {
	String p = "\\." + suffix + "$";
	Pattern pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
	Matcher matcher = pattern.matcher(f.toString());
	boolean result = matcher.find();
	//	System.err.println("file:" + f.toString() + " pattern:" + p + " result:" + result);  // debug
	if (result) ok = true;
      }
    }
    return ok;
  }

  public String getDescription() {
    ArrayList<String> descs = new ArrayList<String>();
    for (String suffix : suffixes) {
      descs.add("." + suffix);
    }

    return Funk.Str.join("/", descs);
  }

}
