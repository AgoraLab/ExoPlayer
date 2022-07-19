package com.agora.stats;

import android.util.Log;
import com.agora.stats.common.ElapseTimer;
import com.agora.stats.common.Logger;
import com.agora.stats.common.UUID;
import com.agora.stats.events.BaseEvent;
import com.agora.stats.events.DestroyEvent;
import com.agora.stats.events.EndEvent;
import com.agora.stats.events.IEvent;
import com.agora.stats.events.IEventProcessor;
import com.agora.stats.events.InitializedEvent;
import com.agora.stats.events.PauseEvent;
import com.agora.stats.events.PlayEvent;
import com.agora.stats.events.SeekedEvent;
import com.agora.stats.events.SeekingEvent;
import com.agora.stats.events.StreamSwitchEvent;
import com.agora.stats.events.StuckEvent;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import java.lang.ref.WeakReference;

public abstract class ExoPlayerBaseStats implements IEventProcessor,   EventStatusTracker.Callback{

  public static final String TAG = "ExoPlayerBaseStats";

  public enum PlayerState {
    INIT, BUFFERING, REBUFFERING, READY, PAUSED, PLAYING, SEEKING, SEEKED, ERROR, ENDED
  }

  private CustomerConfigData customerConfigData;
  protected WeakReference<ExoPlayer> player;

  protected PlayerState state;
  protected boolean firstFrameEventSent = false;
  protected long firstBufferingTimestamp = 0;
  protected long firstPlayTimestamp = 0;

  protected boolean seekingInProgress;
  protected long seekStartTimestamp = 0;
  protected long seekCurrentPos = 0;
  protected long seekToPos = 0;

  protected long rebufferingStartTime = 0;

  protected String instanceUUID = "";
  protected String playerId = "";
  protected String streamId = "";
  protected String playUrl = "";


  protected EventStatusTracker eventStatusTracker;
  protected  EventSender eventSender;

  public static void setLogCallback(Logger.LoggerCallback callback){
    Logger.setLoggerCallback(callback);
  }

  public ExoPlayerBaseStats(ExoPlayer player, String playerName, CustomerConfigData customerConfigData){
    this.player = new WeakReference<>(player);
    this.customerConfigData = customerConfigData;
    this.state = PlayerState.INIT;

    this.instanceUUID = UUID.generateUUID();

    this.eventStatusTracker = new EventStatusTracker(this);
    this.eventSender = new EventSender(new NetworkServerImpl("exo_report"), customerConfigData);

    firstBufferingTimestamp = System.currentTimeMillis();
  }

  public void release(){
    if(null != this.eventStatusTracker){
      this.eventStatusTracker.release();
      this.eventStatusTracker = null;
    }

    if(null != this.eventSender){
      this.eventSender.release();
      this.eventSender = null;
    }
  }

  /*********************** implement of IEventProcessor ***********************/

  @Override
  public void asyncHandle(final IEvent event){}


  @Override
  public void handle(final IEvent event){
    this.eventStatusTracker.handle(event);
  }

  /*********************** implement of EventStatusTracker.Callback ***********************/
  @Override
  public void onEventOutput(final IEvent event){

    this.eventAppendFiled(event);

    Logger.d(TAG, "send event: " + event.getType());

    ElapseTimer elapseTimer = new ElapseTimer("event-handle-time");
    this.eventSender.asyncHandle(event);
    elapseTimer.close();
  }

  private void eventAppendFiled(final IEvent event){

    if(event instanceof BaseEvent){

      BaseEvent baseEvent = (BaseEvent) event;
      baseEvent.setTimestamp( System.currentTimeMillis() );
      baseEvent.setVid(this.customerConfigData.getVid());
      baseEvent.setToken(this.customerConfigData.getToken());
      baseEvent.setStartId(this.instanceUUID);

      if((event instanceof InitializedEvent) || (event instanceof DestroyEvent)){
        return;
      }

      baseEvent.setPlayerId(this.playerId);

      if(event instanceof StreamSwitchEvent){
        return;
      }

      baseEvent.setStreamId(this.streamId);
      baseEvent.setUrl(this.playUrl);

    }
  }


  protected void buffering() {
    if (state == PlayerState.REBUFFERING ||
        state == PlayerState.SEEKING ||
        state == PlayerState.SEEKED) {
      return;
    }

    if (state == PlayerState.PLAYING) {
      rebufferingStarted();
      return;
    }

    state = PlayerState.BUFFERING;
  }

  protected void ready() {
    if(state == PlayerState.REBUFFERING ||
        state == PlayerState.BUFFERING ||
        state == PlayerState.SEEKING
    ){

      if(state == PlayerState.SEEKING){
        seeked();
        return;
      }

      if (state == PlayerState.REBUFFERING) {
        rebufferingEnded();
      }

      state = PlayerState.READY;
    }
  }

  protected void pause() {
    if (state == PlayerState.SEEKING ||  state == PlayerState.SEEKED || state == PlayerState.REBUFFERING) {
      return;
    }

    state = PlayerState.PAUSED;
    PauseEvent pauseEvent = new PauseEvent();

    if(null != this.player.get()){
      pauseEvent.setPos(this.player.get().getCurrentPosition());
    }
    else {
      Logger.d(TAG, "player is null");
    }

    this.handle(pauseEvent);
  }


  protected void playing() {

    if (state == PlayerState.SEEKING) {
      return;
    }

    if (state == PlayerState.PAUSED ||
        state == PlayerState.READY ||
        state == PlayerState.SEEKED) {

      if(0 == firstPlayTimestamp){
        firstPlayTimestamp = System.currentTimeMillis();
      }

      PlayEvent playEvent = new PlayEvent();
      playEvent.setProtocol("hls");
      this.handle(playEvent);
    }

    state = PlayerState.PLAYING;
  }


  protected void seeking(long curPost, long seekPos) {

    state = PlayerState.SEEKING;
    seekingInProgress = true;
    seekStartTimestamp = System.currentTimeMillis();
    this.seekCurrentPos = curPost;
    this.seekToPos = seekPos;

    SeekingEvent seekingEvent = new SeekingEvent();
    seekingEvent.setCurrentPos(curPost);
    seekingEvent.setSeekPos(seekPos);
    this.handle(seekingEvent);

  }

  protected void seeked() {

    state = PlayerState.SEEKED;
    SeekedEvent seekedEvent = new SeekedEvent();
    seekedEvent.setCurrentPos(this.seekCurrentPos);
    seekedEvent.setSeekPos(this.seekToPos);
    seekedEvent.setDuration(System.currentTimeMillis() - this.seekStartTimestamp);
    this.handle(seekedEvent);

    this.seekingInProgress = false;
    this.seekStartTimestamp = 0;
    this.seekCurrentPos = 0;
    this.seekToPos = 0;
  }

  protected void ended() {

    EndEvent endEvent = new EndEvent();

    if(0 != firstPlayTimestamp){
      endEvent.setDuration( (int)((System.currentTimeMillis() - firstPlayTimestamp) / 1000) );
    }

    this.handle(endEvent);

    this.firstPlayTimestamp = 0;
    this.state = PlayerState.ENDED;
  }


  protected void rebufferingStarted() {
    this.state = PlayerState.REBUFFERING;
    this.rebufferingStartTime = System.currentTimeMillis();
  }

  protected void rebufferingEnded() {
    StuckEvent stuckEvent = new StuckEvent();
    stuckEvent.setDuration(System.currentTimeMillis() - this.rebufferingStartTime);
    this.handle(stuckEvent);
  }




}
