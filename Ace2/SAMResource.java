package Ace2;

import net.sf.samtools.*;
import net.sf.samtools.util.CloseableIterator;
import net.sf.samtools.SAMRecord.*;

import java.net.*;
import java.util.*;
import java.io.*;
import java.awt.Color;

public class SAMResource extends Observable {
  //
  // a SAM/BAM resource and associated annotations
  //

  URL url = null;
  public File file = null;
  // location of SAM/BAM: either URL or File

  Sample sample;
  SAMRegion region = null;

  boolean LOAD_DUPLICATES = true;
  // whether to load optical/PCR duplicates
  boolean sam_load_error;

  Color custom_color = null;

  private SAMRecord[] srs;
  // generally temporary until SAMConsensusMapping built
  // FIX ME: replace with flyweight representation??
  // (probably no, will likely need SAMRecord access in future)
  
  private SAMConsensusMapping[] maps, maps_by_name;
  // kept; FIX ME: cache???
  private HashMap<String,SAMConsensusMapping> name2map = new HashMap<String,SAMConsensusMapping>();

  //  private static final int NOTIFY_MOD = 25000;
  private static final int NOTIFY_MOD = 10000;
  private static final int ALIGNMENT_MEMORY_CHECK_MOD = 250;
  //  private static final int SAM_MEMORY_CHECK_MOD = 100;
  private static final int SAM_MEMORY_CHECK_MOD = 25;
  //  private static final int SAM_MEMORY_CHECK_MOD = 250;

  private static SAMMismatchFilter mf = null;
  private int unmapped_reads, filtered_reads, duplicate_ignored_reads;
  private SAMFileReader mysfr = null;

  private static boolean RESTRICT_LOAD = false;
  private static ArrayList<String> RESTRICT_STRINGS = new ArrayList<String>();
  private static ArrayList<String> RESTRICT_FRS = new ArrayList<String>();
  private static ArrayList<String> RESTRICT_STRINGS_NEGATIVE = new ArrayList<String>();

  private static boolean RESTRICT_LOAD_OVERLAP = false;
  private static int RESTRICT_OVERLAP = 0;

  private static boolean RESTRICT_LOAD_FR = false;
  private static boolean RESTRICT_LOAD_FR_F = false;

  public SAMResource () {
    sample = new Sample();
    // init (unknown) sample info
    if (false) {
      System.err.println("setting debug sample info");  // debug
      sample.set_normal(true);
    }
  }

  public int get_size() {
    return srs == null ? 0 : srs.length;
  }

  public void close() {
    if (mysfr != null) {
      mysfr.close();
      mysfr = null;
    }
  }
  
  public void set_file (String filename) {
    file = new File(filename);
  }

  public File get_file () {
    return file;
  }

  public SAMFileReader getSAMFileReader() throws IOException {
    if (mysfr == null) {
      if (file != null) {
	// local file, region specification allowed
	//	System.err.println("local file = " +file);  // debug
	mysfr = new SAMFileReader(file);
      } else if (url != null) {
	// URL
	if (region != null) System.err.println("WARNING: can't query regions in URL mode: " + region);
	//	System.err.println("open stream = " + url);  // debug
	mysfr = new SAMFileReader(url.openStream());
      } else {
	throw new IOException("need file or URL");
      }
    } else {
      System.err.println("using cached SFR");  // debug
    }
    return mysfr;
  }

  public CloseableIterator<SAMRecord> get_iterator() throws IOException {
    SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
    // STFU

    SAMFileReader sfr = getSAMFileReader();
    CloseableIterator iterator = null;
    //    System.err.println("region="+region + " url=" + url);  // debug
    //    System.exit(1);
    if (region != null && url == null) {
      // range-based mode: assume local file
      //	    System.err.println("path="+url.getPath());  // debug
      SAMFileHeader header = sfr.getFileHeader();
      SAMSequenceRecord sr = header.getSequence(region.tname);
      String region_key = region.tname;
      //      System.err.println("rkey="+region_key);  // debug

      if (sr == null) {
	String tname2;
	if (region.tname.indexOf("chr") == 0) {
	  // see if user specified chrX, and file uses X
	  tname2 = new String(region.tname.substring(3));
	} else {
	  // if see user specified X, and file uses chrX
	  tname2 = "chr" + region.tname;
	}

	sr = header.getSequence(tname2);
	if (sr != null) {
	  //	  System.err.println("remapping region key to " + tname2);  // debug
	  region_key = tname2;
	}
      }
      //      System.err.println("SR="+sr + " start="+region.range.start);  // debug

      if (sr != null) {
	// user has specified a valid reference sequence name
	if (region.range.start == -1) region.range.start = 1;
	if (region.range.end == -1) region.range.end = sr.getSequenceLength();
	// provide default query start/end if none provided
      }

      //      iterator = sfr.queryContained(region_key, region.range.start, region.range.end);
      // only reads completely contained in the region, probably not what we want
      iterator = sfr.queryOverlapping(region_key, region.range.start, region.range.end);
      // reads having any portion in the region: must check boundaries when parsing results!
    } else {
      // stream-based mode from URL
      iterator = sfr.iterator();
    }
    return iterator;
  }

  public URL get_url() {
    return url;
  }

  public SAMRecord[] get_sams() {
    sam_load_error = false;
    try {
      if (srs == null) {
	CloseableIterator<SAMRecord> iterator = get_iterator();

	//    (new Exception()).printStackTrace();
	Funk.Timer timer = new Funk.Timer("sam load");

	ArrayList<SAMRecord> sams = new ArrayList<SAMRecord>();
	unmapped_reads = 0;
	filtered_reads = 0;
	duplicate_ignored_reads = 0;
	int count = 0;
	boolean usable;

	MemoryMonitor mm = new MemoryMonitor();
	//	mm.set_sustained_stress_time(1500);

	if (RESTRICT_LOAD || RESTRICT_LOAD_OVERLAP || RESTRICT_LOAD_FR) {
	  System.err.println("DEBUG: RESTRICTING LOAD");  // debug
	}

	while (iterator.hasNext()) {
	  SAMRecord samRecord = iterator.next();
	  usable = true;
	  if (samRecord.getReadUnmappedFlag() == true) {
	    unmapped_reads++;
	    usable = false;
	  } else if (mf != null) {
	    //
	    //  additional filters/checks here
	    //
	    if (!mf.filter(samRecord)) {
	      usable = false;
	      filtered_reads++;
	    }
	  }

	  if (RESTRICT_LOAD_FR) {
	    if (samRecord.getReadNegativeStrandFlag()) {
	      usable = !RESTRICT_LOAD_FR_F;
	    } else {
	      usable = RESTRICT_LOAD_FR_F;
	    }
	  }

	  if (RESTRICT_LOAD_OVERLAP) {
	    if (samRecord.getAlignmentStart() > RESTRICT_OVERLAP ||
		samRecord.getAlignmentEnd() < RESTRICT_OVERLAP) {
	      usable = false;
	    }
	  }

	  if (RESTRICT_LOAD) {
	    String rn = samRecord.getReadName();
	    // 	      usable = rn.indexOf("1:99:246:1632") > -1 || rn.indexOf("1:10:1642:479") > -1;
	    //	      usable = rn.indexOf("7:15:991:1160") > -1;
	    //	      if (samRecord.getReadNegativeStrandFlag() == false) usable = false;
	    // usable = rn.indexOf("1:6:1519:646") > -1;
	    // usable = rn.indexOf("1:76:1384:858") > -1;
	    //usable = rn.indexOf("50:1090:552") > -1;

	    //	      usable = rn.indexOf("6:85:310:1816") > -1;

	    if (RESTRICT_STRINGS.size() > 0 && usable) {
	      //
	      // read name must match one of the given strings to be displayable
	      // (unless already disqualified, e.g. unmapped
	      //
	      boolean ok = false;
	      int len = RESTRICT_STRINGS.size();

	      for (int i = 0; i < len; i++) {
		if (rn.indexOf(RESTRICT_STRINGS.get(i)) > -1) {
		  // matches
		  if (RESTRICT_LOAD_FR) {
		    String dir = RESTRICT_FRS.get(i);
		    if (dir.equals("F")) {
		      if (!samRecord.getReadNegativeStrandFlag()) ok = true;
		    } else if (dir.equals("R")) {
		      if (samRecord.getReadNegativeStrandFlag()) ok = true;
		    }
		  } else {
		    ok = true;
		  }
		  if (ok) usable = true;
		}
	      }
	      //	      System.err.println("rn:" + rn + " usable:" + usable);  // debug
	      usable = ok;
	    }

	    for (String neg : RESTRICT_STRINGS_NEGATIVE) {
	      // 
	      //  read name can't match one of the given strings to be displayed
	      //
	      if (rn.indexOf(neg) > -1) {
		usable = false;
		break;
	      }
	    }

	  }


	  //	    if (true) {
	  //	      System.err.println(samRecord.getReadName() + " NOT primary?: " + samRecord.getNotPrimaryAlignmentFlag());  // debug
	  //	    }

	  if (!LOAD_DUPLICATES) {
	    if (samRecord.getDuplicateReadFlag()) {
	      usable = false;
	      duplicate_ignored_reads++;
	    }
	  }

	  if (false && usable) {
	    System.err.println("checking tags...");  // debug
	    if (samRecord.getAttributesBinarySize() > 0) {
	      // gurgle: getAttributes() crashes with a null pointer exception
	      // if file doesn't contain any attributes.  Better to do this
	      // or just catch the exception?
	      for (SAMTagAndValue tv : samRecord.getAttributes()) {
		System.err.println("tag="+tv.tag);  // debug
	      }
	    }
	  }

	  if (usable) {
	    //	    System.err.println("debug usable, as=" + samRecord.getAlignmentStart() +  " cigar="+SAMUtils.cigar_to_string(samRecord.getCigar()) + " unmapped=" + samRecord.getReadUnmappedFlag());

	    sams.add(samRecord);

	    if (++count % SAM_MEMORY_CHECK_MOD == 0 && mm.is_sustained_stressed()) {
	      sam_load_error = true;
	      break;
	    }
	  }
	}
	iterator.close();
	close();
	timer.finish();

	System.err.println("final SAM count: " + sams.size());  // debug


	if (false) {
	  System.err.println("DEBUG: force SAM load error");  // debug
	  sam_load_error = true;
	}

	if (sam_load_error) {
	  System.err.println("ERROR: ran out of RAM during SAM load");
	} else {
	  System.err.println("reads: removed " + unmapped_reads + " unmapped, " +
			     duplicate_ignored_reads + " ignored_duplicates, " +
			     filtered_reads + " by filters, " +
			     sams.size() + " remaining");  // debug

	  SAMRecord[] srs_tmp = new SAMRecord[sams.size()];
	  int i = 0;
	  for (SAMRecord sr : sams) {
	    srs_tmp[i++] = sr;
	  }
	  srs = srs_tmp;
	}
      }
    } catch (Exception e) {
      new Funk.ErrorReporter(e);
    }
    return srs;
  }

  public boolean has_load_error() {
    return sam_load_error;
  }

  public boolean build_mappings (SAMConsensusMapFactory mf, ProgressInfo info) {
    boolean ok = true;
    try {
      int i;
      maps = new SAMConsensusMapping[srs.length];
      MemoryMonitor mm = new MemoryMonitor();

      for (i=0; i < srs.length; i++) {
	maps[i] = mf.map(srs[i]);
	// do we really need this, or just use hashmap values?
	// - problem with hashmap values is that they will be unsorted
	// - maybe use TreeMap?
	name2map.put(new String(maps[i].get_name()), maps[i]);
	// convenience

	if (i % NOTIFY_MOD == 0) {
	  if (i > 0) info.add_processed(NOTIFY_MOD);
	  setChanged();
	  notifyObservers(info);
	}

	if (i % ALIGNMENT_MEMORY_CHECK_MOD == 0 && mm.is_sustained_stressed()) {
	  System.err.println("ERROR: sustained low-memory condition!  Quitting.");  // debug
	  maps = null;
	  name2map = null;
	  ok = false;
	  break;
	}
      }
      info.add_processed(i % NOTIFY_MOD);
      setChanged();
      notifyObservers(info);

      maps_by_name = null;

      srs = null;
      // flush raw SAMRecord array since references are held within SAMConsensusMapping
      if (false) {
	System.err.println("garbage collecting");  // debug
	System.gc();
      }
    } catch (Exception e) {
      System.err.println("mapping error!: " + e);  // debug
      e.printStackTrace();
      ok = false;
    }
    return ok;
  }

  public SAMConsensusMapping[] get_sequences () {
    return maps;
  }

  public void get_sequences (ArrayList<AssemblySequence> list, boolean by_position) {
    //    System.err.println("get_sequences, by_position:"+by_position);  // debug
    if (!by_position && maps_by_name == null) {
      //      System.err.println("INIT!");  // debug
      ArrayList<String> names = new ArrayList<String>(name2map.keySet());
      Collections.sort(names);
      maps_by_name = new SAMConsensusMapping[maps.length];
      int i=0;
      for (String name : names) {
	maps_by_name[i++] = name2map.get(name);
      }
    }
    SAMConsensusMapping[] set = by_position ? maps : maps_by_name;
    for (int i=0; i < set.length; i++) {
      list.add(set[i]);
    }
  }

  public AssemblySequence get_sequence(String id) {
    return name2map.get(id);
  }

  public void set_tumor_normal (String tn) {
    if (tn == null) {
      sample.set_normal(false);
      sample.set_tumor(false);
    } else {
      tn = tn.toUpperCase();
      if (tn.equals("N")) {
	sample.set_normal(true);
      } else if (tn.equals("T")) {
	sample.set_tumor(true);
      } else {
	System.err.println("ERROR: invalid tumor/normal value (T or N)");  // debug
      }
    }
  }

  public void import_data (SAMResourceTags dtype, String value) {
    try {
      switch (dtype) {
      case SAM_URL:
	try {
	  url = new URL(value);
	} catch (MalformedURLException e) {
	  // try local file
	  // do this only AFTER trying as URL, as in some web-launched
	  // modes we won't have File access permissions
	  //	  url = new URL(localfile_to_url(value));
	  url = null;
	  File f = new File(value);
	  if (f.exists()) {
	    set_file(value);
	  } else {
	    System.err.println("ERROR: can't find " + value + " as file or URL");  // debug
	  }
	}
	break;
      case SAM_SAMPLE:
	sample.set_sample_id(new String(value));
	//	System.err.println("set sample ID to " + value);  // debug
	break;
      case SAM_TUMOR_NORMAL:
	set_tumor_normal(value);
	break;
      default:
	System.err.println("unhandled dtype " + dtype);  // debug
	break;
      }
    } catch (Exception e) {
      System.err.println("SAMResource import error:");  // debug
      e.printStackTrace();
    }
  }

  public static void set_mismatch_filter (SAMMismatchFilter mf) {
    SAMResource.mf = mf;
  }

  private static String localfile_to_url (String s) {
    // hack: assumes in local directory
    String result = null;
    try {
      File f = new File(s);
      result = "file://localhost/" + f.getCanonicalFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  public SAMRegion get_region() {
    return region;
  }

  public void set_region (SAMRegion region) {
    close();
    srs = null;
    name2map = new HashMap<String,SAMConsensusMapping>();
    this.region = region;
  }

  //  public void set_sample_from_url() {
  public void detect_sample_id() {
    String basename = null;
    if (url != null) {
      String path = url.getPath();
      int li = path.lastIndexOf("/");
      if (li > 0) {
	basename = path.substring(li + 1);
	// basename
      }
    } else if (file != null) {
      basename = file.getName();
      //      System.err.println("bn = " + basename);  // debug
    }

    if (basename != null) sample.set_sample_id(basename);

  }

  public static void add_restrict_string (String pattern) {
    // debug: restrict loading to only reads matching pattern
    String[] s = pattern.split(",");
    RESTRICT_LOAD = true;
    System.err.println("adding restrict of " + s[0]);  // debug
    RESTRICT_STRINGS.add(new String(s[0]));
    RESTRICT_FRS.add(s.length == 1 ? "" : new String(s[1]));
  }

  public static void add_negative_restrict_string (String pattern) {
    // debug: restrict loading, read names are not usable if they match pattern
    String[] s = pattern.split(",");
    RESTRICT_LOAD = true;
    System.err.println("adding negative restrict of " + s[0]);  // debug
    RESTRICT_STRINGS_NEGATIVE.add(new String(s[0]));
  }

  public static void set_restrict_overlap (int pos) {
    RESTRICT_LOAD_OVERLAP = true;
    RESTRICT_OVERLAP = pos;
  }

  public static void set_restrict_fr (boolean is_f) {
    RESTRICT_LOAD_FR = true;
    RESTRICT_LOAD_FR_F = is_f;
  }

  public boolean has_maps() {
    return maps != null;
  }

  public Iterable<SAMRecord> get_samrecord_iterable() {
    return new SAMRecordArrayIterable(maps);
  }

  public void set_load_duplicates (boolean v) {
    LOAD_DUPLICATES = v;
  }

  public Sample get_sample() {
    return sample;
  }

  public String get_basename() {
    String result = null;
    if (file != null) {
      result = file.getName();
    } else if (url != null) {
      result = url.toString();
      System.err.println("FIX ME: basename for URL?");  // debug
    } else {
      System.err.println("error: no file or url!");  // debug
    }
    return result;
  }

  public void set_sams (SAMRecord[] srs) {
    // use SAMRecords already loaded elsewhere
    // (e.g. BAMExcerpt)
    this.srs = srs;
  }

  public void set_sams (ArrayList<SAMRecord> sams) {
    // use SAMRecords already loaded elsewhere
    srs = new SAMRecord[sams.size()];
    int i = 0;
    for (SAMRecord sr : sams) {
      srs[i++] = sr;
    }
  }


}
