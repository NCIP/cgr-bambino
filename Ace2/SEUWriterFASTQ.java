package Ace2;

import net.sf.samtools.*;
import java.util.*;
import java.io.*;

public class SEUWriterFASTQ extends SEUWriter {
  //
  // write a single record to traditional hideous marked-up FASTQ output
  //
  MultiplexedWriter mw_paired_reads;
  UniqueReadName urn;
  WorkingFile wf_mate_unmapped;
  PrintStream ps_mate_unmapped;

  public SEUWriterFASTQ (SEUConfig config, SAMFileReader sfr, String base_fn) throws FileNotFoundException,IOException {
    super(config);
    mw_paired_reads = new MultiplexedWriter((base_fn + ".mate_mapped.fastq."), false);
    urn = new UniqueReadName();
    urn.set_inthash_mode(true, 19);

    wf_mate_unmapped = new WorkingFile(base_fn + ".mate_unmapped.fastq");
    ps_mate_unmapped = wf_mate_unmapped.getPrintStream();
  }

  public void writeAlignment (SAMRecord sr, SEUInteresting interesting) {
    String name = sr.getReadName();

    String id = name + "." + urn.get_suffix(name, sr.getReadNegativeStrandFlag());
    // wack; can't remember the reason for this

    ArrayList<String> stuff = new ArrayList<String>();

    if (sr.getReadUnmappedFlag() == true) {
      // unmapped read
      stuff.add("unmapped");

    } else {
      //
      // mapped read
      //
      stuff.add("mapped");
      stuff.add(SAMUtils.bucket_reference_name(sr.getReferenceName(), false));
      stuff.add(Integer.toString(sr.getAlignmentStart()));
      stuff.add(sr.getReadNegativeStrandFlag() ? "-" : "+");

      if (sr.getReadPairedFlag() && !sr.getMateUnmappedFlag()) {
	stuff.add("mate");
	stuff.add(SAMUtils.bucket_reference_name(sr.getMateReferenceName(), false));
	stuff.add(Integer.toString(sr.getMateAlignmentStart()));
      }
      
      if (interesting != null) stuff.addAll(interesting.get_fastq_tags());

      stuff.add("alignEnd");
      stuff.add(Integer.toString(sr.getAlignmentEnd()));
    }

    //
    //  mate pairing/mapping:
    //
    if (!sr.getReadPairedFlag()) {
      stuff.add("unpaired");
    } else if (sr.getMateUnmappedFlag()) {
      stuff.add("mate_unmapped");
      if (!sr.getReadUnmappedFlag()) stuff.add("has_unmapped_mate");
      // only appears for mapped reads w/unmapped mates
    } else {
      // mate is mapped
      stuff.add("has_mapped_mate");
      stuff.add(SAMUtils.bucket_reference_name(sr.getMateReferenceName(), false));
      stuff.add(Integer.toString(sr.getMateAlignmentStart()));
      stuff.add(sr.getMateNegativeStrandFlag() ? "-" : "+");
    }

    //
    // all reads:
    //
    stuff.add("flags");
    stuff.add(Integer.toString(sr.getFlags()));
    if (sr.getDuplicateReadFlag()) stuff.add("duplicate");
    // not sure if this ever happens for unmapped reads

    //    (new Exception("writing " + id + " " + Funk.Str.join(",", stuff))).printStackTrace();

    //
    //  write FASTQ:
    //
    if (sr.getReadUnmappedFlag() == true) {
      //
      // unmapped read
      //
      if (!sr.getReadPairedFlag() || sr.getMateUnmappedFlag()) {
	// read is unpaired and/or mate is not mapped
	SAMUtils.write_fastq(ps_mate_unmapped, sr, id, stuff);
      } else {
	// write unmapped reads w/mapped mates to bucket based on mate name
	String mate_refname = SAMUtils.bucket_reference_name(sr.getMateReferenceName(), true);
	SAMUtils.write_fastq(mw_paired_reads.getPrintStream(mate_refname), sr, id, stuff);
      }
    } else {
      String refname = SAMUtils.bucket_reference_name(sr.getReferenceName(), true);
      SAMUtils.write_fastq(
			   mw_paired_reads.getPrintStream(refname),
			   sr,
			   id,
			   stuff);
    }
  }

  public void close() {
    mw_paired_reads.finish();
    wf_mate_unmapped.finish();
  }

}
