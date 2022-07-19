package com.agora.stats;

import com.agora.stats.common.Logger;
import com.agora.stats.events.EndEvent;
import com.agora.stats.events.FirstframerenderedEvent;
import com.agora.stats.events.PauseEvent;
import com.agora.stats.events.PlayEvent;
import com.agora.stats.events.PlaystateEvent;
import com.agora.stats.events.QualityMetric6sEvent;
import com.agora.stats.events.StuckEvent;
import com.google.android.exoplayer2.util.Log;
import java.util.ArrayList;

import com.agora.stats.events.IEvent;
import com.agora.stats.events.IEventProcessor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventStatusTracker implements IEventProcessor
{
  public static final String TAG = "EventStatusTracker";

  interface Callback{
    void onEventOutput(final IEvent event);
  }

  public class StatusContext {
    public int freezeTime200ms = 0;
    public int freezeTime500ms = 0;
    public int freezeTime600ms = 0;
    public int stuckCount = 0;
  }

  public enum PlayStatusCode {
    IDEL(0), PLAY(1), PAUSE(2), END(3);

    private final long value;

    PlayStatusCode(long value) {
      this.value = value;
    }

    public long getValue() {
      return value;
    }
  }

  public class PlayStatus {

    public long code = PlayStatusCode.IDEL.getValue();
    public long firstFrameElapse = 0;
    public long reportInterval = 0;
  }

  private static int STUCK_SCHEDULE_INTERVAL_TIME_MS = 6000;
  private static int PLAY_STATUS_SCHEDULE_INTERVAL_TIME_MS = 10000;

  private static long STUCK_THRESHOLD_1 = 200;
  private static long STUCK_THRESHOLD_2 = 500;
  private static long STUCK_THRESHOLD_3 = 600;

  private static ArrayList<String> needProcessEventFilter;

  static {
    EventStatusTracker.needProcessEventFilter = new ArrayList<String>();
    EventStatusTracker.needProcessEventFilter.add("FLSPlayerStuck");
    EventStatusTracker.needProcessEventFilter.add("FLSPlayerPlay");
    EventStatusTracker.needProcessEventFilter.add("FLSPlayerPause");
    EventStatusTracker.needProcessEventFilter.add("FLSPlayerEnd");
    EventStatusTracker.needProcessEventFilter.add("FLSPlayerFirstframerendered");
  }


  private ScheduledExecutorService scheduledService;
  private Callback callback = null;
  private StatusContext statusContext;
  private PlayStatus playStatus;

  public EventStatusTracker(Callback callback){

    this.statusContext = new StatusContext();
    this.playStatus = new PlayStatus();

    this.callback = callback;
    this.scheduledService = Executors.newScheduledThreadPool(2);
    this.scheduledService.scheduleWithFixedDelay( () -> this.scheduledService.execute( () -> this.stuckCycleReport()), STUCK_SCHEDULE_INTERVAL_TIME_MS, STUCK_SCHEDULE_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS);
    this.scheduledService.scheduleWithFixedDelay( () -> this.scheduledService.execute( () -> this.playStatusCycleReport()), STUCK_SCHEDULE_INTERVAL_TIME_MS, PLAY_STATUS_SCHEDULE_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS);
  }

  public void release(){
    if (this.scheduledService != null) {
      this.scheduledService.shutdown();
      this.scheduledService = null;
    }
  }


  /*********************** implement of IEventProcessor Start ***********************/


  @Override
  public void asyncHandle(final IEvent event){
  }

  @Override
  public void handle(final IEvent event){
    if(EventStatusTracker.needProcessEventFilter.contains(event.getType())){

      boolean needOutput = true;

      if(event instanceof StuckEvent){

        synchronized (this.statusContext){
          StuckEvent stuckEvent = (StuckEvent)event;
          long duration = stuckEvent.getDuration();
          if(duration < STUCK_THRESHOLD_1){
            this.statusContext.freezeTime200ms+=1;
          } else if(duration < STUCK_THRESHOLD_2){
            this.statusContext.freezeTime500ms+=1;
          } else if(duration < STUCK_THRESHOLD_3){
            this.statusContext.freezeTime600ms+=1;
          }
          this.statusContext.stuckCount+=1;
        }
      }
      else if(event instanceof PlayEvent){
        this.playStatus.code =PlayStatusCode.PLAY.getValue();
      }
      else if(event instanceof PauseEvent){
        this.playStatus.code =PlayStatusCode.PAUSE.getValue();
      }
      else if(event instanceof EndEvent){
        this.playStatus.code =PlayStatusCode.END.getValue();
      }
      else if(event instanceof FirstframerenderedEvent){
        this.playStatus.firstFrameElapse = ((FirstframerenderedEvent)(event)).getCostTime();
      }


      if(needOutput){
        this.callback.onEventOutput(event);
      }
    }
    else {

      if(null != this.callback){
        this.callback.onEventOutput(event);
      }
    }
  }
  /*********************** implement of IEventProcessor End ***********************/

  private void stuckCycleReport(){
    if(null != this.callback){
      QualityMetric6sEvent qualityMetric6sEvent = new QualityMetric6sEvent();

      synchronized (this.statusContext) {
        qualityMetric6sEvent.setFreezeTime200ms(this.statusContext.freezeTime200ms);
        qualityMetric6sEvent.setFreezeTime500ms(this.statusContext.freezeTime500ms);
        qualityMetric6sEvent.setFreezeTime600ms(this.statusContext.freezeTime600ms);
        qualityMetric6sEvent.setStuckCount(this.statusContext.stuckCount);
      }
      qualityMetric6sEvent.setStatsDuration(STUCK_SCHEDULE_INTERVAL_TIME_MS);

      this.callback.onEventOutput(qualityMetric6sEvent);
    }
  }

  private void playStatusCycleReport(){
    if(null != this.callback) {
      Logger.d(TAG, "playStatusCycleReport");

      PlaystateEvent playstateEvent = new PlaystateEvent();
      playstateEvent.setCode(this.playStatus.code);
      playstateEvent.setFirstFrameElase(this.playStatus.firstFrameElapse);
      playstateEvent.setReportInterval(PLAY_STATUS_SCHEDULE_INTERVAL_TIME_MS);

      this.callback.onEventOutput(playstateEvent);
    }
  }


}
