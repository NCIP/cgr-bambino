package Ace2;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class MultiplexedWriter {
  //
  // maintain a set of PrintStreams based on a basename and key
  // - also provides a simple facility to write a list of values to a file by key
  //
  private String basename;
  private HashMap <String,PrintStream> key2ps;
  private HashMap <String,WorkingFile> key2wf;
  private HashMap <String,File> key2file;
  private HashMap <String,Integer> key2count;
  private boolean compress;
  private int BUCKET_SIZE = 0;

  private int MAX_OPEN_FILEHANDLES = 0;
  private boolean FILEHANDLE_LIMIT = false;
  private static final boolean VERBOSE = false;
  
  public MultiplexedWriter (String basename, boolean compress) {
    this.basename = basename;
    this.compress = compress;
    key2ps = new HashMap<String,PrintStream>();
    key2wf = new HashMap<String,WorkingFile>();
    key2file = new HashMap<String,File>();
    key2count = new HashMap<String,Integer>();
  }

  public PrintStream getPrintStream (String key) {
    PrintStream ps = key2ps.get(key);
    if (ps == null) {
      //      System.err.println("WF filename=" + get_filename(key));  // debug
      if (FILEHANDLE_LIMIT) filehandle_limit_check();
      boolean append = key2file.containsKey(key) ? true : false;
      // if we have ever written to the file, append instead of overwriting
      // DOUBLE-CHECK whether this works in compress mode!!
      //      System.err.println("opening " + key + " append = " + append);  // debug

      WorkingFile wf = new WorkingFile(get_filename(key), append);
      try {
	key2wf.put(key, wf);
	key2ps.put(key, ps = wf.getPrintStream());
	key2file.put(key, wf.get_file());
	// WorkingFile references may be removed if we hit open filehandle limit.
	// key2file tracks all filenames EVER opened.
	if (FILEHANDLE_LIMIT) key2count.put(key, Integer.valueOf(0));
      } catch (Exception e) {
	System.err.println("ERROR: can't get PrintStream for key " + key);  // debug
	System.err.println("exception=" + e);  // debug
	e.printStackTrace();
	System.exit(1);
      }
    }
    return ps;
  }

  public void set_bucket_size (int size) {
    BUCKET_SIZE = size;
  }

  public void set_max_open_filehandles (int size) {
    MAX_OPEN_FILEHANDLES = size;
    // actually limit is this + 1
    FILEHANDLE_LIMIT = true;
    if (compress) {
      System.err.println("ERROR: can't use max open filehandle limiter in compressed mode!");  // debug
      // actually this produces .gz compliant files (appended streams)
      // but JAVA doesn't read the results properly!  i.e. if we open
      // a compressed bucket, write to it, and then later open it for
      // writing again, a second gzip stream will be appended to the
      // file.  While this is legal in gzip spec, and gzip -d parses
      // these files without incident, but when Java reads the file it
      // only parses the FIRST compressed stream.  (still true as of
      // 1.6.0_15).  Since getHashSet() will return incomplete data
      // in this mode, disallow.
      System.exit(1);
    }
  }

  public String get_filename (String key) {
    return basename + key + (compress ? ".gz" : "");
  }

  public void write (String key, String value) {
    getPrintStream(key).println(value);
    if (FILEHANDLE_LIMIT) {
      // track usage by filehandle
      key2count.put(key, key2count.get(key) + 1);
    }
  }

  private void filehandle_limit_check () {
    if (key2wf.size() > MAX_OPEN_FILEHANDLES) {
      int closes_needed = key2wf.size() - MAX_OPEN_FILEHANDLES;
      ArrayList<String> delete_queue = new ArrayList<String>();
      while (closes_needed > 0) {
	HashSet<Integer> uc = new HashSet<Integer>(key2count.values());
	ArrayList<Integer> counts = new ArrayList<Integer>(uc);
	Collections.sort(counts);
	int least = counts.get(0);
	if (VERBOSE) {
	  System.err.println("counts="+counts);  // debug
	  System.err.println("least="+least);  // debug
	}
	for (String key : key2count.keySet()) {
	  int count = key2count.get(key);
	  if (count <= least) {
	    // deletable
	    delete_queue.add(key);
	    closes_needed--;
	    if (closes_needed == 0) break;
	  }
	}
      }

      for (String key : delete_queue) {
	if (VERBOSE) System.err.println("limit exceeded, closing " + key);  // debug
	key2wf.get(key).finish();
	key2wf.remove(key);
	key2ps.remove(key);
	key2count.remove(key);
      }

      if (VERBOSE) System.err.println("queue="+delete_queue);  // debug

    }
  }

  public int get_bucket_number (int pos) {
    return pos / BUCKET_SIZE;
  }

  public String get_bucketed_key (String key_raw, int pos) {
    return key_raw + "." + Integer.toString(get_bucket_number(pos));
  }

  public void write_bucketed (String key_raw, int pos, String value) {
    write(get_bucketed_key(key_raw, pos), value);
  }

  public HashSet<String> getBucketedHashSet(String key, int pos) throws IOException {
    return getHashSet(get_bucketed_key(key,pos));
  }

  public HashSet<String> getHashSet(String key) throws IOException {
    HashSet hs = new HashSet<String>();
    try {
      File f = new File(get_filename(key));
      InputStream is = new FileInputStream(f);
      if (compress) is = new GZIPInputStream(is);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line;
      while (true) {
	line = br.readLine();
	if (line == null) break;
	hs.add(line);
      }
      is.close();
    } catch (FileNotFoundException e) {
      //      System.err.println("getHashSet(): no file for " + key + "!");  // debug
    }
    return hs;
  }
  
  public void finish() {
    for (WorkingFile wf : key2wf.values()) {
      wf.finish();
    }
  }

  public void delete() {
    for (File f : key2file.values()) {
      if (!f.delete()) {
	System.err.println("ERROR: can't delete " + f);  // debug
      }
    }
  }

  public static void main (String[] argv) {
    MultiplexedWriter mw = new MultiplexedWriter("mw_test", false);
    mw.set_max_open_filehandles(2);
    mw.write("1", "1_1");
    mw.write("1", "1.2");
    mw.write("2", "2_1");
    mw.write("2", "2.2");
    mw.write("3", "3_1");
    mw.write("3", "3.2");
    mw.write("3", "3.3");
    mw.write("4", "4_1");
    mw.write("2", "2 re-open");
    mw.finish();
    
    try {
      HashSet<String> set = mw.getHashSet("2");
      System.err.println("set for 2 = " + set);  // debug
    } catch (Exception e) {
      System.err.println("ERROR: " +e);  // debug
    }

    //    System.err.println("deleting");  // debug
    //    mw.delete();


    
  }

}
