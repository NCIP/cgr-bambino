package Ace2;

public class GermlineSomaticTestCase {
  String description;
  int ref_n_count, ref_t_count, alt_n_count, alt_t_count;

  public GermlineSomaticTestCase(String description,
				 int ref_n_count, 
				 int ref_t_count,
				 int alt_n_count,
				 int alt_t_count) {
    this.description = description;
    this.ref_n_count = ref_n_count;
    this.ref_t_count = ref_t_count;
    this.alt_n_count = alt_n_count;
    this.alt_t_count = alt_t_count;
  }

}