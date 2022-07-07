package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
public class UrlRequestEvent extends  BaseEvent{

  public enum RequestResultCode {

    SUCCESS(0),
    CANCEL(1),
    FAILD(-1);

    private final int value;

    RequestResultCode(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static final String TYPE_NAME = "FLSPlayerUrlrequest";

  public static final String EVENT_CODE = "code";
  public static final String EVENT_REASON = "reason";
  public static final String EVENT_COST_TIME = "dlCosttime";
  public static final String EVENT_DOWNLOAD_BYTES = "dlbytes";

  public static final int EVENT_ID = 9893;


  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};

  public void setCode(final long value){

    this.dataStorage.put(EVENT_CODE, value);

  }

  public void setReason(final String value){
    if(null != value){
      this.dataStorage.put(EVENT_REASON, value);
    }
  }

  public void setCostTime(final long value){

    this.dataStorage.put(EVENT_COST_TIME, value);

  }

  public void setDownloadBytes(final long value){

    this.dataStorage.put(EVENT_DOWNLOAD_BYTES, value);

  }
}
