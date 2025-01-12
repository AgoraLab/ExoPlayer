/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.extractor.ts;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.CeaUtil;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader.TrackIdGenerator;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.List;

/** Consumes SEI buffers, outputting contained CEA-608/708 messages to a {@link TrackOutput}. */
public final class SeiReader {


  private static final String TAG = "SeiReader";

  private static final int PAYLOAD_TYPE_USER_DATA_UNREGISTERED = 5;
  private static final int PAYLOAD_TYPE_AGORA_DEFINED_DATA = 100;

  private final List<Format> closedCaptionFormats;
  private final TrackOutput[] outputs;


  public interface SeiDataCallback {
    public void onSeiDataNotify(int type, ParsableByteArray userData, long pts);
  }

  /** @param closedCaptionFormats A list of formats for the closed caption channels to expose. */
  public SeiReader(List<Format> closedCaptionFormats) {
    this.closedCaptionFormats = closedCaptionFormats;
    outputs = new TrackOutput[closedCaptionFormats.size()];
  }

  public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
    for (int i = 0; i < outputs.length; i++) {
      idGenerator.generateNewId();
      TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_TEXT);
      Format channelFormat = closedCaptionFormats.get(i);
      @Nullable String channelMimeType = channelFormat.sampleMimeType;
      Assertions.checkArgument(
          MimeTypes.APPLICATION_CEA608.equals(channelMimeType)
              || MimeTypes.APPLICATION_CEA708.equals(channelMimeType),
          "Invalid closed caption mime type provided: " + channelMimeType);
      String formatId = channelFormat.id != null ? channelFormat.id : idGenerator.getFormatId();
      output.format(
          new Format.Builder()
              .setId(formatId)
              .setSampleMimeType(channelMimeType)
              .setSelectionFlags(channelFormat.selectionFlags)
              .setLanguage(channelFormat.language)
              .setAccessibilityChannel(channelFormat.accessibilityChannel)
              .setInitializationData(channelFormat.initializationData)
              .build());
      outputs[i] = output;
    }
  }

  public void consume(long pesTimeUs, ParsableByteArray seiBuffer){
    consume(pesTimeUs, seiBuffer, null);
  }

  /**
   *  consume the sei payload data, and return user_data_unregisted/agora_defined_data by callback
   * @param pesTimeUs
   * @param seiBuffer
   * @param seiDataCallback
   */
  public void consume(long pesTimeUs, ParsableByteArray seiBuffer, SeiDataCallback seiDataCallback) {

    CeaUtil.consume(pesTimeUs, seiBuffer, outputs, (int payloadType, int payloadSize, byte [] data, int position)->{

      if(null != seiDataCallback && (PAYLOAD_TYPE_USER_DATA_UNREGISTERED == payloadType ||
          PAYLOAD_TYPE_AGORA_DEFINED_DATA == payloadType)){

        ParsableByteArray seiData = new ParsableByteArray(payloadSize);
        System.arraycopy(data, position, seiData.getData(), 0, payloadSize);

        seiDataCallback.onSeiDataNotify(payloadType, seiData, pesTimeUs);
      }
    });

  }
}
