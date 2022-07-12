package com.google.android.exoplayer2;

import com.google.android.exoplayer2.util.ParsableByteArray;

/**
 * The SeiDataItem class use to wrap sei data
 */
public class SeiDataItem {

  public static final int SEI_DATA_TYPE_USER_DATA_UNREGISTED = 5;
  public static final int SEI_DATA_TYPE_AGORA_DEFINED_DATA = 100;

  private int seiDataType;
  private ParsableByteArray data;
  private long pts;


  public SeiDataItem(
      int seiDataType,
      ParsableByteArray data,
      long pts){

    this.seiDataType = seiDataType;
    this.data = data;
    this.pts = pts;
  }

  public int getSeiDataType() {return seiDataType;}
  public ParsableByteArray getData() {return data;}
  public long getPts() {return pts;}


}
