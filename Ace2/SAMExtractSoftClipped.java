package Ace2;
// extract reads containing a minimum level of soft clipping
// for putative fusion protein analysis
//
// TO DO:
// - show lower-quality nucleotides in lowercase letters?
// - low-complexity filtering?

import java.io.*;
import java.util.*;
import net.sf.samtools.*;

public class SAMExtractSoftClipped {
  private File sam_file;
  private String report_name;

  private static int MINIMUM_CLIPPED_LENGTH = 18;
  // minimum soft clipping length to extract;
  // long enough to be plausibly specific (i.e. PCR primer minimum length)
  private static int MAX_CLIP_LENGTH_TRACK = 100;
  private static int MINIMUM_CLIPPED_TRACK_LENGTH = 10;
  private static final int FLUSH_BOUNDARY = 250;
  // should be at least maximum read length
  
  private boolean FILTER_LOW_QUALITY = true;
  private int MINIMUM_MEAN_CLIPPED_REGION_QUALITY = 15;

  public SAMExtractSoftClipped (File sam_file) {
    this.sam_file = sam_file;
    report_name = sam_file.getName() + "_softclip.fa";
  }

  public void report() throws FileNotFoundException,IOException {
    SAMFileReader sfr = new SAMFileReader(sam_file);

    WorkingFile wf = new WorkingFile(report_name);
    PrintStream ps = wf.getPrintStream();

    long read_count = 0;
    long unmapped_count = 0;
    long duplicate_count = 0;
    long extracted_count = 0;
    long rejected_lq_clips = 0;

    CigarOperator co;
    Cigar c;
    boolean extract;
    long[] clip_lengths = new long[MAX_CLIP_LENGTH_TRACK + 1];
    Arrays.fill(clip_lengths, 0);

    int clen, max_clen;
    int flush_interval = 100000;

    HashMap<Integer,Integer> total_clip_counts = new HashMap<Integer,Integer>();
    // gather clip site counts for all clips of a certain length 
    // (shorter than extraction site)

    HashSet<SoftClipTrack> queue = new HashSet<SoftClipTrack>();

    Integer last_ri = null;
    int ri = 0;
    boolean usable;
    int i;

    for (SAMRecord sr : sfr) {
      if (++read_count % 1000000 == 0) {
	System.err.println("read " + read_count);  // debug
      };

      if (read_count % flush_interval == 0) {
	queue_flush_check(queue, total_clip_counts,  ps, sr.getAlignmentStart(), false);
      }

      extract = false;
      max_clen = 0;
      byte[] quals;
      int end;
      int total,mean;
      int qcount;
      
      Integer this_ri = sr.getReferenceIndex();
      if (last_ri == null || !(this_ri.equals(last_ri))) {
	queue_flush_check(queue, total_clip_counts,  ps, 0, true);
	last_ri = this_ri;
      }

      if (sr.getReadUnmappedFlag() == true) {
	unmapped_count++;
      } else if (sr.getDuplicateReadFlag() == true) {
	duplicate_count++;
      } else {
	ri = 0;
	//	System.err.println("cigar="+ SAMUtils.cigar_to_string(sr.getCigar()));  // debug

	for (CigarElement ce : sr.getCigar().getCigarElements()) {
	  co = ce.getOperator();
	  clen = ce.getLength();
	  if (co.equals(CigarOperator.MATCH_OR_MISMATCH) ||
	      co.equals(CigarOperator.INSERTION)
	      ) {
	    ri += clen;
	  } else if (co.equals(CigarOperator.SOFT_CLIP)) {
	    usable = true;
	    
	    if (FILTER_LOW_QUALITY) {
	      end = ri + clen;
	      //	      System.err.println("check " + ri + " => " + end);  // debug
	      quals = sr.getBaseQualities();
	      if (quals == null) {
		System.err.println("ERROR: null quality array");  // debug
	      } else {
		qcount = 0;
		total = 0;
		for (i=ri; i < end; i++) {
		  if (i >= quals.length) {
		    System.err.println("ERROR: read index past end of qual array!");  // debug
		  } else {
		    qcount++;
		    total += quals[i];
		    //		    System.err.println("q " + quals[i]);  // debug
		  }
		}
		//		System.err.println("done");  // debug
		if (qcount > 0) {
		  mean = total / qcount;
		  if (mean < MINIMUM_MEAN_CLIPPED_REGION_QUALITY) {
		    usable = false;
		    rejected_lq_clips++;
		  }
		  //		  System.err.println("mean="+mean);  // debug
		}
	      }

	    }

	    if (usable) {
	      if (clen <= MAX_CLIP_LENGTH_TRACK) clip_lengths[clen]++;
	      if (clen > max_clen) max_clen = clen;
	      //	    System.err.println("clip! " + sr.getReferenceName() + " " + sr.getAlignmentStart() + " " + sr.getCigarString());  // debug
	      extract = clen >= MINIMUM_CLIPPED_LENGTH;
	    }

	    ri += clen;
	  } else if (co.equals(CigarOperator.SKIPPED_REGION) ||
		     co.equals(CigarOperator.HARD_CLIP) ||
		     co.equals(CigarOperator.DELETION) ||
		     co.equals(CigarOperator.PADDING)
		     ) {
	    // no effect on read index
	  } else {
	    System.err.println("ERROR: unhandled CIGAR operator " + co);  // debug
	    System.exit(1);
	  }
	}
      }

      ArrayList<Integer> clip_sites = new ArrayList<Integer>();

      clip_track(clip_sites, sr.getAlignmentStart(), sr.getUnclippedStart());
      clip_track(clip_sites, sr.getAlignmentEnd(), sr.getUnclippedEnd());

      for (Integer site : clip_sites) {
	Integer count = total_clip_counts.get(site);
	if (count == null) count = Integer.valueOf(0);
	total_clip_counts.put(site, count + 1);
      }

      if (extract) {
	//
	// usable, queue this hit
	//
	SoftClipTrack sct = new SoftClipTrack();
	sct.position = sr.getAlignmentEnd();
	sct.sr = sr;
	sct.max_clen = max_clen;
	sct.clip_sites = clip_sites;
	queue.add(sct);

	extracted_count++;
      }
    }
    queue_flush_check(queue, total_clip_counts,  ps, 0, true);

    System.err.println("read count: " + read_count);  // debug
    System.err.println("unmapped count: " + unmapped_count);  // debug
    System.err.println("optical/pcr duplicate count: " + duplicate_count);  // debug
    System.err.println("rejected low-quality clipped regions: " + rejected_lq_clips);
    System.err.println("extracted count: " + extracted_count);  // debug

    for (i = 0; i <= MAX_CLIP_LENGTH_TRACK; i++) {
      if (clip_lengths[i] > 0) System.err.println("count for soft-clip length " + i + ": " + clip_lengths[i]);  // debug
    }


    wf.finish();
  }


  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    String bam_file = null;
    for (int i=0; i < argv.length; i++) {
      if (argv[i].equals("-bam")) {
	bam_file = new String(argv[++i]);
      } else {
	System.err.println("ERROR: unknown parameter " + argv[i]);  // debug
	System.exit(1);
      }
    }

    if (bam_file == null) {
      System.err.println("ERROR: specify -bam [file]");  // debug
    } else {
      try {
	SAMExtractSoftClipped esc = new SAMExtractSoftClipped(new File(bam_file));
	esc.report();
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
	e.printStackTrace();
      }
    }
  }

  private void clip_track (ArrayList<Integer> clip_sites, int clipped, int unclipped) {
    if (clipped != unclipped) {
      //      System.err.println("clip="+clipped + " uc="+unclipped);  // debug

      int len = Math.abs(clipped - unclipped);
      // start or end
      if (len >= MINIMUM_CLIPPED_TRACK_LENGTH) 
	clip_sites.add(clipped);
    }
  }

  private void queue_flush_check (HashSet<SoftClipTrack> queue, HashMap<Integer,Integer> total_clip_counts, PrintStream ps, int current_position, boolean force) {
    ArrayList<SoftClipTrack> remove = new ArrayList<SoftClipTrack>();

    current_position -= FLUSH_BOUNDARY;
    // pretend we're not quite as far along for safety's sake

    for (SoftClipTrack sct : queue) {
      if (force || current_position > sct.position) {
	
	boolean trackable = true;

	if (sct.clip_sites.size() == 0) {
	  System.err.println("odd read w/no leading/trailing softclip: " + sct.sr.getReadName() + " " + sct.sr.getReferenceName() + ":" + sct.sr.getAlignmentStart() + " CIGAR=" + SAMUtils.cigar_to_string(sct.sr.getCigar()));  // debug
	  trackable = false;
	}

	ArrayList<String> stuff = new ArrayList<String>();
	HashMap<String,String> features = new HashMap<String,String>();

	features.put("pos", sct.sr.getReferenceName() + "." + sct.sr.getAlignmentStart());
	// always report site because clip sites may not be trackable

	features.put("strand", sct.sr.getReadNegativeStrandFlag() ? "-" : "+");
	features.put("clip_len", Integer.toString(sct.max_clen));

	if (trackable) {
	  ArrayList<String> mapped = new ArrayList<String>();
	  for (Integer site : sct.clip_sites) {
	    mapped.add(sct.sr.getReferenceName() + "." + site + ":" + total_clip_counts.get(site));
	  }
	  features.put("clip_sites", Funk.Str.join(",", mapped));
	}

	ArrayList<String> flist = new ArrayList<String>(features.keySet());
	for (String key : flist) {
	  stuff.add(key + "=" + features.get(key));
	}

	ps.println(">" +  sct.sr.getReadName() + " " + Funk.Str.join("|", stuff));
	ps.println(new String(sct.sr.getReadBases()));

	remove.add(sct);
      }
    }

    System.err.println("queue before: " + queue.size() + "/" + total_clip_counts.size());  // debug
    queue.removeAll(remove);

    HashSet<Integer> remove_sites = new HashSet<Integer>();
    for (Integer site : total_clip_counts.keySet()) {
      if (force || current_position > site) remove_sites.add(site);
    }
    for (Integer site : remove_sites) {
      total_clip_counts.remove(site);
    }
    System.err.println("       after: " + queue.size() + "/" + total_clip_counts.size());  // debug
    
  }



}