package Trace;

interface RandomAccessStream {
  void seek (int offset);
  int read (byte [] b);
  int readInt ();
  int readUnsignedByte ();
  void skipBytes (int num);
  byte readByte();
  String readString (int num_bytes);
  short readShort();
  long length();
}
