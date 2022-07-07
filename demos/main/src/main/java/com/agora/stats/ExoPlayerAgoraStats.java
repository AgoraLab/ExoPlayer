package com.agora.stats;


import android.util.Log;
import androidx.annotation.Nullable;
import com.agora.stats.common.Logger;
import com.agora.stats.common.UUID;
import com.agora.stats.common.Utils;
import com.agora.stats.events.BaseEvent;
import com.agora.stats.events.DestroyEvent;
import com.agora.stats.events.ErrorEvent;
import com.agora.stats.events.FirstframerenderedEvent;
import com.agora.stats.events.IEvent;
import com.agora.stats.events.InitializedEvent;
import com.agora.stats.events.StreamSwitchEvent;
import com.agora.stats.events.UrlRequestEvent;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.TracksInfo;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.hls.HlsManifest;

import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;

import com.google.android.exoplayer2.video.VideoSize;
import java.io.IOException;


public class ExoPlayerAgoraStats extends ExoPlayerBaseStats implements AnalyticsListener {

  public static final String TAG = "ExoPlayerAgoraStats";

  private static final String MP_VERSION = "exoplayer-2.17.1";
  private static final String PLUGIN_VERSION ="exo_agora_stats-0.0.1";


  public ExoPlayerAgoraStats(ExoPlayer player, String playerName, CustomerConfigData customerConfigData) {
    super(player, playerName, customerConfigData);

    player.addAnalyticsListener(this);

    this.handle(new InitializedEvent(MP_VERSION, PLUGIN_VERSION));
  }

  public void release(){

    player.get().removeAnalyticsListener(this);

    this.handle(new DestroyEvent());


    super.release();
  }


  /*********************** Implement AnalyticsListener interface *********************************/

  @Override
  public void onLoadStarted(AnalyticsListener.EventTime eventTime,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData) {
  }

  @Override
  public void onLoadCanceled(AnalyticsListener.EventTime eventTime,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData) {

    if (loadEventInfo.uri != null) {
      UrlRequestEvent urlRequestEvent = new UrlRequestEvent();

      urlRequestEvent.setCode(UrlRequestEvent.RequestResultCode.CANCEL.getValue());

      this.handle(urlRequestEvent);
    } else {
      Logger.d(TAG,
          "ERROR: onLoadCanceled called but mediaLoadData argument have no uri parameter.");
    }
  }

  @Override
  public void onLoadCompleted(AnalyticsListener.EventTime eventTime,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData) {


    if (loadEventInfo.uri != null) {

      UrlRequestEvent urlRequestEvent = new UrlRequestEvent();

      urlRequestEvent.setUrl(loadEventInfo.uri.toString());
      urlRequestEvent.setCode(UrlRequestEvent.RequestResultCode.SUCCESS.getValue());
      urlRequestEvent.setCostTime(loadEventInfo.loadDurationMs);
      urlRequestEvent.setDownloadBytes(loadEventInfo.bytesLoaded);

      this.handle(urlRequestEvent);

    } else {
      Logger.d(TAG,
          "ERROR: onLoadCompleted called but mediaLoadData argument have no uri parameter.");
    }
  }

  @Override
  public void onLoadError(AnalyticsListener.EventTime eventTime,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData, IOException e,
      boolean wasCanceled) {

    if(!wasCanceled){
      UrlRequestEvent urlRequestEvent = new UrlRequestEvent();
      urlRequestEvent.setCode(UrlRequestEvent.RequestResultCode.FAILD.getValue());
      urlRequestEvent.setReason(e.getMessage());

      this.handle(urlRequestEvent);
    }
  }


  @Override
  public void onIsLoadingChanged(AnalyticsListener.EventTime eventTime, boolean isLoading) {
    Logger.d(TAG, "onIsLoadingChanged： " + isLoading);
  }

  @Override
  public void onIsPlayingChanged(AnalyticsListener.EventTime eventTime, boolean isPlaying) {
    // playing 状态切换
    Logger.d(TAG, "onIsPlayingChanged： " + isPlaying);
    if(isPlaying){
      playing();
    }
    else{
      pause();
    }
  }

  @Override
  public void onPlaybackStateChanged(EventTime eventTime, @Player.State int state) {
    // 播放状态改变了
    Logger.d(TAG, "onPlaybackStateChanged： " + state);
    onPlaybackStateChanged(state);
  }

  public void onPlaybackStateChanged(int playbackState){

    boolean playWhenReady = player.get().getPlayWhenReady();
    PlayerState state = this.state;

    switch (playbackState) {

      case Player.STATE_IDLE:

        if (state == PlayerState.PLAY || state == PlayerState.PLAYING) {
          pause();
        }
        break;

      case Player.STATE_BUFFERING:
        buffering();
        break;

      case Player.STATE_READY:
        pause();
        break;

      case Player.STATE_ENDED:
        ended();
        break;

      default:
        // don't care
        break;
    }

  }


  @Override
  public void onPlayerError(AnalyticsListener.EventTime eventTime, PlaybackException error) {
    // 在这里处理所有的错误事件

    ErrorEvent errorEvent = new ErrorEvent();
    errorEvent.setErrorMsg(error.getMessage());
    this.handle(errorEvent);
  }

  @Override
  public void onPlayWhenReadyChanged(AnalyticsListener.EventTime eventTime, boolean playWhenReady,
      int reason) {
    // 是否在ready的时候自动播放，设置改变的时候会回调到这里
//    onPlayWhenReadyChanged(playWhenReady, reason);
//    onPlaybackStateChanged(player.get().getPlaybackState());
  }


  @Override
  public void onPositionDiscontinuity(
      EventTime eventTime,
      Player.PositionInfo oldPosition,
      Player.PositionInfo newPosition,
      @Player.DiscontinuityReason int reason) {
    // seeking 和 seeked 状态都在这里处理
    if(Player.DISCONTINUITY_REASON_SEEK == reason){
      Logger.d(TAG, "onPositionDiscontinuity, old pos: " + oldPosition.positionMs + " new pos:" + newPosition.positionMs);
      seeking(oldPosition.positionMs, newPosition.positionMs);
    }

  }



  @Override
  public void onTimelineChanged(AnalyticsListener.EventTime eventTime,@Player.TimelineChangeReason int reason) {
    // dont need process
    Logger.d(TAG, "onTimelineChanged: " + reason);
    Object object = this.player.get().getCurrentManifest();
    if(null != object && HlsManifest.class.isInstance(object)){
      HlsManifest hlsManifest = HlsManifest.class.cast(object);

      if(null != hlsManifest.mediaPlaylist.baseUri &&
          !hlsManifest.mediaPlaylist.baseUri.isEmpty() &&
          this.playUrl != hlsManifest.mediaPlaylist.baseUri){

        if(null != this.playUrl && !this.playUrl.isEmpty()){
          // url, streamId field of StreamSwitchEvent use old value
          StreamSwitchEvent streamSwitchEvent = new StreamSwitchEvent();
          streamSwitchEvent.setUrl(this.playUrl);
          streamSwitchEvent.setStreamId(this.streamId);
          streamSwitchEvent.setNewStreamId(Utils.MD5(hlsManifest.mediaPlaylist.baseUri));
          this.handle(streamSwitchEvent);
        }

        this.playUrl = hlsManifest.mediaPlaylist.baseUri;
        this.streamId = Utils.MD5(this.playUrl);
      }

      Logger.d(TAG, "onTimelineChanged, mediaPlaylist.baseUri:" + hlsManifest.mediaPlaylist.baseUri);
    }
  }


  @Override
  public void onMediaItemTransition(
      EventTime eventTime, @Nullable MediaItem mediaItem, int reason) {

    if(Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED == reason){
      Logger.d(TAG, "onMediaItemTransition: " + mediaItem.localConfiguration.uri);

      this.playerId = Utils.MD5(mediaItem.localConfiguration.uri.toString());
      this.playUrl = mediaItem.localConfiguration.uri.toString();
    }

  }

  @Override
  public void onTracksInfoChanged(EventTime eventTime, TracksInfo tracksInfo) {
    // 处理流切换
    Logger.d(TAG, "onTracksInfoChanged");
  }


  @Override
  public void onRenderedFirstFrame(EventTime eventTime, Object output, long renderTimeMs) {
    FirstframerenderedEvent firstframerenderedEvent = new FirstframerenderedEvent();
    firstframerenderedEvent.setCostTime(System.currentTimeMillis() - firstBufferingTimestamp);
    this.handle(firstframerenderedEvent);
  }


  @Override
  public void onAudioAttributesChanged(AnalyticsListener.EventTime eventTime,
      AudioAttributes audioAttributes) {
    // dont need process
  }

  @Override
  public void onAudioUnderrun(AnalyticsListener.EventTime eventTime, int bufferSize,
      long bufferSizeMs, long elapsedSinceLastFeedMs) {
    // dont need process
  }

  @Override
  public void onVideoInputFormatChanged(EventTime eventTime,
      Format format,
      @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
    // dont need process
  }

  @Override
  public void onDownstreamFormatChanged(AnalyticsListener.EventTime eventTime,
      MediaLoadData mediaLoadData) {
    // dont need process
  }

  @Override
  public void onDrmKeysLoaded(AnalyticsListener.EventTime eventTime) {
    // dont need process
  }

  @Override
  public void onDrmKeysRemoved(AnalyticsListener.EventTime eventTime) {
    // dont need process
  }

  @Override
  public void onDrmKeysRestored(AnalyticsListener.EventTime eventTime) {
    // dont need process
  }

  @Override
  public void onDrmSessionManagerError(AnalyticsListener.EventTime eventTime, Exception e) {
//    internalError(new MuxErrorException(ERROR_DRM, "DrmSessionManagerError - " + e.getMessage()));
    // 这里需要处理错误
    Logger.d(TAG, "onDrmSessionManagerError： " + e.getMessage());
  }

  @Override
  public void onMetadata(AnalyticsListener.EventTime eventTime, Metadata metadata) {
    // dont need process
    Logger.d(TAG, "onMetadata： " + metadata.toString());
  }

  @Override
  public void onPlaybackParametersChanged(AnalyticsListener.EventTime eventTime,
      PlaybackParameters playbackParameters) {
    // dont need process
  }

  @Override
  public void onTrackSelectionParametersChanged(
      EventTime eventTime, TrackSelectionParameters trackSelectionParameters) {
    Logger.d(TAG, "onTrackSelectionParametersChanged");
    // dont need process
  }

  @Override
  public void onUpstreamDiscarded(EventTime eventTime, MediaLoadData mediaLoadData) {
    // dont need process
  }

  @Override
  public void onVideoSizeChanged(EventTime eventTime, VideoSize videoSize) {
    // dont need process
  }

  @Override
  public void onVolumeChanged(AnalyticsListener.EventTime eventTime, float volume) {
    // dont need process
  }

  @Override
  public void onShuffleModeChanged(AnalyticsListener.EventTime eventTime,
      boolean shuffleModeEnabled) {
    // dont need process
  }

  @Override
  public void onSurfaceSizeChanged(AnalyticsListener.EventTime eventTime, int width,
      int height) {
    // dont need process
  }







}
