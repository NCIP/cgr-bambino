package Ace2;
// TO DO:
//   - if no-interesting-mates is used, CHANGE DEFAULTS a-la seu_swarm.
//   - print config summary on start
//   - calculate speed since last chunk update
//   - dump memory usage info
//   - "verbose" switch
//   - direct query counts
//   - CONCAT BAM OUTPUT

import java.io.*;
import java.util.*;
import java.text.*;

import net.sf.samtools.*;

public class SAMExtractUnmapped2 {
  SEUConfig config;
  Integer reference_index;
  SEUMateQuery smq;
  SAMFileReader sfr_mate;
  SAMCache sam_cache;
  long time_mate_query_same_ref = 0;
  long time_mate_query_other_ref = 0;
  SEUWriter seuw = null;
  WorkingDirectory wd = null;

  int cache_hits, cache_misses;
  SAMCanonicalReference scr;
  HashMap<Integer,SEUInteresting> seui_cache;
  HashMap<Integer,SEUMateQuery> seumq_cache;

  public SAMExtractUnmapped2 (SEUConfig config) {
    this.config = config;
    seui_cache = new HashMap<Integer,SEUInteresting>();
    seumq_cache = new HashMap<Integer,SEUMateQuery>();
    wd = null;
  }

  public void extract() throws IOException {
    cache_hits = cache_misses = 0;
    SAMFileReader sfr = new SAMFileReader(config.bam_file);
    sfr_mate = new SAMFileReader(config.bam_file);
    if (config.report_basename == null) config.report_basename = config.bam_file.getName();

    ChromosomeDisambiguator cd = new ChromosomeDisambiguator(sfr);
    scr = new SAMCanonicalReference(sfr);

    Iterator<SAMRecord> query = null;
    SEUInteresting interesting = null;

    if (config.QUERY_UNMAPPED) {
      //
      // query pure-unmapped reads
      //
      query = sfr.queryUnmapped();
      reference_index = -1;
    } else {
      //
      // reference sequence query/setup
      //
      String ref_name_bam = cd.find(config.reference_name);
      if (ref_name_bam == null) throw new IOException("ERROR: can't find reference sequence " + config.reference_name + " in " + config.bam_file);
      SAMSequenceDictionary dict = sfr.getFileHeader().getSequenceDictionary();
      SAMSequenceRecord ssr = dict.getSequence(ref_name_bam);
      reference_index = ssr.getSequenceIndex();

      interesting = get_interesting(reference_index);
      
      System.err.println("reference: user=" + config.reference_name + " BAM="+ref_name_bam);  // debug

      int qstart = config.ref_start;
      int qend = config.ref_end;
      if (qstart == 0) {
	// process this entire reference sequence; 
	// get sequence length from header
	qstart = 1;
	qend = ssr.getSequenceLength();
      }
      query = sfr.queryOverlapping(ref_name_bam, qstart, qend);
      System.err.println("query="+qstart + "-" + qend);  // debug
    }

    // TO DO:
    // PRINT RUN CONFIGURATION
    System.err.println("chunk size=" + config.READ_CHUNK_COUNT);

    long read_count=0;

    if (config.ITERATE_REFERENCES ? seuw == null : true) {
      // when iterating through all references (externally),
      // only init output once
      String base_fn = null;
      if (config.temp_dir != null) {
	// write files to a temporary/working directory
	// then move to output dir
	File f = (new File(config.report_basename)).getCanonicalFile();
	File parent = f.getParentFile();
	File target_dir = parent == null ? new File(".") : parent;
	wd = new WorkingDirectory(config.temp_dir, target_dir);
	base_fn = wd.get_file(f.getName()).getCanonicalPath();
      } else {
	base_fn = config.report_basename;
	if (config.output_directory != null) {
	  File f = new File(new File(config.output_directory), base_fn);
	  base_fn = f.getCanonicalPath();
	}
      }

      if (config.WRITE_BAM) {
	seuw = new SEUWriterBAM(config, sfr, base_fn);
      } else if (config.WRITE_FASTQ) {
	seuw = new SEUWriterFASTQ(config, sfr, base_fn);
      } else {
	throw new IOException("ERROR setting up SEUWriter");
      }
    }

    SAMRecord sr;

    if (config.MATE_QUERY_GROUP) smq = new SEUMateQuery(
							config,
							interesting,
							seuw
							);

    ArrayList<SAMRecord> chunk = new ArrayList<SAMRecord>();
    sam_cache = new SAMCache();
    while (query.hasNext()) {
      sr = query.next();
      sam_cache.add(sr);
      chunk.add(sr);

      if (config.ENABLE_READ_LIMIT && read_count > config.READ_LIMIT) {
	System.err.println("read limit enabled: stopping at read #" + read_count);  // debug
	break;
      }

      if (++read_count % config.READ_CHUNK_COUNT == 0) {
	process_chunk(chunk, interesting);
	chunk.clear();
	sam_cache.clear();
      }
    }
    process_chunk(chunk, interesting);
    if (config.MATE_QUERY_GROUP) {
      ArrayList<SEUMateQuery> list = new ArrayList<SEUMateQuery>();
      list.add(smq);
      list.addAll(seumq_cache.values());
      for (SEUMateQuery q : list) {
	System.err.println("flushing queue for " + q.get_reference_name());  // debug
	q.flush();
      }
    }

    System.err.println("mate query time: same_ref=" + time_mate_query_same_ref + " other_ref=" + time_mate_query_other_ref + " total=" + (time_mate_query_same_ref + time_mate_query_other_ref));  // debug

    if (!config.ITERATE_REFERENCES) close_all();

    int cache_total = cache_hits + cache_misses;
    if (cache_total > 0) {
      // n/a when querying unmapped
      System.err.println("SAM cache hits:" + cache_hits + "/" + cache_total + " (" + (cache_hits * 100 / cache_total) + "%)");  // debug
    }

  }

  public void close_all() throws IOException {
    seuw.close();
    if (wd != null) wd.finish();
  }


  public static void main (String[] argv) {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SEUConfig config = new SEUConfig();

    File temp_dir = null;

    try {
      for (int i=0; i < argv.length; i++) {
	if (argv[i].equals("-bam")) {
	  config.bam_file = new File(argv[++i]);
	} else if (argv[i].equals("-basename")) {
	  config.report_basename = new String(argv[++i]);
	  //	} else if (argv[i].equals("-dir")) {
	  //	  config.output_directory = new String(argv[++i]);
	  // DISABLED: don't use, results need to be cached/chunked in 
	  // a standard directory.  Use -tmpdir to automatically use
	  // scratch space during building.
	} else if (argv[i].equals("-nib")) {
	  NIB.DEFAULT_NIB_DIR = new String(argv[++i]);
	  config.reference_sequence = new NIB();
	} else if (argv[i].equals("-2bit")) {
	  config.reference_sequence = new TwoBitFile(argv[++i]);
	} else if (argv[i].equals("-fasta")) {
	  String thing = argv[++i];
	  File f = new File(thing);
	  if (f.isFile()) {
	    // .fai-indexed FASTA file
	    config.reference_sequence = new FASTAIndexedFAI(thing);
	  } else if (f.isDirectory()) {
	    config.reference_sequence = new FASTADirectory(thing);
	  } else {
	    System.err.println("ERROR: not a file/directory: " + thing);  // debug
	  }
	} else if (argv[i].equals("-ref")) {
	  // reference name to query
	  config.reference_name = new String(argv[++i]);
	} else if (argv[i].equals("-start")) {
	  config.ref_start = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-end")) {
	  config.ref_end = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-chunk")) {
	  config.READ_CHUNK_COUNT = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-unmapped")) {
	  config.QUERY_UNMAPPED = true;
	} else if (argv[i].equals("-unmapped-only")) {
	  config.EXTRACT_UNMAPPED_READS_ONLY = true;
	} else if (argv[i].equals("-limit")) {
	  config.ENABLE_READ_LIMIT = true;
	  config.READ_LIMIT = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-no-indels")) {
	  config.EXTRACT_INDELS = false;
	} else if (argv[i].equals("-no-interesting-mates")) {
	  config.EXTRACT_INTERESTING_MATES = false;
	  config.MATE_QUERY_GROUP_QUEUE_LIMIT = 500;
	  config.READ_CHUNK_COUNT = 5000;
	} else if (argv[i].equals("-no-duplicates")) {
	  config.EXTRACT_DUPLICATES = false;
	} else if (argv[i].equals("-mate-query-direct")) {
	  config.MATE_QUERY_DIRECT = true;
	  config.MATE_QUERY_GROUP = false;
	} else if (argv[i].equals("-mate-query-group")) {
	  config.MATE_QUERY_GROUP = true;
	  config.MATE_QUERY_DIRECT = false;
	} else if (argv[i].equals("-mate-query-group-distance")) {
	  config.MATE_QUERY_GROUP_MAX_ALIGN_DISTANCE = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-mate-query-group-queue")) {
	  config.MATE_QUERY_GROUP_QUEUE_LIMIT = Integer.parseInt(argv[++i]);
	} else if (argv[i].equals("-verbose-mate-query")) {
	  SEUMateQuery.VERBOSE = true;
	} else if (argv[i].equals("-require-canonical-mates")) {
	  config.EXTRACT_MATES_ON_STANDARD_REFERENCES_ONLY = true;
	} else if (argv[i].equals("-no-require-canonical-mates")) {
	  config.EXTRACT_MATES_ON_STANDARD_REFERENCES_ONLY = false;
	} else if (argv[i].equals("-write-fastq")) {
	  config.WRITE_FASTQ = true;
	  config.WRITE_BAM = false;
	} else if (argv[i].equals("-temp-dir")) {
	  config.temp_dir = new File(argv[++i]);
	} else if (argv[i].equals("-tmpdir")) {
	  // use $ENV{TMPDIR}, set on clusters
	  String dir = System.getenv("TMPDIR");
	  if (dir == null) {
	    System.err.println("ERROR: no TMPDIR environment variable; use -temp-dir X to specify manually");  // debug
	    System.exit(1);
	  } else {
	    config.temp_dir = new File(dir);
	  }
	} else if (argv[i].equals("-all-ref")) {
	  config.ITERATE_REFERENCES = true;
	} else {
	  System.err.println("ERROR: unknown argument " + argv[i]);  // debug
	  usage();
	  System.exit(1);
	  //	usage();
	}
      }
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
      System.exit(1);
    }

    if (config.bam_file == null ||
	(config.reference_sequence == null && !config.EXTRACT_UNMAPPED_READS_ONLY) ||
	(config.reference_name == null &&
	 !(config.QUERY_UNMAPPED || config.ITERATE_REFERENCES))
	) {
      usage();
    } else {
      try {
	System.err.println("extract mapped mates of interesting mapped reads?: " + config.EXTRACT_INTERESTING_MATES);  // debug
	System.err.println("only extract mates mapped to canonical references?: " + config.EXTRACT_MATES_ON_STANDARD_REFERENCES_ONLY);  // debug
	System.err.println("extract unmapped reads only?: " + config.EXTRACT_UNMAPPED_READS_ONLY);  // debug

	SAMExtractUnmapped2 seu = new SAMExtractUnmapped2(config);
	if (config.ITERATE_REFERENCES) {
	  
	  if (config.reference_sequence == null) throw new Exception("reference sequence not specified");

	  SAMFileReader sfr = new SAMFileReader(config.bam_file);
	  SAMFileHeader h = sfr.getFileHeader();
	  SAMSequenceDictionary dict = h.getSequenceDictionary();

	  config.QUERY_UNMAPPED = false;

	  ArrayList<String> references = new ArrayList<String>();
	  for (SAMSequenceRecord ssr : dict.getSequences()) {
	    // query each reference sequence in turn
	    String rname = ssr.getSequenceName();
	    int rs_len = config.reference_sequence.get_length(rname);
	    if (rs_len == ReferenceSequence.NULL_LENGTH) {
	      System.err.println("skipping ID " + rname + ", not in reference file");  // debug
	    } else {
	      references.add(rname);
	    }
	  }

	  for (String rname : references) {
	    config.reference_name = rname;
	    System.err.println("extracting reads for " + config.reference_name);  // debug
	    seu.extract();
	  }

	  // final pass: unmapped reads
	  config.QUERY_UNMAPPED = true;
	  config.reference_name = null;
	  System.err.println("extracting unmapped reads");  // debug
	  seu.extract();

	  seu.close_all();
	  // finish

	} else {
	  // extract sequences for a single reference (or unmapped)
	  seu.extract();
	}
      } catch (Exception e) {
	System.err.println("ERROR: " + e);  // debug
	e.printStackTrace();
      }
    }
  }

  private static void usage() {
    System.err.println("-bam [file]");  // debug
    System.err.println("-nib [dir]");  // debug
    System.err.println("-ref [reference to query]");  // debug
  }

  private void mate_check (SAMRecord sr, SEUInteresting interesting) throws IOException {
    //
    // FIX ME: add check for mapping to undesirable reference seqs
    //
    if (sr.getReadPairedFlag() &&
	!sr.getMateUnmappedFlag() &&
	!config.EXTRACT_UNMAPPED_READS_ONLY
	) {
      if (sr.getMateReferenceIndex().equals(reference_index)) {
	//
	//  read's mate is mapped to the reference we're currently processing
	//
	SAMRecord mate = sam_cache.find_mate(sr);

	if (mate == null) {
	  cache_misses++;
	  if (config.MATE_QUERY_DIRECT) {
	    // directly query mate reads as we encounter them.
	    // This is INSANELY SLOW and will consume nearly 100% of run time
	    // if this is the only method used to retrieve mates.
	    //
	    // Processing large blocks of reads and caching helps a lot
	    // but there can still be significant thrashing if read coverage
	    // is very high.
	    mate = direct_mate_query(sr, true);
	  } else if (config.MATE_QUERY_GROUP) {
	    // bucket neighboring queries for mate sequences.
	    // HUGE SPEED IMPROVEMENT.
	    smq.add_mate(sr);
	  }
	} else {
	  cache_hits++;
	}

	if (mate != null) {
	  interesting.interesting_check(mate);
	  // stamp w/XU tag
	  seuw.addAlignment(mate, interesting);
	}
      } else {
	// mate is mapped to a different reference sequence.
	// just load **all** reference sequences into memory?
	//
	// another object to queue queries, then load each refseq and flush?
	// doing in another thread will probably cause problems, but
	// doing it synchronously would probably be OK
	// => JUST SAVE SAMRECORDS BY CHR AND USE GETMATE CALL!

	if (config.EXTRACT_MATES_ON_STANDARD_REFERENCES_ONLY ? 
	    scr.is_canonical(sr.getMateReferenceIndex()) : true) {
	  if (config.MATE_QUERY_DIRECT) {
	    // directly query mate reads as we need them
	    // DOG SLOW
	    SAMRecord mate = direct_mate_query(sr, false);
	    if (mate != null) {
	      System.err.println("FIX ME: mate in different reference: " + sr.getReadName() + " @ " + sr.getMateReferenceName() + "." + sr.getMateAlignmentStart());  // debug
	      SEUInteresting si = get_interesting(mate.getReferenceIndex());
	      si.interesting_check(mate);
	      // stamp w/XU tag
	      seuw.addAlignment(mate, si);
	    }
	  } else if (config.MATE_QUERY_GROUP) {
	    get_seumq(sr.getMateReferenceIndex()).add_mate(sr);
	  }
	} else {
	  //	  System.err.println("ignoring mate mapped to " + sr.getMateReferenceName());  // debug
	}
      }
    }
  }


  private void process_chunk (ArrayList<SAMRecord> chunk,
			      SEUInteresting interesting) throws IOException {
    int aligned = 0;

    for (SAMRecord sr : chunk) {
      if (sr.getDuplicateReadFlag() && !config.EXTRACT_DUPLICATES) continue;

      String name = sr.getReadName();

      if (sr.getReadUnmappedFlag() == true) {
	// unmapped read
	seuw.addAlignment(sr, null);
	mate_check(sr, interesting);
      } else {
	// mapped read
	if (config.EXTRACT_UNMAPPED_READS_ONLY) continue;
	int seu_flags = interesting.interesting_check(sr);
	aligned = sr.getAlignmentStart();
	if (seu_flags > 0) {
	  // interesting
	  seuw.addAlignment(sr, interesting);
	  // save interesting mapped read
	  if (config.EXTRACT_INTERESTING_MATES) mate_check(sr, interesting);
	}
      }
    }
    
    log_msg("processed chunk of " + chunk.size(), false);
    if (aligned > 0) System.err.print(", last_pos=" + aligned);
    System.err.println("");  // debug
    if (config.MATE_QUERY_GROUP) {
      System.err.println("mate query cache status:");  // debug
      System.err.println("SMQ: " + smq.get_reference_name() +": " + smq.get_job_count());  // debug

      for (SEUMateQuery q : seumq_cache.values()) {
	System.err.println("  " + q.get_reference_name() + ": " + q.get_job_count());  // debug
      }

    }

  }

  private SEUMateQuery get_seumq (int ri) {
    // FIX ME: add option to use a persistent SEUInteresting object
    // at cost of higher RAM usage
    SEUMateQuery result = seumq_cache.get(ri);
    if (result == null)
      seumq_cache.put(ri, result = new SEUMateQuery(config, seuw));
    return result;
  }

  private SEUInteresting get_interesting (int ri) {
    //
    // FIX ME: utility class to do this via reflection / generics?
    //
    SEUInteresting result = seui_cache.get(ri);
    if (result == null)
      seui_cache.put(ri, result = new SEUInteresting(config, ri));
    return result;
  }

  private SAMRecord direct_mate_query (SAMRecord sr, boolean on_same_reference) {
    long start_time = System.currentTimeMillis();
    SAMRecord mate = sfr_mate.queryMate(sr);
    int elapsed = (int) (System.currentTimeMillis() - start_time);
    if (on_same_reference) {
      time_mate_query_same_ref += elapsed;
    } else {
      time_mate_query_other_ref += elapsed;
    }
    if (mate == null) System.err.println("ERROR: couldn't fetch mate for " + sr.getReadName());  // debug
    if (mate != null) {
      System.err.println("direct mate query: " + sr.getReadName() + " on_same_ref:" + on_same_reference + " main="+sr.getAlignmentStart() + " mate="+mate.getAlignmentStart() + " took=" + elapsed  + " cache=" + sam_cache.get_cache_range());  // debug
    }
    return mate;
  }

  private void add_to_mate_queue (SAMRecord sr) {
    // - find/create/cache the SMQ for the MATE reference
    // - smq.add_mate(sr)
    // - FINISH FLUSH AT END OF RUN
  }

  private void log_msg (String msg, boolean newline) {
    Calendar now = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("E MMM d HH:mm:ss zzz yyyy");
    System.err.print(sdf.format(now.getTime()) + ": " + msg);
    if (newline) System.err.println("");  // debug
  }



}