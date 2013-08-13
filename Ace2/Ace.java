//
//  Import .ace sequence assembly files (output of "phrap" program).
//  Michael Edmonson (edmonson@nih.gov) 1997-
//

// TO DO:
// - build_summary method:
//   given current member information, built summary info;
//   "leftmost", etc.

package Ace2;
import java.io.*;
import java.util.*;
import Funk.Notifier;

public class Ace extends Notifier implements Runnable {
  // an Ace file consists of a collection of contigs and their
  // member sequences.
  String filename;  // source file
  public Contig current_contig;

  Hashtable contigs;  // contigs
  String [] member_id_list;
  private boolean loaded, status;

  private Notifier notifier = new Notifier();

  private int bytes_loaded;
  // how many bytes of .ace file should we read before notifying observers?

  private boolean is_empty = false;
  private DataInputStream input_stream=null;

  public Ace (String filename) {
    // load synchronously
    this.filename = filename;
    load();
  }

  public Ace (String filename, Observer o) {
    // load asynchronously
    this.filename = filename;
    addObserver(o);
    Thread thread = new Thread(this);
    //    thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }

  public Ace (DataInputStream input_stream, Observer o) {
    // load asynchronously
    this.input_stream = input_stream;
    addObserver(o);
    Thread thread = new Thread(this);
    //    thread.setPriority(Thread.MIN_PRIORITY);
    thread.start();
  }

  public Ace (DataInputStream input_stream) {
    // synchronous
    this.input_stream = input_stream;
    load();
  }


  public boolean is_empty () {
    // if .ace file contains no assembly (newer .ace format can specify this)
    return is_empty;
  }

  public boolean loaded () {
    return loaded;
  }

  public boolean error () {
    return !status;
  }

  public int count () {
    // count of seqs in current contig
    return member_id_list.length;
  }
  
  public int bytes_loaded () {
    return bytes_loaded;
  }

  public void run () {
    load();
    setChanged();
    notifyObservers(new Boolean(status));
  }

  public void load () {
    loaded = status = false;
    contigs = new Hashtable();
    try {
      //      DataInputStream d = input_stream == null ? Trace.StreamDelegator.getStream(filename, 3) : input_stream;
      DataInputStream d = input_stream;
      long before = System.currentTimeMillis();
      parse(new BufferedReader(new InputStreamReader(d)));
      long after = System.currentTimeMillis();
      System.err.println("took:" + (after - before) + " ms to load/parse");
    } catch (IOException e) {
      System.out.println("load: " + e);  // debug
    }
    loaded = true;
  }

  Enumeration get_contig_list() {
    // to do: sort!
    return contigs.keys();
  }
  
  String get_contig_id () {
    return (current_contig == null) ? "null" : current_contig.name;
  }

  int get_contig_length () {
    return (current_contig == null) ? 0 : current_contig.consensus.sequence.length();
  }

  private void parse_new (BufferedReader d) throws java.io.IOException {
    StringTokenizer st;
    String line, id, key;
    AceSequence seq = null;

    while (true) {
      line = d.readLine();
      if (line == null) break;
      // EOF

      if (line.length() > 0) {
	st = new StringTokenizer(line);
	key = st.nextToken();
	if (key.equals("CO")) {
	  id = st.nextToken();
	  int len = Integer.parseInt(st.nextToken());
	  int max = len + (len / 50);
	  Contig c = new Contig(id);
	  contigs.put(id, c);
	  current_contig = c;
	  grab_sequence(d, c.consensus);
	} else if (key.equals("AF")) {
	  // assembled from
	  seq = current_contig.create_member(st.nextToken());
	  seq.complemented = st.nextToken().equals("C");
	  seq.asm_start_padded = Integer.parseInt(st.nextToken());
	  //	  System.out.println("id:" + seq.name + " comp:" + seq.complemented + " as:" + seq.asm_start_padded);  // debug
	} else if (key.equals("RD")) {
	  // RD <read name> <# of padded bases> <# of whole read info items> <# of read tags>
	  seq = current_contig.get_member(st.nextToken());
	  int len = Integer.parseInt(st.nextToken());
	  int max = len + (len / 50);
	  grab_sequence(d, seq);
	  //	  seq.asm_end_padded = seq.asm_start_padded + seq.sequence.length();
	  seq.asm_end_padded = seq.asm_start_padded + seq.sequence.length() - 1;
	  //	  System.out.println("id:" + seq.name + " comp:" + seq.complemented + " as:" + seq.asm_start_padded + " aep:" + seq.asm_end_padded);  // debug

	} else if (key.equals("QA")) {
	  // sequence/alignment quality/clipping
	  st.nextToken();
	  st.nextToken();
	  seq.clip_start_padded = Integer.parseInt(st.nextToken());
	  seq.clip_end_padded = Integer.parseInt(st.nextToken());
	  if (seq.clip_start_padded == -1 && seq.clip_end_padded == -1) {
	    // entire sequence is low quality; ignore clipping
	    seq.clip_start_padded = 1;
	    seq.clip_end_padded = seq.sequence.length();
	  }
	  //	  System.out.println(seq.name + " cs:" + seq.clip_start_padded + " ce:" + seq.clip_end_padded);  // debug
	} else if (key.equals("DS")) {
	  // sequence description
	  seq.description = line.substring(2);
	  //	  System.out.println(seq.name + " desc:" + seq.description);
	} else if (key.equals("BQ")) {
	  // consensus base quality
	  StringBuffer sb = grab_until_whitespace(d);
	  StringTokenizer st2 = new StringTokenizer(sb.toString());
	  while (st2.hasMoreTokens()) {
	    current_contig.quality.addElement(st2.nextToken());
	  }
	} else if (key.equals("BS")) {
	  // ignored for now
	  StringBuffer sb = grab_until_whitespace(d);
	} else if (key.substring(0,2).equals("RT") ||
		   key.substring(0,2).equals("WA") ||
		   key.substring(0,2).equals("WR")
		    ) {
	  // ignored "comment" tags
	  while (true) {
	    line = d.readLine();
	    if (line == null || line.substring(0, 1).equals("}")) break;
	    //	    System.out.println("ignoring:" + line);  // debug
	  }
	} else {
	  System.out.println("unhandled acefile tag: " + key + " line:" +line);  // debug
	  System.exit(1);
	}
      }
    }
  }

  private void parse (BufferedReader d) throws java.io.IOException {
    System.err.println("loading .ace file");
    boolean contig_seq_mode = false;
    boolean seq_mode = false;
    boolean ignore_mode = false;
    boolean bq_mode = false;

    String line;
    AceSequence current_seq = null;
    AceSequence s;
    StringTokenizer st;
    String key, id;
    getID g;
    //      while (d.available() > 0) {
    bytes_loaded = 0;
    boolean first = true;
    while (true) {
      line = d.readLine();
      if (line == null) break;
      // EOF

      if (first) {
	if (line.length() >= 2 && line.substring(0,2).equals("AS")) {
	  // newer versions of phrap introduce a new file format
	  StringTokenizer nst = new StringTokenizer(line);
	  nst.nextToken();
	  if (nst.nextToken().equals("0") && nst.nextToken().equals("0"))
	    is_empty = true;
	  // no contigs, no sequences; sequences couldn't be assembled
	  // (eg M31642.fasta.screen.ace)
	  parse_new(d);
	  break;
	}
	first = false;
      }
      
      int line_len = line.length();
      bytes_loaded += line_len;
      notify_check(bytes_loaded);
      if (line_len == 0) {
	// separator; end of block
	//	System.out.println("separator");  // debug
	seq_mode = false;
	contig_seq_mode = false;
	ignore_mode = false;
	bq_mode = false;
      }	else if (ignore_mode) {
	// not doing anything with this section
      } else if (bq_mode) {
	// contig base quality
	StringTokenizer st2 = new StringTokenizer(line);
	while (st2.hasMoreTokens()) {
	  current_contig.quality.addElement(st2.nextToken());
	}
      } else {
	st = new StringTokenizer(line);
	key = st.nextToken();
	//	System.out.println("key: " + key);  // debug
	if (contig_seq_mode) {
	  // assembly alignment info
	  if (key.equals("Assembled_from")) {
	    // need to create this id
	    g = new getID(st.nextToken());
	    id = g.id;
	    s = current_contig.create_member(id);
	    // s.asm_start = Integer.parseInt(st.nextToken());
	    // s.asm_end = Integer.parseInt(st.nextToken());
	  } else if (key.equals("Assembled_from*")) {
	    g = new getID(st.nextToken());
	    id = g.id;
	    s = get_member(id);
	    s.asm_start_padded = Integer.parseInt(st.nextToken());
	    s.asm_end_padded = Integer.parseInt(st.nextToken());
	  } else {
	    //	      System.out.println("unhandled key " + key);
	  }
	} else if (seq_mode) {
	  // read in sequence-specific alignment info
	  //	    System.out.println("sequence info:" + line);
	  if (key.equals("Clipping")) {
	    //	    current_seq.clip_start = Integer.parseInt(st.nextToken());
	    //	    current_seq.clip_end = Integer.parseInt(st.nextToken());
	  } else if (key.equals("Clipping*")) {
	    current_seq.clip_start_padded = Integer.parseInt(st.nextToken());
	    current_seq.clip_end_padded = Integer.parseInt(st.nextToken());
	  } else if (key.equals("Description")) {
	    current_seq.description = line.substring(12);
	  } else {
	    System.out.println("unknown key " + key);
	  }
	} else if (key.equals("DNA")) {
	  g = new getID(st.nextToken());
	  id = g.id;
	  if (g.contig) {
	    // a new contig ID
	    Contig c = new Contig(id);
	    contigs.put(id, c);
	    current_contig = c;
	    current_seq = c.consensus;
	    //	      System.out.println("new contig:" + id);
	  } else {
	    // a new contig member ID
	    //	      System.out.println("new sequence:" + id);
	    current_seq = get_member(id);
	  }
	  current_seq.complemented = g.complemented;
	  grab_sequence(d, current_seq);
	} else if (key.equals("Sequence")) {
	  g = new getID(st.nextToken());
	  id = g.id;
	  if (g.contig) {
	    contig_seq_mode = true;
	    current_seq = current_contig.consensus;
	  } else {
	    seq_mode = true;
	    current_seq = get_member(id);
	  }
	} else if (key.equals("BaseQuality")) {
	  bq_mode = true;
	} else {
	  System.out.println("ace: unknown block " + key + ", line=" + line);
	}
      }
    }

    Enumeration e = contigs.elements();
    while (e.hasMoreElements()) {
      Contig c = (Contig) e.nextElement();
      c.find_bounds();

      if (true) {
	// sanity check: make sure we have quality values for contigs
	//	System.err.println(c.name + ": " + c.consensus.sequence.length() + " " + c.quality.size());  // debug
      }
    }
    status = true;
    //    System.err.println("done");
  }
  
  public void set_snps (SNPList snps) {
    current_contig.snps = snps;
  }

  public SNPList get_snps () {
    // return SNP scores for the current contig
    return current_contig.snps;
  }

  public void set_current_contig (String id) {
    current_contig = get_contig(id);
    // fix me -- error checking!

    // build list of keys for this contig;
    // prevent having to repeatedly use Enumerations from keys()
    Enumeration e = current_contig.members.keys();
    int count = 0;
    while (e.hasMoreElements()) {
      count++;
      e.nextElement();
    }

    member_id_list = new String[count];
    e = current_contig.members.keys();
    count = 0;
    while (e.hasMoreElements()) {
      member_id_list[count++] = (String) e.nextElement();
    }
  }

  Contig get_contig (String id) {
    return((Contig) contigs.get(id));
  }

  public AceSequence get_member(String id) {
    return(current_contig.get_member(id));
  }

  public String get_biggest_contig () {
    // return ID of the contig with the most members
    String id = null;
    int biggest = 0;
    Enumeration e = contigs.elements();
    while (e.hasMoreElements()) {
      Contig c = (Contig) e.nextElement();
      if (c.get_size() > biggest) {
	id = c.name;
	biggest = c.get_size();
      }
    }
    return id;
  }

  AceSequence consensus () {
    return(current_contig.consensus);
  }

  public String [] member_id_list () {
    return member_id_list;
  }

  public String consensus_sequence () {
    // return consensus sequence for current contig
    return(current_contig.consensus.sequence.toString());
  }

  public char consensus_nucleotide (int offset) {
    // note that offset is in CONSENSUS space (i.e. starts at 1) 
    // rather than string offset (starts at 0)
    return current_contig.consensus.sequence.charAt(offset - 1);
  }

  private StringBuffer grab_until_whitespace (BufferedReader d) throws java.io.IOException {
    StringBuffer sb = new StringBuffer();
    String line;
    while (true) {
      line = d.readLine();
      if (line == null || line.length() == 0) break;
      sb.append(line);
    }
    return sb;
  }

  private void grab_sequence (BufferedReader d, AceSequence as) throws java.io.IOException {
    StringBuffer sb = new StringBuffer();
    String line;
    while (true) {
      line = d.readLine();
      if (line == null || line.length() == 0) break;
      sb.append(line);
    }
    as.sequence = sb.toString();
  }

}

class getID {
  // parses out identifier from given string.
  // - If ".comp" tag if present, remove it and set "complemented" flag.
  // - If ID begins with "Contig", set "contig" flag to true.
  String id;
  boolean complemented;
  boolean contig;
  // a Contig ID

  getID (String s) {
    // directly given the ID string, process it
    this.id = s;

    // determine if the ID is complemented and/or a contig ID
    int pos = s.length() - 5;
    if (pos >= 0 && s.substring(pos).equals(".comp")) {
      this.id = s.substring(0, pos);
      complemented = true;
    } else {
      complemented = false;
    }
    contig = (s.length() > 6 && s.substring(0,6).equals("Contig")) ? true : false;
  }
}

