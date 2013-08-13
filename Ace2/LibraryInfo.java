package Ace2;

public class LibraryInfo {
  String name, organism, strain;

  public String name_plus_strain () {
    if (! strain.equals(" ")) {
      return name + " (" + strain + ")";
    } else {
      return name;
    }
  }
}
