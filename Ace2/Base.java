package Ace2;
import static Ace2.Constants.ALIGNMENT_GAP_CHAR;
import static Ace2.Constants.ALIGNMENT_DELETION_CHAR;

public enum Base {
  BASE_A, BASE_C, BASE_G, BASE_T, BASE_GAP, BASE_DELETION, BASE_UNKNOWN;

  public char charValue() {
    char result;
    switch (this) {
    case BASE_A: result = 'A'; break;
    case BASE_C: result = 'C'; break;
    case BASE_G: result = 'G'; break;
    case BASE_T: result = 'T'; break;
    case BASE_GAP: result = ALIGNMENT_GAP_CHAR; break;
    case BASE_DELETION: result = ALIGNMENT_DELETION_CHAR; break;
    default: result = '?';
    }
    return result;
  }

  public static Base valueOf (char c) {
    Base result=null;
    switch (c) {
    case 'a': case 'A': result = BASE_A; break;
    case 'c': case 'C': result = BASE_C; break;
    case 'g': case 'G': result = BASE_G; break;
    case 't': case 'T': result = BASE_T; break;
    case ALIGNMENT_GAP_CHAR: result = BASE_GAP; break;
    case ALIGNMENT_DELETION_CHAR: result = BASE_DELETION; break;
    default: result = BASE_UNKNOWN; break;
    }
    return result;
  }

  public String toString() {
    return Character.toString(charValue());
  }

  public boolean equals(char c) {
    return equals(Base.valueOf(c));
  }


}

