// .poly file data line
package Trace;

import java.util.StringTokenizer;

public class Poly {
  // G  1  4481.267327  44812.673267  A  15  30023.486736  66.982811  0.000000  0.000000  4481.267327  0.000000  4936.476256  4877.933967  43.089109  0.000000
  //  0 - called base
  //  1 - position of called base
  //  2 - area of called peak
  //  3 - relative area of called peak
  //  4 - uncalled base
  //  5 - position of uncalled base
  //  6 - area of uncalled peak
  //  7 - relative area of uncalled peak
  //  8 - amplitude of "a" trace at position of called base
  //  9 - amplitude of "c" trace at position of called base
  // 10 - amplitude of "g" trace at position of called base
  // 11 - amplitude of "t" trace at position of called base
  // NOTE: the following only appear in patched version of
  //       phred!
  // 12 - amplitude of "a" trace at position of uncalled base
  // 13 - amplitude of "c" trace at position of uncalled base
  // 14 - amplitude of "g" trace at position of uncalled base
  // 15 - amplitude of "t" trace at position of uncalled base

  public char called_base;
  public int called_pos;
  public float called_a, called_c, called_g, called_t;

  public float called_area, called_rel_area;
  public float uncalled_area, uncalled_rel_area;

  public char uncalled_base;
  public int uncalled_pos;
  public float uncalled_a, uncalled_c, uncalled_g, uncalled_t;

  private boolean extended;

  // DERIVED:
  public float called_amp, uncalled_amp;

  public Poly (String line) {
    // parse a data line
    called_base = uncalled_base = ' ';
    called_pos = uncalled_pos = 0;

    called_area = called_rel_area = (float) 0;
    called_a = called_c = called_g = called_t = (float) 0;
    uncalled_area = uncalled_rel_area = (float) 0;
    uncalled_a = uncalled_c = uncalled_g = uncalled_t = (float) 0;

    String token;
    
    StringTokenizer st = new StringTokenizer(line);
    
    called_base = st.nextToken().charAt(0);
    //  0 - called base

    called_pos = Integer.parseInt(st.nextToken());
    //  1 - position of called base

    called_area = (Float.valueOf(st.nextToken())).floatValue();
    //  2 - area of called peak

    called_rel_area = (Float.valueOf(st.nextToken())).floatValue();
    //  3 - relative area of called peak

    uncalled_base = st.nextToken().charAt(0);
    //  4 - uncalled base

    uncalled_pos = Integer.parseInt(st.nextToken());
    //  5 - position of uncalled base

    uncalled_area = (Float.valueOf(st.nextToken())).floatValue();
    //  6 - area of uncalled peak

    uncalled_rel_area = (Float.valueOf(st.nextToken())).floatValue();
    //  7 - relative area of uncalled peak

    called_a = (Float.valueOf(st.nextToken())).floatValue();
    //  8 - amplitude of "a" trace at position of called base
    called_c = (Float.valueOf(st.nextToken())).floatValue();
    //  9 - amplitude of "c" trace at position of called base
    called_g = (Float.valueOf(st.nextToken())).floatValue();
    //  10 - amplitude of "g" trace at position of called base
    called_t = (Float.valueOf(st.nextToken())).floatValue();
    //  11 - amplitude of "t" trace at position of called base

    if (st.hasMoreTokens()) {
      // NOTE: the following only appear in patched version of phred
      extended = true;
      uncalled_a = (Float.valueOf(st.nextToken())).floatValue();
      //  12 - amplitude of "a" trace at position of uncalled base
      uncalled_c = (Float.valueOf(st.nextToken())).floatValue();
      //  13 - amplitude of "c" trace at position of uncalled base
      uncalled_g = (Float.valueOf(st.nextToken())).floatValue();
      //  14 - amplitude of "g" trace at position of uncalled base
      uncalled_t = (Float.valueOf(st.nextToken())).floatValue();
      //  15 - amplitude of "t" trace at position of uncalled base
    } else {
      extended = false;
    }

    switch (called_base) {
    case 'A': called_amp = called_a; break;
    case 'C': called_amp = called_c; break;
    case 'G': called_amp = called_g; break;
    case 'T': called_amp = called_t; break;
    case 'N':
      if (called_a > 0 || called_c > 0 || called_g > 0 || called_t > 0) {
	System.out.println("wtf: called N!");
      }
      called_amp = -1;
      break;
    default:
      System.out.println("wtf: unknown base");
      called_amp = -1;
      break;
    }
  
    if (extended && uncalled_base != 'N') {
      switch (uncalled_base) {
      case 'A': uncalled_amp = uncalled_a; break;
      case 'C': uncalled_amp = uncalled_c; break;
      case 'G': uncalled_amp = uncalled_g; break;
      case 'T': uncalled_amp = uncalled_t; break;
      default:
	System.out.println("wtf: unknown uncalled base");
	uncalled_amp = -1;
	break;
      }
    } else {
      uncalled_amp = -1;
    }
  }

  public void reverse_complement (int num_samples) {
    float f_tmp;

    f_tmp = called_a;
    called_a = called_t;
    called_t = f_tmp;
    // swap A/T

    f_tmp = called_c;
    called_c = called_g;
    called_g = f_tmp;
    // swap C/G

    f_tmp = uncalled_a;
    uncalled_a = uncalled_t;
    uncalled_t = f_tmp;
    // swap A/T

    f_tmp = uncalled_c;
    uncalled_c = uncalled_g;
    uncalled_g = f_tmp;
    // swap C/G
    
    if (called_pos != -1) called_pos = num_samples - 1 - called_pos;
    if (uncalled_pos != -1) uncalled_pos = num_samples - 1 - uncalled_pos;

    switch (called_base) {
    case 'A': called_base = 'T'; break;
    case 'C': called_base = 'G'; break;
    case 'G': called_base = 'C'; break;
    case 'T': called_base = 'A'; break;
    }

    switch (uncalled_base) {
    case 'A': uncalled_base = 'T'; break;
    case 'C': uncalled_base = 'G'; break;
    case 'G': uncalled_base = 'C'; break;
    case 'T': uncalled_base = 'A'; break;
    }
  }

}
