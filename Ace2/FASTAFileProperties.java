package Ace2;

import java.io.*;

public class FASTAFileProperties {

  private long fa_ptr_id;
  // file pointer for sequence ID line
  private long fa_ptr_sequence;
  // file pointer for start of sequence
  private long fa_cooked_line_nt;
  // number of nucleotides per line
  private long fa_raw_line_bytes;
  // number of file bytes per line
  private int line_overhead;

  private long sequence_length;
  // length (in nt) of sequence (assuming 1 sequence in file)

  public void detect_record_properties (RandomAccessFile raf) throws IOException {
    // given a RandomAccessFile pointed to start of a FASTA record (">" line),
    // set class variables for
    //
    // - file pointer of ID line
    // - file pointer of first sequence line
    // - number of nucleotides per line (cooked)
    // - number of file bytes per line (includes line break)
    //

    fa_ptr_id = raf.getFilePointer();
    String id_line = raf.readLine();
    if (id_line.indexOf(">") == 0) {
      fa_ptr_sequence = raf.getFilePointer();
      String first_sequence_line = raf.readLine();
      long ptr_second = raf.getFilePointer();
      fa_raw_line_bytes = ptr_second - fa_ptr_sequence;
      //      System.err.println("first="+first_sequence_line);  // debug
      fa_cooked_line_nt = first_sequence_line.length();
      //      System.err.println("nt/line="+fa_cooked_line_nt);  // debug
      //      System.err.println("bytes/line="+fa_raw_line_bytes);  // debug

      long flen = raf.length();
      //      System.err.println("flen="+flen);  // debug

      long body_size = flen - fa_ptr_sequence;
      //      System.err.println("body="+body_size);  // debug

      long lines = body_size / fa_raw_line_bytes;
      line_overhead = (int) (fa_raw_line_bytes - fa_cooked_line_nt);

      long leftover = body_size % fa_raw_line_bytes;
      if (leftover > 0) leftover -= line_overhead;
      // if partial line, make sure to account for line-terminator overhead

      sequence_length = (lines * fa_cooked_line_nt) + leftover;
    } else {
      System.err.println("ERROR: expected FASTA ID line, got " + id_line);  // debug
    }
  }

  public long get_sequence_length() {
    return sequence_length;
  }

  public long get_sequence_pointer () {
    return fa_ptr_sequence;
  }

  public long get_raw_bytes_per_line() {
    return fa_raw_line_bytes;
  }
  
  public int get_line_overhead() {
    return line_overhead;
  }

  public long get_nt_per_line() {
    return fa_cooked_line_nt;
  }



}