package com.insprout.okblib.network;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by okubo on 2018/02/21.
 * HttpRequestクラスで使用する Responseデータ格納クラス
 */

public class HttpResponse {
    private int mHttpStatus;
    private byte[] mResponseBytes;
    private Map<String, String> mResponseHeaders;

    public HttpResponse(int httpStatus, byte[] responseBytes, Map<String, String> responseHeaders) {
        mHttpStatus = httpStatus;
        mResponseBytes = responseBytes;
        mResponseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return getResponseBody("UTF-8");
    }

    public String getResponseBody(String charset) {
        if (mResponseBytes == null) return null;
        try {
            return new String(mResponseBytes, charset);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
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
