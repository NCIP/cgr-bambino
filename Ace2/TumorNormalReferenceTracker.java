package Ace2;
//
// track SAMRecord/Sample by reference/variant status
//

import net.sf.samtools.*;
import java.util.*;

import static Ace2.TumorNormal.TUMOR_BYTE;
import static Ace2.TumorNormal.NORMAL_BYTE;

public class TumorNormalReferenceTracker {
  EnumMap<ReferenceOrVariant,ArrayList<SNPTrackInfo>> tracker;
  EnumMap<ReferenceOrVariant,EnumMap<TumorNormal,EnumMap<Strand,Integer>>> counter;

  public static ArrayList<ReferenceOrVariant> all_rov;
  public static ArrayList<Strand> all_strand;
  public static ArrayList<TumorNormal> all_tn;

  public TumorNormalReferenceTracker() {
    // static setup:
    if (all_rov == null) {
      all_rov = new ArrayList<ReferenceOrVariant>();
      all_rov.add(ReferenceOrVariant.REFERENCE);
      all_rov.add(ReferenceOrVariant.VARIANT);

      all_strand = new ArrayList<Strand>();
      all_strand.add(Strand.STRAND_POSITIVE);
      all_strand.add(Strand.STRAND_NEGATIVE);

      all_tn = new ArrayList<TumorNormal>();
      all_tn.add(TumorNormal.TUMOR);
      all_tn.add(TumorNormal.NORMAL);
      all_tn.add(TumorNormal.UNKNOWN);
    }

    reset_counts();
  }

  private void reset_counts() {
    tracker = new EnumMap<ReferenceOrVariant,ArrayList<SNPTrackInfo>>(ReferenceOrVariant.class);
    
    //
    //  initialize counts for ReferenceOrVariant -> Strand -> TumorNormal 
    //
    counter = new EnumMap<ReferenceOrVariant,EnumMap<TumorNormal,EnumMap<Strand,Integer>>>(ReferenceOrVariant.class);
    for (ReferenceOrVariant rov : all_rov) {
      EnumMap<TumorNormal,EnumMap<Strand,Integer>> tn_bucket = new EnumMap<TumorNormal,EnumMap<Strand,Integer>>(TumorNormal.class);
      counter.put(rov, tn_bucket);
      for (TumorNormal tn : all_tn) {
	EnumMap<Strand,Integer> strand_bucket = new EnumMap<Strand,Integer>(Strand.class);	
	tn_bucket.put(tn, strand_bucket);
	for (Strand str : all_strand) {
	  strand_bucket.put(str, new Integer(0));
	}
      }
    }

  }

  //  public void add (ReferenceOrVariant rov, SAMRecord sr, Sample sample) {
  //    System.err.println("raw add() not implemented");  // debug
  //  }

  public void add (IndelInfo ii) {
    // indels always assumed to be the variant type
    //    bucket_add(ReferenceOrVariant.VARIANT, new SNPTrackInfo(ii.sr, (byte) 0, ii.tumor_normal, ii.minimum_flanking_sequence));
    bucket_add(ReferenceOrVariant.VARIANT, new SNPTrackInfo(ii.sr, -1, ii.tumor_normal));
  }

  public void add (ReferenceOrVariant rov, SNPTrackInfo sti) {
    bucket_add(rov, sti);
  }

  public void addAll (ArrayList<IndelInfo> iis) {
    // indels always assumed to be the variant type
    for (IndelInfo ii : iis) {
      add(ii);
    }
  }

  private String get_name (SAMRecord sr) {
    return sr.getReadName() + "." + (sr.getReadNegativeStrandFlag() ? "R" : "F");
  }

  public void add_set (ReferenceOrVariant rov, Strand strand, TumorNormal tn, int count) {
    EnumMap<Strand,Integer> bucket = counter.get(rov).get(tn);
    int current = bucket.get(strand);
    bucket.put(strand, current + count);
  }


  public ArrayList<SNPTrackInfo> get_sti (ReferenceOrVariant rov) {
    return tracker.get(rov);
  }

  public void analyze() {
    //
    // reconcile lists and generate counts
    //
    ArrayList<SNPTrackInfo> refs = tracker.get(ReferenceOrVariant.REFERENCE);
    ArrayList<SNPTrackInfo> variants = tracker.get(ReferenceOrVariant.VARIANT);
    
    HashSet<String> variant_names = new HashSet<String>();
    if (variants != null && refs != null) {
      // remove any variant set reads from reference set list.
      // This happens for indels, where the event occurs between
      // reference bases (so tracked at the same site as a normal reference base).
      for (SNPTrackInfo sti : variants) {
	variant_names.add(get_name(sti.sr));
      }

      ArrayList<SNPTrackInfo> remove = new ArrayList<SNPTrackInfo>();
      for (SNPTrackInfo sti : refs) {
	if (variant_names.contains(get_name(sti.sr))) {
	  //	  System.err.println("TOSSING " + get_name(sti.sr));  // debug
	  remove.add(sti);
	}
      }
      refs.removeAll(remove);
    }

    int error_count = 0;

    error_count += bucket_rov_tn(ReferenceOrVariant.REFERENCE);
    error_count += bucket_rov_tn(ReferenceOrVariant.VARIANT);

    if (error_count > 0) {
      System.err.println("ERROR: can't track tumor/normal counts for " + error_count + " reads.  Don't know whether is BAM data is normal or tumor; use \"-tn [N|T]\" command line parameter");  // debug
    }
  }

  protected void bucket_add(ReferenceOrVariant rov, SNPTrackInfo sti) {
    ArrayList<SNPTrackInfo> bucket = tracker.get(rov);
    if (bucket == null) tracker.put(rov, bucket = new ArrayList<SNPTrackInfo>());
    bucket.add(sti);
  }

  public int get_count (ReferenceOrVariant rov, TumorNormal tn) {
    int total = 0;
    for (Integer count : counter.get(rov).get(tn).values()) {
      total += count;
    }
    return total;
  }

  public int get_count (ReferenceOrVariant rov, TumorNormal tn, Strand str) {
    return counter.get(rov).get(tn).get(str);
  }

  public int get_coverage () {
    //    return reference_normal_count + reference_tumor_count + variant_normal_count + variant_tumor_count;
    return get_count(ReferenceOrVariant.REFERENCE, TumorNormal.NORMAL) +
      get_count(ReferenceOrVariant.REFERENCE, TumorNormal.TUMOR) +
      get_count(ReferenceOrVariant.VARIANT, TumorNormal.NORMAL) +
      get_count(ReferenceOrVariant.VARIANT, TumorNormal.TUMOR);
  }

  public int get_coverage_fwd() {
    int count = 0;
    for (ReferenceOrVariant rov : all_rov) {
      EnumMap<TumorNormal,EnumMap<Strand,Integer>> bucket = counter.get(rov);
      for (TumorNormal tn : all_tn) {
	count += bucket.get(tn).get(Strand.STRAND_POSITIVE);
      }
    }
    return count;
  }

  public int get_coverage_rev() {
    int count = 0;
    for (ReferenceOrVariant rov : all_rov) {
      EnumMap<TumorNormal,EnumMap<Strand,Integer>> bucket = counter.get(rov);
      for (TumorNormal tn : all_tn) {
	count += bucket.get(tn).get(Strand.STRAND_NEGATIVE);
      }
    }
    return count;
  }

  public int get_variant_fwd_count() {
    EnumMap<TumorNormal,EnumMap<Strand,Integer>> bucket = counter.get(ReferenceOrVariant.VARIANT);
    int count = 0;
    for (TumorNormal tn : all_tn) {
      count += bucket.get(tn).get(Strand.STRAND_POSITIVE);
    }
    return count;
  }

  public int get_variant_rev_count() {
    EnumMap<TumorNormal,EnumMap<Strand,Integer>> bucket = counter.get(ReferenceOrVariant.VARIANT);
    int count = 0;
    for (TumorNormal tn : all_tn) {
      count += bucket.get(tn).get(Strand.STRAND_NEGATIVE);
    }
    return count;
  }

  public boolean get_variant_fwd_reverse_confirmation() {
    return get_variant_fwd_count() > 0 && get_variant_rev_count() > 0;
  }

  private int bucket_rov_tn (ReferenceOrVariant rov) {
    // bucket reads by reference/variant + tumor/normal
    int errors = 0;
    ArrayList<SNPTrackInfo> set = tracker.get(rov);
    if (set != null) {
      for (SNPTrackInfo i : set) {
	Strand str = Strand.valueOfSAMRecord(i.sr);
	if (i.tumor_normal == NORMAL_BYTE) {
	  add_set(rov, str, TumorNormal.NORMAL, 1);
	} else if (i.tumor_normal == TUMOR_BYTE) {
	  add_set(rov, str, TumorNormal.TUMOR, 1);
	} else {
	  errors++;
	}
      }
    }
    return errors;
  }

  public int get_unique_read_name_count (ReferenceOrVariant rov) {
    //
    // must call after analyze() to ensure ID pruning has been performed
    //
    ArrayList<SNPTrackInfo> list = tracker.get(rov);
    int count = 0;
    if (list != null) {
      HashSet<String> names = new HashSet<String>();
      for (SNPTrackInfo sti : list) {
	names.add(sti.sr.getReadName());
      }
      count = names.size();
    }
    return count;
  }


}