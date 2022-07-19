package com.agora.stats;
import com.agora.stats.common.INetworkServer;
import com.agora.stats.common.Logger;
import com.google.android.exoplayer2.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;


public class NetworkServerImpl implements INetworkServer {

  public static final String TAG = "NetworkServerImpl";

  private class NetworkTaskRunnable implements Runnable {

    private static final int IO_READ_TIMEOUT_MS = 20 * 1000;

    private static final int NET_CONNECT_TIMEOUT_MS = 30 * 1000;

    private static final int MAXIMUM_RETRY_COUNT = 4;

    private static final int BASE_TIME_BETWEEN_BEACONS = 5000;


    private NetworkRequest networkRequest;

    private INetworkServer.INetworkRequestsCompletion networkRequestsCompletion;

    private int failureCount = 0;


    NetworkTaskRunnable(NetworkRequest networkRequest,
        INetworkServer.INetworkRequestsCompletion completion) {
      this.networkRequest = networkRequest;
      this.networkRequestsCompletion = completion;
    }

    @Override
    public void run() {

      URL url = this.networkRequest.getUrl();
      String method = this.networkRequest.getMethod();
      Hashtable<String, String> headers = this.networkRequest.getHeaders();
      String body = this.networkRequest.getBody();
      byte[] bytesBody = this.networkRequest.getBytesBody();

      boolean successful = false;
      while (!needExit && !successful && failureCount < MAXIMUM_RETRY_COUNT) {
        try {
          Thread.sleep(getNextBeaconTime());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        successful = executeHttp(url, method, headers, body, bytesBody);
      }
      if (this.networkRequestsCompletion != null) {
        this.networkRequestsCompletion.onComplete(successful);
      }

      Logger.d(TAG, "Task runable completed, exit: " + needExit);

    }

    private long getNextBeaconTime() {
      if (failureCount == 0) {
        return 0;
      }
      double factor = Math.pow(2, failureCount - 1);
      factor = factor * Math.random();
      return (long) (1 + factor) * BASE_TIME_BETWEEN_BEACONS;
    }

    private boolean executeHttp(URL url, String method, Hashtable<String, String> headers,
        String body, byte[] bytesBody) {
      HttpURLConnection conn = null;
      InputStream stream = null;
      boolean successful = true;

      try {
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(IO_READ_TIMEOUT_MS);
        conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MS);
        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);


        Enumeration<String> headerKeys = headers.keys();
        boolean shouldGzip = false;
        while (headerKeys.hasMoreElements()) {
          String key = headerKeys.nextElement();
          String value = headers.get(key);
          conn.setRequestProperty(key, value);
          if (key.equalsIgnoreCase("Content-Encoding")
              && value.equalsIgnoreCase("gzip")) {
            shouldGzip = true;
          }
        }

        if (method.equals("POST")) {

          byte[] bytes;
          if(null != body && !body.isEmpty()){
            conn.setRequestProperty("Content-Type", "application/json");
             bytes = body.getBytes();
          } else {
            bytes = bytesBody;
          }

          if (shouldGzip) {
            bytes = gzip(bytes);
          }
          conn.connect();

          OutputStream outputStream = conn.getOutputStream();
          outputStream.write(bytes);
          outputStream.close();
        }

        Logger.d(TAG, "network response code: " + conn.getResponseCode());
        stream = conn.getInputStream();

      } catch (Exception e) {

        successful = false;
        failureCount++;
      } finally {
        if (stream != null) {
          try {
            stream.close();
          } catch (IOException ioe) {
            successful = false;
            failureCount++;
          }
        }
        if (conn != null) {
          conn.disconnect();
        }
      }
      return successful;
    }


    private byte[] gzip(byte[] input) throws Exception {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
      gzipOutputStream.write(input);
      gzipOutputStream.flush();
      gzipOutputStream.close();
      return byteArrayOutputStream.toByteArray();
    }
  }


  private interface NetworkRequest {

    URL getUrl();

    String getMethod();

    String getBody();

    byte[] getBytesBody();

    Hashtable<String, String> getHeaders();
  }


  private static class PostRequest implements NetworkRequest {

    /**
     * URL to send request to.
     */
    private final URL url;
    /**
     * POST method body to send with this request.
     */
    private final String body;
    private final byte[] bytesBody;
    /**
     * HTTP headers to send with this POST request.
     */
    private final Hashtable<String, String> headers;

    /**
     * Basic constructor with body.
     *
     * @param url  to send request to.
     * @param body payload to send with this request.
     */
    PostRequest(URL url, String body) {
      this(url, body, null, new Hashtable<String, String>());
    }

    /**
     * Constructor with additional HTTP headers.
     *
     * @param url     to send request to.
     * @param body    payload to send with this request.
     * @param headers to send with this request.
     */
    PostRequest(URL url, String body, Hashtable<String, String> headers) {
      this(url, body, null, headers);
    }

    PostRequest(URL url, byte[] body, Hashtable<String, String> headers) {
      this(url, null, body, headers);
    }

    PostRequest(URL url, String body, byte[] bytesBody, Hashtable<String, String> headers) {
      this.url = url;
      this.body = body == null ? "" : body;
      this.bytesBody = bytesBody;
      this.headers = headers == null ? new Hashtable<String, String>() : headers;
    }

    @Override
    public URL getUrl() {
      return url;
    }

    @Override
    public String getMethod() {
      return "POST";
    }

    @Override
    public String getBody() {
      return body;
    }

    @Override
    public byte[] getBytesBody() {
      return bytesBody;
    }

    ;

    @Override
    public Hashtable<String, String> getHeaders() {
      return headers;
    }
  }


  private static class GetRequest implements NetworkRequest {

    /**
     * Backend server url.
     */
    private final URL url;
    /**
     * Backend server url.
     */
    private final Hashtable<String, String> headers;

    /**
     * Basic constructor.
     *
     * @param url to send GET request to.
     */
    GetRequest(URL url) {
      this.url = url;
      this.headers = new Hashtable<String, String>();
    }

    /**
     * Constructor with extra HTTP headers associated with the GET request.
     *
     * @param url     to send request to.
     * @param headers to send with this request.
     */
    GetRequest(URL url, Hashtable<String, String> headers) {
      this.url = url;
      this.headers = headers == null ? new Hashtable<String, String>() : headers;
    }

    @Override
    public URL getUrl() {
      return url;
    }

    @Override
    public String getMethod() {
      return "GET";
    }

    @Override
    public String getBody() {
      return null;
    }

    @Override
    public byte[] getBytesBody() {
      return null;
    }

    ;

    @Override
    public Hashtable<String, String> getHeaders() {
      return headers;
    }
  }


  private ExecutorService executorService;
  private boolean needExit = false;


  public NetworkServerImpl(String threadName) {
    this.executorService = Executors
        .newSingleThreadExecutor(runnable -> new Thread(runnable, threadName));
  }

  @Override
  public void shutdown(){
    this.needExit = true;
    this.executorService.shutdownNow();
    Logger.d(TAG, "Network server shutdown completed");
  }


  public void get(final URL url, final INetworkRequestsCompletion callback) {
    GetRequest getRequest = new GetRequest(url);
    this.executorService.execute(new NetworkTaskRunnable(getRequest, callback));
  }

  public void post(final URL url, String body, Hashtable<String, String> headers,
      INetworkRequestsCompletion callback) {
    PostRequest postRequest = new PostRequest(url, body, headers);
    this.executorService.execute(new NetworkTaskRunnable(postRequest, callback));
  }


  public void post(final URL url, byte[] bytesBody, Hashtable<String, String> headers,
      INetworkRequestsCompletion callback) {
    PostRequest postRequest = new PostRequest(url, bytesBody, headers);
    this.executorService.execute(new NetworkTaskRunnable(postRequest, callback));
  }
}
