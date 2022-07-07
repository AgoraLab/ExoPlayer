package com.agora.stats.common;
import java.net.URL;
import java.util.Hashtable;
import org.json.JSONObject;

public interface INetworkServer {

  public interface INetworkRequestsCompletion
  {
    void onComplete(final boolean success);
  }

  void shutdown();

  void get(final URL url, final INetworkRequestsCompletion callback);

  void post(final URL url, String body, Hashtable<String, String> headers, INetworkRequestsCompletion callback);

  void post(final URL url, byte[] bytesBody, Hashtable<String, String> headers, INetworkRequestsCompletion callback);
}
