package TCGA;

import java.util.*;
import java.text.ParseException;

public class CommentOptions {
  private HashMap<String,String> options;
  private ArrayList<TrackInfo> tracks;
  private ArrayList<CombineInfo> combines;
  private TrackInfo current_track;
  private DataScales data_scales;
  private CombineInfo current_combine;
  private GISTIC gistic;

  public CommentOptions() {
    options = new HashMap<String,String>();
    put("genome", "on");
    // default unless specified otherwise
    tracks = new ArrayList<TrackInfo>();
    combines = new ArrayList<CombineInfo>();
  }

  public String get(String key) {
    return options.get(key);
  }

  public void put (String key, String value) {
    options.put(key,value);
  }

  
  public boolean has_option (String key) {
    return options.containsKey(key);
  }

  public boolean parse_line (String line) {
    boolean status = false;
    if (line == null || line.length() == 0) {
      System.err.println("WARNING: null line in input file");  // debug
    } else if (line.charAt(0) == '#') {
      status = true;
      int ei = line.indexOf('=');
      if (ei > 0) {
	String key = Funk.Str.trim_whitespace(line.substring(1,ei)).toLowerCase();
	String value = Funk.Str.trim_whitespace(line.substring(ei+1));
	
	if (key.equals(DataScale.DATA_SCALE_TAG)) {
	  if (data_scales == null) data_scales = new DataScales();
	  try {
	    data_scales.add(new DataScale(value));
	  } catch (ParseException e) {
	    System.err.println("ERROR parsing data scale: " + e);  // debug
	  }
	} else if (key.equals(GISTIC.GISTIC_TAG)) {
	  if (gistic == null) gistic = new GISTIC(this);
	  gistic.add(value);
	} else if (key.indexOf(CombineInfo.TAG_PREFIX) == 0) {
	  if (key.equals(CombineInfo.TAG_LABEL)) {
	    // a new specification is starting
	    combines.add(current_combine = new CombineInfo());
	  }
	  if (combines != null) current_combine.add(key,value);
	} else {
	  if (key.equals(TrackInfo.TRACK_START_KEY)) {
	    // a new track specification is starting
	    tracks.add(current_track = new TrackInfo());
	  }
	  if (current_track != null) current_track.add(key,value);
	  // hacky
	}

	options.put(key,value);
      } else {
	System.err.println("ignoring comment " + line);
      }
    }
    //    System.err.println("DEBUG, comment parse of " + line + ", return value=" + status);  // debug
    return status;
  }

  public boolean option_equals_lc (String key, String want_value) {
    String value = get(key);
    return value == null ? false : value.toLowerCase().equals(want_value);
  }

  public int[] get_int_list (String key) {
    // return an option as an array of integers
    int[] list = null;
    String s = get(key);
    if (s != null) {
      String[] f = s.split(",");
      list = new int[f.length];
      for (int i=0; i < f.length; i++) {
	list[i] = Integer.parseInt(f[i]);
      }
    }
    return list;
  }
  
  private int average(int[] list) {
    int result = 0;
    if (list != null) {
      int i;
      int total = 0;
      for (i=0; i < list.length; i++) {
	total += list[i];
      }
      result = (int) (total / list.length);
    }
    return result;
  }

  public int get_single_neutral_level () {
    return average(get_int_list("neutral"));
  }

  public ArrayList<TrackInfo> get_tracks() {
    return tracks;
  }

  public ArrayList<CombineInfo> get_combines() {
    return combines;
  }
  
  public DataScales get_data_scales() {
    return data_scales;
  }

  public GISTIC get_gistic () {
    return gistic;
  }

  public boolean has_gistic () {
    return gistic != null;
  }

  public boolean has_combines() {
    return combines != null && combines.size() > 0;
  }

  
}
