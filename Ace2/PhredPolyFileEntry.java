package Ace2;

public class PhredPolyFileEntry {
  static final int I_CALLED_BASE = 0;
  static final int I_CALLED_BASE_POSITION = 1;
  static final int I_CALLED_BASE_AREA = 2;
  static final int I_CALLED_BASE_RELATIVE_AREA = 3;

  static final int I_UNCALLED_BASE = 4;
  static final int I_UNCALLED_BASE_POSITION = 5;
  static final int I_UNCALLED_BASE_AREA = 6;
  static final int I_UNCALLED_BASE_RELATIVE_AREA = 7;

  static final int I_CALLED_BASE_AMP_A = 8;
  static final int I_CALLED_BASE_AMP_C = 9;
  static final int I_CALLED_BASE_AMP_G = 10;
  static final int I_CALLED_BASE_AMP_T = 11;
  // amplitudes at site of called base

  static final int I_UNCALLED_BASE_AMP_A = 12;
  static final int I_UNCALLED_BASE_AMP_C = 13;
  static final int I_UNCALLED_BASE_AMP_G = 14;
  static final int I_UNCALLED_BASE_AMP_T = 15;
  // amplitudes at site of uncalled base (*patched* version of phred only)

  char called_base;
  int called_base_position;
  float called_base_area, called_base_relative_area;

  char uncalled_base;
  int uncalled_base_position;
  float uncalled_base_area, uncalled_base_relative_area;

  float called_base_amp_a, called_base_amp_c, called_base_amp_g, called_base_amp_t; 
  float uncalled_base_amp_a, uncalled_base_amp_c, uncalled_base_amp_g, uncalled_base_amp_t; 
  // patched version of phred only!

  boolean is_patched_phred = false;


  public PhredPolyFileEntry (String line) {
    //    System.err.println("line="+line);  // debug
    String[] fields = line.split("\\s+");

    is_patched_phred = fields.length == 16;

    called_base = fields[I_CALLED_BASE].charAt(0);
    called_base_position = Integer.parseInt(fields[I_CALLED_BASE_POSITION]);
    called_base_area = parse_float(fields[I_CALLED_BASE_AREA]);
    called_base_relative_area = parse_float(fields[I_CALLED_BASE_RELATIVE_AREA]);

    uncalled_base = fields[I_UNCALLED_BASE].charAt(0);
    uncalled_base_position = Integer.parseInt(fields[I_UNCALLED_BASE_POSITION]);
    uncalled_base_area = parse_float(fields[I_UNCALLED_BASE_AREA]);
    uncalled_base_relative_area = parse_float(fields[I_UNCALLED_BASE_RELATIVE_AREA]);

    called_base_amp_a = parse_float(fields[I_CALLED_BASE_AMP_A]);
    called_base_amp_c = parse_float(fields[I_CALLED_BASE_AMP_C]);
    called_base_amp_g = parse_float(fields[I_CALLED_BASE_AMP_G]);
    called_base_amp_t = parse_float(fields[I_CALLED_BASE_AMP_T]);

    if (is_patched_phred) {
      uncalled_base_amp_a = parse_float(fields[I_UNCALLED_BASE_AMP_A]);
      uncalled_base_amp_c = parse_float(fields[I_UNCALLED_BASE_AMP_C]);
      uncalled_base_amp_g = parse_float(fields[I_UNCALLED_BASE_AMP_G]);
      uncalled_base_amp_t = parse_float(fields[I_UNCALLED_BASE_AMP_T]);
    } else {
      // hacky substitute
      uncalled_base_amp_a = called_base_amp_a;
      uncalled_base_amp_c = called_base_amp_c;
      uncalled_base_amp_g = called_base_amp_g;
      uncalled_base_amp_t = called_base_amp_t;
    }
  }

  private float parse_float (String s) {
    float result;
    if (s.equals("nan")) {
      result = 0;
    } else {
      result = Float.parseFloat(s);
    }
    return result;
  }

  public void complement() {
    called_base = complement_base(called_base);
    uncalled_base = complement_base(uncalled_base);
    float tmp;
    tmp = called_base_amp_a; called_base_amp_a = called_base_amp_t; called_base_amp_t = tmp;
    tmp = called_base_amp_c; called_base_amp_c = called_base_amp_g; called_base_amp_g = tmp;
    tmp = uncalled_base_amp_a; uncalled_base_amp_a = uncalled_base_amp_t; uncalled_base_amp_t = tmp;
    tmp = uncalled_base_amp_c; uncalled_base_amp_c = uncalled_base_amp_g; uncalled_base_amp_g = tmp;
  }

  private char complement_base (char b) {
    char c;
    switch (b) {
    case 'a':
      c = 't'; break;
    case 'A':
      c = 'T'; break;
    case 'c':
      c = 'g'; break;
    case 'C':
      c = 'G'; break;
    case 'g':
      c = 'c'; break;
    case 'G':
      c = 'C'; break;
    case 't':
      c = 'a'; break;
    case 'T':
      c = 'A'; break;
    case '*':
      c = '*'; break;
    case 'n':
      c = 'n'; break;
    case 'N':
      c = 'N'; break;
    default:
      c = b;
      System.out.println("reverse: warning, don't know how to rc " + b);
      break;
    }
    return c;
  }

  public float get_called_base_amplitude() {
    float amp = 0;
    if (called_base == 'A') {
      amp = called_base_amp_a;
    } else if (called_base == 'C') {
      amp = called_base_amp_c;
    } else if (called_base == 'G') {
      amp = called_base_amp_g;
    } else if (called_base == 'T') {
      amp = called_base_amp_t;
    } else {
      System.err.println("ERROR: unhandled called base " + called_base);  // debug
    }
    return amp;
  }

  public float get_uncalled_base_amplitude() {
    float amp = 0;
    if (uncalled_base == 'A') {
      amp = uncalled_base_amp_a;
    } else if (uncalled_base == 'C') {
      amp = uncalled_base_amp_c;
    } else if (uncalled_base == 'G') {
      amp = uncalled_base_amp_g;
    } else if (uncalled_base == 'T') {
      amp = uncalled_base_amp_t;
    } else {
      System.err.println("ERROR: unhandled uncalled base " + uncalled_base);  // debug
    }
    return amp;
  }

  
  
}
