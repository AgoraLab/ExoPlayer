package com.agora.stats.events;

public class PlaystateEvent extends BaseEvent{
  public static final String TYPE_NAME = "PlaystateEvent";

  public static final String EVENT_CODE = "code";
  public static final String EVENT_FIRST_FRAME_ELASE = "firstFrameElapse";
  public static final String EVENT_REPORT_INTERVAL = "reportInterval";

  public static final int EVENT_ID = 9912;

  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};


  public void setCode(long value){
    this.dataStorage.put(EVENT_CODE, value);
  }
  public void setFirstFrameElase(long value){
    this.dataStorage.put(EVENT_FIRST_FRAME_ELASE, value);
  }
  public void setReportInterval(long value){
    this.dataStorage.put(EVENT_REPORT_INTERVAL, value);
  }

}
