package com.agora.stats;


import android.os.Build;
import androidx.annotation.RequiresApi;
import com.agora.stats.common.ElapseTimer;
import com.agora.stats.common.INetworkServer;
import com.agora.stats.common.Logger;
import com.agora.stats.events.IEvent;
import com.agora.stats.events.IEventProcessor;
import com.google.android.exoplayer2.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventSender implements IEventProcessor, INetworkServer.INetworkRequestsCompletion {

  public static final String TAG = "EventSender";

  public static final boolean IS_DEBUG = true;

  public static final String ASYNC_EXECUTE_THREAD_NAME = "async_send";

  public static final String EVENT_POST_URL_STAGING = "https://data-reporting.sh.agoralab.co/event";
  public static final String EVENT_POST_URL = "https://data-reporting.agora.io/event";



  public static final String HEADER_TOKEN_FIELD = "token";
  public static final String HEADER_APPID_FIELD = "appid";
  public static final String HEADER_BUSINESS_FIELD = "biz";
  public static final String HEADER_SEND_TIMESTAMP_FIELD = "sendts";
  public static final String HEADER_DEBUG_FIELD = "debug";
  public static final String HEADER_ENCODING_FIELD = "content-encoding";
  public static final String HEADER_TRACE_ID = "traceId";

  public static final String HEADER_BUSINESS_VALUE = "ad";
  public static final String HEADER_ENCODING_VALUE = "gzip";



  private static int MAX_QUEUE_SIZE = 3000;
  private static int SCHEDULE_INTERVAL_TIME_MS = 1000;
  private static long SEND_TIME_INTERVAL = 5000;
  private static int MAX_SEND_EVENT_COUNT_ONCE = 300;

  private static int EVENT_DES_HEADER_LEN = 5;

  private static int MAX_SEND_FAIL_COUNT = 10;

  private static ArrayList<String> immediateSendEvent;
  static {
    EventSender.immediateSendEvent = new ArrayList<String>();
    EventSender.immediateSendEvent.add("FLSPlayerEnd");
    EventSender.immediateSendEvent.add("FLSPlayerDestroy");
  }

  private INetworkServer networkServer;
  private ScheduledExecutorService scheduledService;
  private ExecutorService executorService;
  protected ArrayList<IEvent> eventQueue;
  protected ArrayList<IEvent> pendingEventsQueue;


  protected long lastSentTime = 0;
  protected int sendFailCount = 0;
  private boolean canSendRequest = true;
  private CustomerConfigData customerConfigData;

  private ElapseTimer networkElapseTimer;


  EventSender(INetworkServer networkServer, CustomerConfigData customerConfigData){

    this.eventQueue = new ArrayList<IEvent>();
    this.pendingEventsQueue = new ArrayList<IEvent>();

    this.networkServer = networkServer;
    this.customerConfigData = customerConfigData;
    this.scheduledService = Executors.newScheduledThreadPool(2);
    this.scheduledService.scheduleWithFixedDelay( () -> this.scheduledService.execute( () -> {
      ElapseTimer elapseTimer = new ElapseTimer("echedule-process-event");
      this.enqueueEvent(null);
      elapseTimer.close();
    }), 0L, SCHEDULE_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS);

    this.executorService = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, ASYNC_EXECUTE_THREAD_NAME));

  }

  public void release() {

    if(null != this.executorService && !this.executorService.isShutdown()) {

      Runnable releaseRunnable = new Runnable() {
        @Override
        public void run() {
          if (null != executorService) {
            executorService.shutdown();
            executorService = null;
          }

          if (null != scheduledService) {
            scheduledService.shutdown();
            scheduledService = null;
          }
          if (null != networkServer) {
            networkServer.shutdown();
            networkServer = null;
          }
        }
      };

      this.executorService.execute(releaseRunnable);
    }
  }


  @Override
  public void asyncHandle(final IEvent event) {


    if(null != this.executorService && !this.executorService.isShutdown()){

      Runnable asyncRunable = new Runnable() {
        @Override
        public void run() {
          ElapseTimer elapseTimer = new ElapseTimer("async-process-event");
          handle(event);
          elapseTimer.close();
        }
      };

      this.executorService.execute(asyncRunable);
    }
    else {
      Logger.d(TAG, "executorService maybe shutdown: " + this.executorService);
    }

  }

  @Override
  public void handle(final IEvent event){

    Logger.d(TAG, "handle event: " + event.getType() + " content:" + event.toString() );
    if(enqueueEvent(event)){

    }
    else{

      Logger.d(TAG, "enqueueEvent fail, event: " + event.getType());
    }

    if(EventSender.immediateSendEvent.contains(event.getType())){
      this.flush();
    }
  }

  private void flush(){
    Logger.d(TAG, "flush");
    this.processEventQueue(true);
  }

  @Override
  public synchronized void onComplete(boolean success) {

    this.networkElapseTimer.close();

    this.canSendRequest = true;

    if (success){
      this.sendFailCount = 0;

      Logger.d(TAG, "network request success");
    }
    else {
      if(this.eventQueue.size() + this.pendingEventsQueue.size() < MAX_QUEUE_SIZE && this.sendFailCount < MAX_SEND_FAIL_COUNT){
        this.eventQueue.addAll(0, this.pendingEventsQueue);
        this.sendFailCount ++;
      }
      else{ // 如果队列数量超过队列限制大小或者超过发送失败次数，则丢弃掉数据，不再重发
        this.sendFailCount = 0;
      }

      Logger.d(TAG, "network request fail");
    }

    this.pendingEventsQueue.clear();
  }

  // 事件入队
  private synchronized boolean enqueueEvent(final IEvent event) {

    if(this.eventQueue.size() < EventSender.MAX_QUEUE_SIZE){

      // 入队
      if(null != event){
        this.eventQueue.add(event);
      }

      if(this.needProcess()){
        this.processEventQueue(false);
        this.recordLastSendTime();
      }

      return this.eventQueue.size() <= EventSender.MAX_QUEUE_SIZE;
    }

    return false;
  }


  private synchronized void processEventQueue(final boolean flushAllEvent) {
    // 发送数据后， 转到padding 队列上
    int eventCount = (!flushAllEvent && this.eventQueue.size() > MAX_SEND_EVENT_COUNT_ONCE) ? MAX_SEND_EVENT_COUNT_ONCE : this.eventQueue.size();

    if(0 == eventCount){
      return;
    }

    if((this.canSendRequest || flushAllEvent) && null != this.networkServer){
      try{
        this.sendEvent(eventCount);

        this.canSendRequest = false;
      }
      catch (Throwable throwable){
        this.canSendRequest = true;
      }
    }

  }

  private void sendEvent(int count) throws IOException {

    Logger.d(TAG, "send event, count:" +count);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    ElapseTimer elapseTimer = new ElapseTimer("combine-the-events");

    for(int index = 0; index < count && !this.eventQueue.isEmpty(); ++index){
      IEvent event = this.eventQueue.remove(0);

      this.pendingEventsQueue.add(event);

      byte[] sendBytes = event.toString().getBytes();
      int jsonStrLen = sendBytes.length;
      int eventId = event.getEventId();

      byte[] eventDesHeader = new byte[EVENT_DES_HEADER_LEN];
      int indexIndicator = -1;
      eventDesHeader[++indexIndicator] = (byte) (0xff & jsonStrLen);
      eventDesHeader[++indexIndicator] = (byte) ((0xff00 & jsonStrLen) >> 8);
      eventDesHeader[++indexIndicator] = (byte) (0xff & eventId);
      eventDesHeader[++indexIndicator] = (byte) ((0xff00 & eventId) >> 8);
      eventDesHeader[++indexIndicator] = (byte) ((0xff0000 & eventId) >> 16);

      assert(indexIndicator <= EVENT_DES_HEADER_LEN);


      outputStream.write(eventDesHeader, 0, EVENT_DES_HEADER_LEN);
      outputStream.write(sendBytes, 0, sendBytes.length);
    }

    Hashtable<String, String> headerTable = new Hashtable<String, String>();
    headerTable.put(HEADER_TOKEN_FIELD, this.customerConfigData.getEnvironmentToken());
    headerTable.put(HEADER_APPID_FIELD, "");
    headerTable.put(HEADER_BUSINESS_FIELD, HEADER_BUSINESS_VALUE);
    headerTable.put(HEADER_SEND_TIMESTAMP_FIELD, String.valueOf(System.currentTimeMillis()/1000));
    headerTable.put(HEADER_DEBUG_FIELD, IS_DEBUG ? "true" : "false");
    headerTable.put(HEADER_ENCODING_FIELD, HEADER_ENCODING_VALUE);


    elapseTimer.close();

    this.networkElapseTimer = new ElapseTimer("network-send-time");
    this.networkServer
        .post(new URL(IS_DEBUG ? EVENT_POST_URL_STAGING : EVENT_POST_URL), outputStream.toByteArray(),  headerTable, this);
  }

  private boolean needProcess(){
    if(System.currentTimeMillis() - this.lastSentTime > this.getSendTimeInterval()){
      return true;
    }
    return false;
  }


  private void recordLastSendTime(){
    this.lastSentTime = System.currentTimeMillis();
  }


  protected long getSendTimeInterval() {
    if (this.sendFailCount == 0) {
      return SEND_TIME_INTERVAL;
    }
    return (long)((Math.pow(2.0, this.sendFailCount - 1) * Math.random() + 1.0) * (float)(SEND_TIME_INTERVAL));
  }

}
