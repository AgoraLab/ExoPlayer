package com.agora.stats.events;
import com.agora.stats.events.BaseEvent;
public class QualityMetric6sEvent extends BaseEvent{
  public static final String TYPE_NAME = "FLSPlayerQualityMetric6s";

  public static final String EVENT_200MS_FREEZE_TIME = "FreezeTime200ms";
  public static final String EVENT_500MS_FREEZE_TIME = "FreezeTime500ms";
  public static final String EVENT_600MS_FREEZE_TIME = "FreezeTime600ms";
  public static final String EVENT_STUCK_COUNT = "stuckCount";
  public static final String EVENT_STATS_DURATION = "statsDuration";

  public static final int EVENT_ID = 9897;

  @Override
  public int getEventId(){return EVENT_ID;};

  @Override
  public String getType(){return TYPE_NAME;};


  public void setFreezeTime200ms(int value){
    this.dataStorage.put(EVENT_200MS_FREEZE_TIME, value);
  }

  public void setFreezeTime500ms(int value){
    this.dataStorage.put(EVENT_500MS_FREEZE_TIME, value);
  }

  public void setFreezeTime600ms(int value){
    this.dataStorage.put(EVENT_600MS_FREEZE_TIME, value);
  }

  public void setStuckCount(int value){
    this.dataStorage.put(EVENT_STUCK_COUNT, value);
  }

  public void setStatsDuration(long value){
    this.dataStorage.put(EVENT_STATS_DURATION, value);
  }
}
