package Trace;

import java.io.*;
import java.net.*;
import java.util.*;

// to do:
//
// - split up different formats into different subclasses
//   which fill in raw data fields.  Maybe a TraceLoader interface?

public class TraceFile extends Observable implements Runnable, Observer {
  public static final short TRACE_A = 0;
  public static final short TRACE_C = 1;
  public static final short TRACE_G = 2;
  public static final short TRACE_T = 3;
  public static final short TRACE_UNDEF = -1;

  public static boolean NORMALIZE = false;
  // whether to normalize the peak data after loading;
  // for rendering, probably better to use TraceDataView

  private static double DUBIOUS_BASECALL_CORRUPTION_THRESHOLD = 0.33;

  public static final short UNINITIALIZED = -1;
  public static final short LOADING = 0;
  public static final short LOADED = 1;
  public static final short NO_DATA = 2;
  public static final short UNKNOWN_FORMAT = 3;
  
  /* public variables; FIX ME */
  public String name;
  public int format, num_bases, num_samples, average_peak_spacing;
  public int max_amplitude = 0;
  public int trace_data[][] = new int[4][];
  public int [] base_position;
  public char [] bases;
  public boolean reverse_complemented = false;
  public boolean flipped = false;

  /* private */
  private static final int ABI_NUM_INDICES = 14;
  private static final int ABI_TRACE1 = 0;  // trace data
  private static final int ABI_TRACE2 = 1;  // trace data
  private static final int ABI_TRACE3 = 2;  // trace data
  private static final int ABI_TRACE4 = 3;  // trace data
  private static final int ABI_BASMAP = 4;
  // map between sample number and base type
  private static final int ABI_BASES = 5;   // base data
  private static final int ABI_BASPOS = 6;  // base positions
  private static final int ABI_SIGSTR = 7;  // signal strength
  private static final int ABI_AVGSPC = 8;  // average peak spacing
  private static final int ABI_PRIPOS = 9;  // primer position
  private static final int ABI_MCHNAM = 10; // machine name
  private static final int ABI_DYEPRI = 11; // 
  private static final int ABI_SMPNAM = 12; // sample name
  private static final int ABI_THMPRT = 13; // thumbprint
  private static final int [] ABI_HEADER_SERIAL = {9, 10, 11, 12, 1,  1,  1,  1, 1,  1,  1,  1, 1,  1 };
  private static final String [] ABI_HEADER_LABELS = { "DATA", "DATA", "DATA", "DATA",
	     "FWO_", "PBAS", "PLOC", "S/N%",
	     "SPAC", "PPOS", "MCHN", "PDMF",
	     "SMPL", "THUM" };

  private short status = UNINITIALIZED;
  private boolean load_from_dis = false;
  private DataInputStream passed_dis = null;

  private boolean internal_basecalls_corrupt = false;

  public TraceFile (String name) {
    // simplest case -- load the given file, synchronously
    load(name);
  }

  public TraceFile (String name, DataInputStream dis) {
    // load synchronously from passed stream
    load_stream(name, dis);
  }

  public TraceFile (String name, DataInputStream dis, Observer o) {
    // load asynchronously from passed-in stream
    load_from_dis = true;
    passed_dis = dis;
    load_async(name, o);
  }

  public TraceFile (String name, Observer o) {
    // load the file, asynchronously
    load_async(name, o);
  }

  public TraceFile (String name, boolean rc, Observer o) {
    // load the file, asynchronously, w/reverse-comp info
    reverse_complemented = rc;
    load_async(name, o);
  }

  public static char index_to_base (short s) {
    // convert base index (0,1,2,3) to base (A,C,G,T)
    switch (s) {
    case TRACE_A: return('A');
    case TRACE_C: return('C');
    case TRACE_G: return('G');
    case TRACE_T: return('T');
    default:
      return('?');
    }
  }

  public static short base_to_index (char c) {
    // convert base char (a,c,g,t) to base index (0,1,2,3)
    switch (c) {
    case 'a' : case 'A': return TRACE_A;
    case 'c' : case 'C': return TRACE_C;
    case 'g' : case 'G': return TRACE_G;
    case 't' : case 'T': return TRACE_T;
    default: return TRACE_UNDEF;
    }
  }

  public void update (Observable o, Object arg) {
    // observer: receives reports of trace loading progress
    // from FakeFile as it reads stream
    //    System.out.println("trace loaded:" + arg);
    setChanged();
    notifyObservers(arg);
  }

  void load_async ( String name, Observer o) {
    // load tracefile in the background, notify when done
    this.name = name;
    addObserver(o);
    //    System.err.println("STARTING NEW THREAD for " + name);  // debug
    //    new Exception().printStackTrace();
    new Thread(this).start();
  }

  public int status () {
    // return status of loading
    return status;
  }

  public boolean loaded () {
    // has the data successfully loaded?
    return (status == LOADED ? true : false);
  }

  public boolean error () {
    // was an error encounted during loading?
    return (status == NO_DATA || status == UNKNOWN_FORMAT ? true : false);
  }

  public int get_value (short index, int position) {
    // return the value of the specified base at the specified position
    int value = 0;
    if (index < 0 || index > 3) {
      System.out.println("get_value: bad base " + index + "!");
    } else {
      // to do: error checking!
      value = trace_data[index][position];
      //      System.out.println("index:" + index + " pos:" + position + " value:" + value);  // debug
    }
    return(value);
  }

  public int get_value (char base, int position) {
    switch (base) {
    case 'a' : case 'A' : return(get_value(TRACE_A, position));
    case 'c' : case 'C' : return(get_value(TRACE_C, position));
    case 'g' : case 'G' : return(get_value(TRACE_G, position));
    case 't' : case 'T' : return(get_value(TRACE_T, position));
    default: return(-1);
    }
  }

  public void run () {
    // asychronous loading version
    // System.err.println("run(): name=" + name + " load_from_dis=" + load_from_dis + " dis=" + passed_dis);  // debug
    if (load_from_dis) {
      if (passed_dis == null) {
        System.err.println("WTF, null stream!");  // debug
        System.exit(1);
      } else {
        load_stream(name, passed_dis);
      }
    } else {
      load(name);
    }
    setChanged();
    notifyObservers();
  }

  public void load ( String name ) {
    // load trace data into memory
    this.name = name;
    try {
      load_stream(name, StreamDelegator.getStream(name, 0));
    } catch (IOException e) {
      status = NO_DATA;
    }
  }

  public void load_stream (String name, DataInputStream d) {
    // load trace data into memory
    status = LOADING;
    this.name = name;

    if (d == null) {
      System.err.println("WTF: null trace stream!");  // debug
      status = NO_DATA;
    } else {
      //      System.err.println("loading trace stream for " + name);
      RandomAccessStream rs = new FakeFile(d, this);
      setup_stream(rs);
    }
  }

  private void setup_stream (RandomAccessStream rs) {
    if (rs.length() > 0) {
      // data available
      format = get_format(rs);
      if (format == 1) {
        load_scf(rs);
      } else if (format == 2) {
        load_abi(rs);
      } else {
        // unimplemented format
        status = UNKNOWN_FORMAT;
        System.out.println("format " + format + " not implemented");
        return;
      }

      if (true) {
        corruption_check();
      } else {
        System.err.println("DEBUG: corruption check disabled");  // debug
      }

      if (NORMALIZE) {
        normalize_peaks();
      } else {
        //        System.err.println("debug: normalization disabled");  // debug
      }
      if (reverse_complemented) reverse_complement();
      // reverse-complement on startup was requested
      status = LOADED;
    } else {
      // no data
      status = NO_DATA;
    }
  }

  int get_format (RandomAccessStream rs) {
    /* identify tracefile format */
    int result = 0;
    String SCF_magic = ".scf";
    String ABI_magic = "ABIF";

    rs.seek(0);
    String s = rs.readString(4);
    //    System.out.println("magic:" + s);
    if (s.equals(SCF_magic)) {
      /* .scf format */
      result = 1;
    } else if (s.equals(ABI_magic)) {
      /* ABI format */
      result = 2;
    } else {
      rs.seek(128);
      s = rs.readString(4);
      //      System.out.println("magic:" + s);
      if (s.equals(ABI_magic)) result = 2;
    }
    return(result);
  }

  void load_scf (RandomAccessStream rs) {
    /* read .scf format trace data */
    /* data is in big-endian order, and so is Java (joy...) */

    /* read header */
    /* TO DO: should this really be readInt()?  Signed?? */
    //    long start = System.currentTimeMillis();
    rs.seek(4);
    num_samples = rs.readInt();
    int samples_offset = rs.readInt();
    num_bases = rs.readInt();
    int bases_left_clip = rs.readInt();  // left quality clipping?
    int bases_right_clip = rs.readInt();  // right quality clipping?
    int bases_offset = rs.readInt();
    int comments_size = rs.readInt();
    int comments_offset = rs.readInt();
    //    int version = rs.readInt();  // machine?
    String version = rs.readString(4);
    int sample_size = rs.readInt();  // bytes per trace sample, 1 or 2
    int code_set = rs.readInt(); // ??

    float vers;
    try {
      // in version 2.00 (and later?) version is 4 digits (float)
      vers = (Float.valueOf(version)).floatValue();
    } catch (NumberFormatException e) {
      // early versions seem to be binary zeros, so conversion throws an exception
      vers = 0;
    }
    //    System.err.println("SCF version: " + vers);  // debug

    if (vers < 2.0) sample_size = 1;
    if (vers > 3) {
      System.out.println("WARNING: possibly unsupported scf version " + vers + "!");
    }

    /* allocate arrays */
    trace_data[TRACE_A] = new int[num_samples];
    trace_data[TRACE_C] = new int[num_samples];
    trace_data[TRACE_G] = new int[num_samples];
    trace_data[TRACE_T] = new int[num_samples];
    base_position = new int[num_bases];
    bases = new char[num_bases];

    /* read trace peak data */
    rs.seek(samples_offset);
    int base,i,deref;

    if (vers >= 3) {
      // new format
      for (base=0; base < 4; base++) {
	// 1. get raw values...
	for (i=0; i < num_samples; i++) {
	  if (sample_size == 1) {
	    // I know this is slower than duplicate code but I can't bear it...
	    trace_data[base][i] = rs.readUnsignedByte();
	  } else if (sample_size == 2) {
	    trace_data[base][i] = rs.readShort();
	  } else {
	    System.out.println("unknown ss " + sample_size);  // debug
	    System.exit(1);
	  }
	}

	// 2. unpack delta format
	int p_value;
	for (deref = 0; deref < sample_size; deref++) {
	  p_value = 0;
	  for (i = 0; i < num_samples; i++) {
	    trace_data[base][i] = trace_data[base][i] + p_value;
	    p_value = trace_data[base][i];
	  }
	}
	if (sample_size != 2) {
	  System.out.println("warning: untested delta w/this ss!");
	}

	for (i=0; i < num_samples; i++) {
	  if (trace_data[base][i] > max_amplitude)
	    max_amplitude = trace_data[base][i];
	}
      }
    } else {
      // olde format
      if (sample_size == 1) {
	for (i=0; i < num_samples; i++) {
	  trace_data[TRACE_A][i] = base = rs.readUnsignedByte();
	  if (base > max_amplitude) max_amplitude = base;
	  trace_data[TRACE_C][i] = base = rs.readUnsignedByte();
	  if (base > max_amplitude) max_amplitude = base;
	  trace_data[TRACE_G][i] = base = rs.readUnsignedByte();
	  if (base > max_amplitude) max_amplitude = base;
	  trace_data[TRACE_T][i] = base = rs.readUnsignedByte();
	  if (base > max_amplitude) max_amplitude = base;
	}
      } else if (sample_size == 2) {
	for (i=0; i < num_samples; i++) {
	  trace_data[TRACE_A][i] = base = rs.readShort();
	  if (base > max_amplitude) max_amplitude = base;
	  trace_data[TRACE_C][i] = base = rs.readShort();
	  if (base > max_amplitude) max_amplitude = base;
	  trace_data[TRACE_G][i] = base = rs.readShort();
	  if (base > max_amplitude) max_amplitude = base;
	  trace_data[TRACE_T][i] = base = rs.readShort();
	  if (base > max_amplitude) max_amplitude = base;
	}
      } else {
	System.out.println("unimplemented sample size " + sample_size + "!");
      }
    }

    /* read bases and positions */
    rs.seek(bases_offset);

    if (vers == 3) {
      for (i=0; i < num_bases; i++) {
	// base positions
	base_position[i] = rs.readInt();
      }

      for (base=0; base < 4; base++) {
	// arrays of 1-byte base probabilities; ignore
	for(i=0; i < num_bases; i++) {
	  rs.readUnsignedByte();
	}
      }

      // basecalls
      for (i=0; i < num_bases; i++) {
	bases[i] = (char) rs.readByte();
      }
    } else {
      for (i=0; i < num_bases; i++) {
	base_position[i] = rs.readInt();
	rs.skipBytes(4);
	/* call probability info; not currently used */
	bases[i] = (char) rs.readByte();
	rs.skipBytes(3);
	/* 3 spare bytes */
      }
    }

    // calculate avg peak spacing
    int total = 0;
    for (i=1; i < num_bases; i++) {
      total += base_position[i] - base_position[i-1];
    }
    this.average_peak_spacing = total / (num_bases - 1);
    // - 1 because for X bases, total only reflects X - 1 spacings

    //      System.out.println("scf load:" + (System.currentTimeMillis() - start));  // debug
  }

  void load_abi (RandomAccessStream rs) {
    //    System.out.println("loading ABI");

    /*
    String [] header_labels = { "DATA", "DATA", "DATA", "DATA",
	     "FWO_", "PBAS", "PLOC", "S/N%",
	     "SPAC", "PPOS", "MCHN", "PDMF",
	     "SMPL", "THUM" };
	     */

    // initialize index
    abi_header [] abi_index = new abi_header[ABI_NUM_INDICES];
    for (int i=0; i < ABI_NUM_INDICES; i++) {
      abi_index[i] = new abi_header();
      // wtf?  why is new() necessary twice?
      abi_index[i].offset = -1;
      abi_index[i].occur = false;
      abi_index[i].num_bytes = 0;
      abi_index[i].num_words = 0;
      abi_index[i].label = ABI_HEADER_LABELS[i];
      abi_index[i].serial_number = ABI_HEADER_SERIAL[i];
      //      System.out.println(i + " " + abi_index[i].label);
    }

    // find data start position
    int start_offset = -1;
    rs.seek(0);
    if (rs.readString(4).equals("ABIF")) {
      start_offset = 0;
    } else {
      rs.seek(128);
      if (rs.readString(4).equals("ABIF")) start_offset = 128;
    }
    //    System.out.println("start at:" + start_offset);

    rs.seek(start_offset + 16);
    int block_size = rs.readShort();
    // get block size
    //    System.out.println("blocksize:" + block_size);

    rs.seek(start_offset + 26);
    int first_block_offset = rs.readInt() + start_offset;
    rs.seek(first_block_offset);
    String label;
    int serial_number, i;
    byte [] buf = new byte[block_size];
    
    //
    // load the index information
    //
    int max_num_bytes = 0;
    while(rs.read(buf) == block_size) {
      //      RandomAccessStream rs2 = new RandomAccessStream(buf);
      RandomAccessStream rs2 = new FakeFile(buf);
      label = rs2.readString(4);
      serial_number = rs2.readInt();
      for (i=0; i < ABI_NUM_INDICES; i++) {
	if (abi_index[i].label.equals(label) &&
	    abi_index[i].serial_number == serial_number) {
	  abi_index[i].occur = true;
	  short data_type = rs2.readShort();
	  short word_size = rs2.readShort();
	  int num_words = abi_index[i].num_words = rs2.readInt();
	  int num_bytes = abi_index[i].num_bytes = rs2.readInt();
	  if (num_bytes > max_num_bytes) max_num_bytes = num_bytes;
	  //	  System.out.println("type:" + data_type + " numw:" + num_words + " ws:" + word_size + " nb:" + num_bytes);
	  if (num_bytes <= 4 && data_type != 18) {
	    // actual data is stored here
	    if (word_size == 1) {
	      // a string
	      abi_index[i].data = rs2.readString(num_words);
	    } else if (word_size == 2) {
	      // a short
	      abi_index[i].data = String.valueOf(rs2.readShort());
	    } else if (word_size == 4) {
	      // a long 
	      abi_index[i].data = String.valueOf(rs2.readInt());
	    } else {
	      System.out.println("unsupported word size " + word_size);
	    }
	  } else {
	    // pointer to the data is stored here
	    abi_index[i].offset = start_offset + rs2.readInt();
	  }
	}
      }
    }

    //
    // read trace data
    // 
    max_amplitude = 0;
    char [] base_map = abi_index[ABI_BASMAP].data.toCharArray();
    for (int trace = ABI_TRACE1; trace <= ABI_TRACE4; trace++) {
      if (abi_index[trace].occur) {
	rs.seek(abi_index[trace].offset);
	int words = abi_index[trace].num_words;
	num_samples = words;
	int sample;
	int [] samples = new int[words];

        //        System.err.println("samples for base type " + base_map[trace] + ": " + num_samples + " => " +samples);  // debug

	for (i=0; i < words; i++) {
	  sample = samples[i] = rs.readShort();
          //          System.err.println("  " + sample);  // debug
	  if (sample > max_amplitude) max_amplitude = sample;
	}
	switch (base_map[trace]) {
	case 'A' :
	  trace_data[TRACE_A] = samples; break;
	case 'C' :
	  trace_data[TRACE_C] = samples; break;
	case 'G' :
	  trace_data[TRACE_G] = samples; break;
	case 'T' :
	  trace_data[TRACE_T] = samples; break;
	default:
	  System.out.println("argggh"); break;
	}
      } else {
	System.err.println("missing trace data; huh?");
      }
    }

    //
    // read bases
    //
    if (abi_index[ABI_BASES].occur) {
      rs.seek(abi_index[ABI_BASES].offset);
      String s = rs.readString(abi_index[ABI_BASES].num_bytes);
      num_bases = abi_index[ABI_BASES].num_words;
      //      System.out.println(s);
      bases = s.toCharArray();
    } else {
      System.out.println("No base information; huh?");
    }

    //
    // read base positions
    //
    if (abi_index[ABI_BASPOS].occur) {
      rs.seek(abi_index[ABI_BASPOS].offset);
      int words = abi_index[ABI_BASPOS].num_words;
      base_position = new int[words];
      int total = 0;
      for (i=0; i<words; i++) {
	base_position[i] = rs.readShort();
	if (i > 0) total += base_position[i] - base_position[i - 1];
      }
      average_peak_spacing = total / (num_bases - 1);
    } else {
      System.out.println("No base information; huh?");
    }
    
  }

  public void reverse_complement () {
    // reverse-complement the trace data.

    flipped = !flipped;
    trace_data[TRACE_A] = reverse(trace_data[TRACE_A]);
    trace_data[TRACE_C] = reverse(trace_data[TRACE_C]);
    trace_data[TRACE_G] = reverse(trace_data[TRACE_G]);
    trace_data[TRACE_T] = reverse(trace_data[TRACE_T]);
    // This sucks, but I can't bring myself to cut-and-paste code just because
    // Java won't let me can't pass a reference to an array.
    // My kingdom for a pointer!

    // complement the data by exchanging the A/T and C/G arrays.
    int [] temp;
    temp = trace_data[TRACE_A];
    trace_data[TRACE_A] = trace_data[TRACE_T];
    trace_data[TRACE_T] = temp;
    temp = trace_data[TRACE_C];
    trace_data[TRACE_C] = trace_data[TRACE_G];
    trace_data[TRACE_G] = temp;

    // flip orientation of the base positions
    int i,j;
    base_position = reverse(base_position);
    // hack

    for (i=0; i < base_position.length; i++) {
      base_position[i] = num_samples - 1 - base_position[i];
    }

    // reverse-complement base calls
    bases = reverse(bases);
    // hack
    for (i=0; i < num_bases; i++) {
      switch (bases[i]) {
      case 'a': bases[i] = 't'; break;
      case 'A': bases[i] = 'T'; break;
      case 'c': bases[i] = 'g'; break;
      case 'C': bases[i] = 'G'; break;
      case 'g': bases[i] = 'c'; break;
      case 'G': bases[i] = 'C'; break;
      case 't': bases[i] = 'a'; break;
      case 'T': bases[i] = 'A'; break;

      case 'R': bases[i] = 'Y'; break;
      case 'r': bases[i] = 'y'; break;
      case 'Y': bases[i] = 'R'; break;
      case 'y': bases[i] = 'r'; break;

      case 'M': bases[i] = 'K'; break;
      case 'm': bases[i] = 'k'; break;
      case 'K': bases[i] = 'M'; break;
      case 'k': bases[i] = 'm'; break;

      case 'B': bases[i] = 'V'; break;
      case 'b': bases[i] = 'v'; break;
      case 'V': bases[i] = 'B'; break;
      case 'v': bases[i] = 'b'; break;

      case 'D': bases[i] = 'H'; break;
      case 'd': bases[i] = 'h'; break;
      case 'H': bases[i] = 'd'; break;
      case 'h': bases[i] = 'd'; break;

      case '-': case 'N' : case 's' : case 'S' : case 'w' : case 'W' : break; // nothing needed

      default: 
	System.out.println("reverse_complement: don't know how to handle base " + bases[i]);
      }
    }
  }

  private void normalize_peaks () {
    // find the tallest called peak for each base type, then amplify
    // weaker types so tallest peaks are the same height for each
    int base,amp,bp;
    short bi;
    int [] max_call = new int[4];
    max_call[0] = max_call[1] = max_call[2] = max_call[3] = 0;

    //    System.err.println("num_bases: " + num_bases);  // debug

    for(base=0; base < num_bases; base++) {
      //      System.err.println("base: " + bases[base]);  // debug
      bi = base_to_index(bases[base]);
      //      System.err.println("bi: " + bi);  // debug
      if (bi >= 0) {
        bp = base_position[base];
        //        System.err.println("bp: " + bp);
        //	amp = trace_data[bi][base_position[base]];
        if (bp >= 0) {
          amp = trace_data[bi][bp];
          if (amp > max_call[bi]) max_call[bi] = amp;
        } else {
          System.err.println("ERROR: illegal base position in " + name + ": " + bp);
        }
      }
    }

    int i,j;
    int max_i = 0;
    for (i=0; i < 4; i++) {
      if (max_call[i] > max_call[max_i]) max_i = i;
    }

//     System.out.println(max_call[0]);  // debug
//     System.out.println(max_call[1]);  // debug
//     System.out.println(max_call[2]);  // debug
//     System.out.println(max_call[3]);  // debug
//     System.out.println(max_i);  // debug

    double ratio;
    for (bi=0; bi < 4; bi++) {
      if (bi != max_i && max_call[bi] > 0) {
        // only normalize if there were calls for this base type,
        // otherwise samples will be reset to zero!
        // e.g. local TI 694719, an ABI file whose internal basecalls
        // consist of only 4 Ns.
	ratio = (double) max_call[max_i] / max_call[bi];
	//	System.out.println(bi +  " " + ratio);  // debug
	for (i=0; i < num_samples; i++) {
	  trace_data[bi][i] *= ratio;
	}
      }
    }
    
  }
  
  private void normalize_peaks_old () {
    // normalize the peak data
    // based on phred's "normalize_traces" from fit_sine.supp.c
    // 
    // alternative approach -- 2 pass normalization
    //   1. follow the degeneration of signal strength from start to
    //      finish for each base type, amplify later signal to 
    //      compensate
    //   2. phred-style base-to-base normalization
    int i,j,index;
    double ratio;
    int [] peak_counts = new int[4];
    int [] peak_sizes = new int[4];
    int [] trace_sums = new int[num_samples];
    int start = (int) (num_samples * 0.30);
    int end = (int) (num_samples * 0.70);
    
    for (i=start; i < end; i++) {
      trace_sums[i] = trace_data[TRACE_A][i] + trace_data[TRACE_C][i] +
	trace_data[TRACE_G][i] + trace_data[TRACE_T][i];
    }
    
    // find the peaks in the area, store counts and sizes
    // NOTE: this does NOT work for truncated peaks!
    for (i = start + 1; i < end - 1; i++) {
      if (trace_sums[i] > trace_sums[i-1] && trace_sums[i] > trace_sums[i+1]) {
	// peak here
	for (j=1, index=0; j < 4; j++) {
	  if (trace_data[j][i] > trace_data[index][i]) index = j;
	}
	peak_counts[index]++;
	peak_sizes[index] += trace_data[index][i];
      }
    }

    // get the average peak size for each base type
    for (i=0; i<4; i++) {
      if (peak_counts[i] > 0) peak_sizes[i] /= peak_counts[i];
    }

    // find the base w/smallest avg peak size
    for (index=0, i=0; i<4; i++) {
      if (peak_sizes[i] > 0 && peak_sizes[i] < peak_sizes[index]) index = i;
    }
    
    for (i=0; i<4; i++) {
      // normalize
      if (i != index && peak_counts[i] > 0) {
	ratio = (double) peak_sizes[index] / (double) peak_sizes[i];
        //	System.out.println("ratio for " + i + " is " + ratio);  // debug
	for (j = 0; j < num_samples; j++) {
	  trace_data[i][j] *= ratio;
	}
      }
    }
  }

  int [] reverse (int [] a) {
    // return reversed contents of an array.
    int i, j;
    int len = a.length;
    int [] result = new int[len];

    for (i=0, j = len - 1; i < len; i++, j--) {
      result[j] = a[i];
    }
    return(result);
  }

  char [] reverse (char [] a) {
    // return reversed contents of an array.
    int i, j;
    int len = a.length;
    char [] result = new char[len];

    for (i=0, j = len - 1; i < len; i++, j--) {
      result[j] = a[i];
    }
    return(result);
  }

  public static void main (String [] argv) {
    // debug
    Trace.StreamDelegator.set_local(true);
    TraceFile tf = new TraceFile("/fccc/chlcfs/chlc5/edmonson/src/java2/VirtualNorthern/1_f_only/chromat_dir/SHE1030-plt4_p53_4a_C07.f1");
  }

  private void corruption_check () {
    int base,bi,bp;
    internal_basecalls_corrupt = false;
    
    HashSet bases_seen = new HashSet();
    int bi_scan,v,max,max_i;
    int dubious_calls = 0;

    for(base=0; base < num_bases; base++) {
      //      System.err.println("base: " + bases[base]);  // debug

      bp = base_position[base];
      if (bp < 0) {
        internal_basecalls_corrupt = true;
        System.err.println("ERROR: illegal base position in " + name + ": " + bp);
      } else {
        bi = base_to_index(bases[base]);
      
        if (bi == TRACE_A || bi == TRACE_C || bi == TRACE_G || bi == TRACE_T) {
          bases_seen.add(new Integer(bi));

          max = max_i = 0;
          for (bi_scan = 0; bi_scan < 4; bi_scan++) {
            v = trace_data[bi_scan][bp];
            if (v > max) {
              max = v;
              max_i = bi_scan;
            }
          }
          if (bi != max_i) {
            dubious_calls++;
            //          System.err.println("base:" + base + " call:" + bi + " max_i:" + max_i);  // debug
          }

        }
      }

    }

    double dubious_ratio = (double) dubious_calls / num_bases;
    //    System.err.println("dubious ratio:" + dubious_ratio);  // debug

    if (dubious_ratio > DUBIOUS_BASECALL_CORRUPTION_THRESHOLD) {
      System.err.println("ERROR: " + (int) (dubious_ratio * 100) + "% of basecalls don't match strongest channel");  // debug
      internal_basecalls_corrupt = true;
    }
    
    if (bases_seen.size() != 4) {
      System.err.println("ERROR: not all of A/C/G/T observed in basecalls");  // debug
      internal_basecalls_corrupt = true;
    }

    if (internal_basecalls_corrupt) {
      // 6/2006:
      // some NCBI traces appear to have corrupt internal basecalling info.
      // e.g. NCBI ti 1009273498 (ABOBG2001D002.y.BT_013.1, local TI 46061).
      // NCBI's java viewer also shows what seems to be bad data, so probably
      // not a parsing problem (I hope).
      System.err.println("internal basecalls appear to be corrupt!  Using trivial recalling.");  // debug
      BaseBasecaller bbc = new BaseBasecaller(this);
      //      bbc.dump_peaks();
      bbc.assign_called();
      bbc.set_tracefile_values();
    }
  }

  public void dump_first_sample (String msg) {
    System.err.println("debug: " + msg);  // debug
    System.err.println("A0: " + trace_data[TRACE_A][0]);  // debug
    System.err.println("C0: " + trace_data[TRACE_C][0]);  // debug
    System.err.println("G0: " + trace_data[TRACE_G][0]);  // debug
    System.err.println("T0: " + trace_data[TRACE_T][0]);  // debug
  }

}

class abi_header {
  boolean occur;
  int offset, num_bytes, num_words, serial_number;
  String label, data;
}


