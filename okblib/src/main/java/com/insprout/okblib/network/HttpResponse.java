package com.insprout.okblib.network;

import java.util.Map;

/**
 * Created by okubo on 2018/02/21.
 * HttpRequestクラスで使用する Responseデータ格納クラス
 */

public class HttpResponse {
    private int mHttpStatus;
    private String mResponseBody;
    private byte[] mResponseBytes;
    private Map<String, String> mResponseHeaders;

    public HttpResponse(int httpStatus, String responseBody, byte[] responseBytes, Map<String, String> responseHeaders) {
        mHttpStatus = httpStatus;
        mResponseBody = responseBody;
        mResponseBytes = responseBytes;
        mResponseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return mResponseBody;
    }

    public byte[] getResponseBytes() {
        return mResponseBytes;
    }

    public int getHttpStatus() {
        return mHttpStatus;
    }

    public String getResponseHeader(String headerName) {
        if (mResponseHeaders == null) return null;
        if (!mResponseHeaders.containsKey(headerName)) return null;

        return mResponseHeaders.get(headerName);
    }

}
