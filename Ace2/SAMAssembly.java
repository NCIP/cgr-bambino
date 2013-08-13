package Ace2;

import net.sf.samtools.*;
import java.util.*;
import java.io.IOException;

public class SAMAssembly extends Assembly implements Observer,Runnable {
  private char[] consensus_padded;
  private ArrayList<SAMResource> sams;

  private static final String FAKE_CONS_ID = "Contig1";
  private boolean data_loaded = false;
  private boolean alignment_built = false;
  private String error_message = null;
  private int sams_loaded = 0;
  private SAMConsensusBuilder scb;
  private int start_offset;
  private AceViewerConfig avc = null;

  private static final String RAM_ERROR_MESSAGE = "Insufficient memory to create padded alignment.  Try viewing a smaller region or running the viewer with more RAM.";
  private static final String PARSE_ERROR_MESSAGE = "Error parsing BAM data: see Java console/STDERR for details.";

//   public SAMAssembly (ArrayList<SAMResource> sams, byte[] cons_unpadded, int start_offset, boolean async) throws IOException {
//     //    new Exception().printStackTrace();
//     this.sams=sams;
//     this.start_offset=start_offset;
//     scb = new SAMConsensusBuilder(cons_unpadded, start_offset);
//     data_loaded = alignment_built = false;

//     //    SAMResource.set_mismatch_filter(new SAMMismatchFilter(cons_unpadded, start_offset));
//     // DISABLED: use all reads in assembly.  Summary info about mismatch counts
//     // will now be generated which can be used during SNP calling, etc. to filter
//     // possibly incorrectly-aligned reads.
// //    System.err.println("SAMAssembly() thread=" + Thread.currentThread().getName());

//     if (async) {
//       new Thread(this).start();
//     } else {
//       run();
//     }
//   }

  public SAMAssembly (AceViewerConfig avc, boolean async) throws IOException {
    this.avc = avc;
    this.sams = avc.sams;
    this.start_offset = avc.ruler_start;
    scb = new SAMConsensusBuilder(avc.target_sequence, start_offset);
    data_loaded = alignment_built = false;

    //    SAMResource.set_mismatch_filter(new SAMMismatchFilter(cons_unpadded, start_offset));
    // DISABLED: use all reads in assembly.  Summary info about mismatch counts
    // will now be generated which can be used during SNP calling, etc. to filter
    // possibly incorrectly-aligned reads.
//    System.err.println("SAMAssembly() thread=" + Thread.currentThread().getName());

    if (async) {
      new Thread(this).start();
    } else {
      run();
    }
  }

  // begin Observer stub
  public void update (Observable o, Object arg) {
    SAMResource sr = (SAMResource) arg;
    try {
      if (++sams_loaded == sams.size()) {
	// SAM resources all loaded, now build assembly

	if (false) {
	  try {
	    System.out.println("SAMAssembly update: killing time...");
	    Thread.sleep(2000);
	  } catch (InterruptedException e) {}
	}

	build_assembly();
      }
    } catch (Exception e) {
      new Funk.ErrorReporter(e);
    }

  }
  // end Observer stub


  private void build_assembly() {
    //
    // SAMConsensusBuilder must be fully populated before calling
    //
    try {
//      System.err.println("build_assembly: thread=" + Thread.currentThread().getName());
      data_loaded = true;
      scb.build_consensus();

      consensus_padded = scb.get_consensus();

      Funk.Timer timer = new Funk.Timer("cons map");
      //
      //  map sequences to consensus:
      //
      if (avc == null) {
	System.err.println("WTF: null AVC!");  // debug
      } else {
	avc.REFERENCE_SEQUENCE_PREPADDED = scb.is_reference_prepadded();
      }

      SAMConsensusMapFactory sm = new SAMConsensusMapFactory(consensus_padded, scb.get_reference_sequence(), get_padmap(), start_offset, scb.is_reference_prepadded());
      // FIX ME: pass in SAMAssembly and query vars from there?

      ProgressInfo info = new ProgressInfo();
      for (SAMResource sr : sams) {
	info.total += sr.get_size();
      }

      boolean error = false;
      for (SAMResource sr : sams) {
	if (!sr.build_mappings(sm, info)) {
	  error = true;
	}
      }

      timer.finish();

      //      System.err.println("assembly built");  // debug

      if (error) {
	error_message = RAM_ERROR_MESSAGE;
      } else {
	if (avc.COMPRESS_INTRONS) {
	  super.build_alignment();
	  // temporary summary info
	  avc.intron_compressor = new IntronCompressor(avc, this);
	}

	// summary alignment:
	super.build_alignment();
	// FIX ME: VILE!
	// - in .ace files, alignment summary information can be built immediately
	//   after the .ace file is loaded.
	// - this would also be the case with .bam files displayed verbatim
	//   (i.e. without a padded consensus).
	// - however in our case we have to build the padded consensus and
	//   map the reads to it, an asynchronous process.  Unless we call
	//   Assembly.build_alignment() here ourselves, a race condition will
	//   be created and downstream code may execute, assuming
	//   summary information is available which isn't yet built.  We
	//   can't just extend build_alignment() because the load is asynchronous.
	//
	// need to ensure summary info is built first
	alignment_built = true;
      }
    } catch (Exception e) {
      error_message = PARSE_ERROR_MESSAGE;
      System.err.println("ERROR during assembly build");  // debug
      //      alignment_built = true;
      new ErrorReporter(e);
    }
  }

  public String get_title() {
    return "FIX ME";
  }

  public void addObserver (Observer o) {
    for (SAMResource sr : sams) {
      sr.addObserver(o);
    }
  }

  public void build_summary_info () {}

  public boolean is_loaded() {
    return data_loaded;
  }

  public boolean alignment_is_built() {
    return alignment_built;
  }

  public boolean is_empty() {
    boolean result = true;
    for (SAMResource sr : sams) {
      SAMConsensusMapping[] maps = sr.get_sequences();
      if (maps != null && maps.length > 0) {
	result = false;
	break;
      }
    }
    return result;
  }

  public boolean has_error() {
    return error_message != null;
  }

  public String get_error_message() {
    return error_message;
  }

  public char[] get_consensus_sequence() {
    return consensus_padded;
  };

  public void set_quality(FASTAQualityReader fq) {
    // not applicable to SAM/BAM
  }

  public boolean has_quality() {
    return true;
    // generally present in every SAM file
  }

  public AssemblySequence get_sequence(String id) {
    AssemblySequence result = null;
    for (SAMResource sr : sams) {
      result = sr.get_sequence(id);
      if (result != null) break;
    }
    return result;
  }

  public ArrayList<AssemblySequence> get_sequences() {
    ArrayList<AssemblySequence> all = new ArrayList<AssemblySequence>();
    for (SAMResource sr : sams) {
      // FIX ME: apply optional sorting here?
      sr.get_sequences(all, display_sequences_by_position);
    }
    return all;
  };

  public boolean supports_contigs() {
    return false;
  }
  public ArrayList<String> get_contig_id_list() {
    ArrayList<String> list = new ArrayList<String>();
    list.add(FAKE_CONS_ID);
    return list;
  }
  public String get_contig_id() {
    return FAKE_CONS_ID;
  };
  public void set_contig (String id) {};
  public String get_biggest_contig_id() {
    return FAKE_CONS_ID;
  };
  public void set_current_contig(String id) {};

  public Sample get_sample_for(String read_name) {
    SAMResource sr = get_sr_for(read_name);
    return sr == null ? null : sr.sample;
  }

  public SAMResource get_sr_for(String read_name) {
    SAMResource wanted = null;
    for (SAMResource sr : sams) {
      AssemblySequence as = sr.get_sequence(read_name);
      if (as != null) {
	wanted = sr;
	break;
      }
    }
    return wanted;
  }

  public void run() {
    boolean has_something = false;
    boolean load_error = false;
    for (SAMResource sr : sams) {
      SAMRecord[] srs = sr.get_sams();

      if (sr.has_load_error()) {
	load_error = true;
	break;
      }

      if (srs == null || srs.length == 0) {
	// no data
	//	data_loaded = true;
	//	error_message = "Can't build assembly: no reads found.";
	// no longer considered an error: might be viewing multiple bam files
	// where only some overlap
	System.err.println("empty .bam file");
      } else {
	has_something = true;
	scb.add_samrecords(srs);
      }
    }
    if (load_error) {
      error_message = RAM_ERROR_MESSAGE;
    } else if (has_something) {
      build_assembly();
    } else {
      error_message = "Nothing to display: no reads in this region.";
      System.err.println(error_message);
    }
    data_loaded = true;
  }

  public void set_consensus_sequence (char[] seq) {
    // intron trimming
    consensus_padded = seq;
  }

  public void reset_padmap() {
    pm = null;
    super.get_padmap();
  }

}

  

