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

    if(null != player && null != player.get()){
      player.get().removeAnalyticsListener(this);
    }

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
    Logger.d(TAG, "onPlaybackStateChanged： " + state);
    onPlaybackStateChanged(state);
  }

  public void onPlaybackStateChanged(int playbackState){

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
    Logger.d(TAG, "onPlayerError: " + error.getMessage());

    ErrorEvent errorEvent = new ErrorEvent();
    errorEvent.setErrorMsg(error.getMessage());
    this.handle(errorEvent);
  }

  @Override
  public void onPlayWhenReadyChanged(AnalyticsListener.EventTime eventTime, boolean playWhenReady,
      int reason) {
    Logger.d(TAG, "onPlayWhenReadyChanged: " + reason);
  }


  @Override
  public void onPositionDiscontinuity(
      EventTime eventTime,
      Player.PositionInfo oldPosition,
      Player.PositionInfo newPosition,
      @Player.DiscontinuityReason int reason) {
    Logger.d(TAG, "onPositionDiscontinuity: " + reason);

    if(Player.DISCONTINUITY_REASON_SEEK == reason){
      Logger.d(TAG, "onPositionDiscontinuity, old pos: " + oldPosition.positionMs + " new pos:" + newPosition.positionMs);
      seeking(oldPosition.positionMs, newPosition.positionMs);
    }

  }


  @Override
  public void onTimelineChanged(AnalyticsListener.EventTime eventTime,@Player.TimelineChangeReason int reason) {
    Logger.d(TAG, "onTimelineChanged: " + reason);

    Object object = null;

    if(null != player && null != player.get()){
      object= this.player.get().getCurrentManifest();
    }

    if(null != object && HlsManifest.class.isInstance(object)){

      HlsManifest hlsManifest = HlsManifest.class.cast(object);

      if(null != hlsManifest.mediaPlaylist.baseUri &&
          !hlsManifest.mediaPlaylist.baseUri.isEmpty() &&
          this.playUrl != hlsManifest.mediaPlaylist.baseUri){

        if(null != this.playUrl && !this.playUrl.isEmpty()){
          // streamId field of StreamSwitchEvent use old value
          StreamSwitchEvent streamSwitchEvent = new StreamSwitchEvent();
          streamSwitchEvent.setStreamId(this.streamId);
          streamSwitchEvent.setNewStreamId(Utils.MD5(hlsManifest.mediaPlaylist.baseUri));
          streamSwitchEvent.setUrl(hlsManifest.mediaPlaylist.baseUri);
          this.handle(streamSwitchEvent);
        }
      }

      this.playUrl = hlsManifest.mediaPlaylist.baseUri;
      this.streamId = Utils.MD5(this.playUrl);

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
    Logger.d(TAG, "onTracksInfoChanged");
  }


  @Override
  public void onRenderedFirstFrame(EventTime eventTime, Object output, long renderTimeMs) {
    Logger.d(TAG, "onRenderedFirstFrame");
    FirstframerenderedEvent firstframerenderedEvent = new FirstframerenderedEvent();
    firstframerenderedEvent.setCostTime(System.currentTimeMillis() - firstBufferingTimestamp);
    this.handle(firstframerenderedEvent);
  }


  @Override
  public void onAudioAttributesChanged(AnalyticsListener.EventTime eventTime,
      AudioAttributes audioAttributes) {
    Logger.d(TAG, "onAudioAttributesChanged");
  }

  @Override
  public void onAudioUnderrun(AnalyticsListener.EventTime eventTime, int bufferSize,
      long bufferSizeMs, long elapsedSinceLastFeedMs) {
    Logger.d(TAG, "onAudioUnderrun");
  }

  @Override
  public void onVideoInputFormatChanged(EventTime eventTime,
      Format format,
      @Nullable DecoderReuseEvaluation decoderReuseEvaluation) {
    Logger.d(TAG, "onVideoInputFormatChanged");
  }

  @Override
  public void onDownstreamFormatChanged(AnalyticsListener.EventTime eventTime,
      MediaLoadData mediaLoadData) {
    Logger.d(TAG, "onDownstreamFormatChanged");
  }

  @Override
  public void onDrmKeysLoaded(AnalyticsListener.EventTime eventTime) {
    Logger.d(TAG, "onDrmKeysLoaded");
  }

  @Override
  public void onDrmKeysRemoved(AnalyticsListener.EventTime eventTime) {
    Logger.d(TAG, "onDrmKeysRemoved");
  }

  @Override
  public void onDrmKeysRestored(AnalyticsListener.EventTime eventTime) {
    Logger.d(TAG, "onDrmKeysRestored");
  }

  @Override
  public void onDrmSessionManagerError(AnalyticsListener.EventTime eventTime, Exception e) {
    Logger.d(TAG, "onDrmSessionManagerError： " + e.getMessage());
    ErrorEvent errorEvent = new ErrorEvent();
    errorEvent.setErrorMsg(e.getMessage());
    this.handle(errorEvent);
  }

  @Override
  public void onMetadata(AnalyticsListener.EventTime eventTime, Metadata metadata) {
    Logger.d(TAG, "onMetadata： " + metadata.toString());
  }

  @Override
  public void onPlaybackParametersChanged(AnalyticsListener.EventTime eventTime,
      PlaybackParameters playbackParameters) {
    Logger.d(TAG, "onPlaybackParametersChanged");
  }

  @Override
  public void onTrackSelectionParametersChanged(
      EventTime eventTime, TrackSelectionParameters trackSelectionParameters) {
    Logger.d(TAG, "onTrackSelectionParametersChanged");
  }

  @Override
  public void onUpstreamDiscarded(EventTime eventTime, MediaLoadData mediaLoadData) {
    Logger.d(TAG, "onUpstreamDiscarded");
  }

  @Override
  public void onVideoSizeChanged(EventTime eventTime, VideoSize videoSize) {
    Logger.d(TAG, "onVideoSizeChanged");
  }

  @Override
  public void onVolumeChanged(AnalyticsListener.EventTime eventTime, float volume) {
    Logger.d(TAG, "onVolumeChanged");
  }

  @Override
  public void onShuffleModeChanged(AnalyticsListener.EventTime eventTime,
      boolean shuffleModeEnabled) {
    Logger.d(TAG, "onShuffleModeChanged");
  }

  @Override
  public void onSurfaceSizeChanged(AnalyticsListener.EventTime eventTime, int width,
      int height) {
    Logger.d(TAG, "onSurfaceSizeChanged");
  }







}
