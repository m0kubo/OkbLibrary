package com.insprout.okblib.network;

import android.os.AsyncTask;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by okubo on 2018/02/21.
 * Http通信(HttpRequestクラス)を AsyncTaskで 非同期に実行させるためのクラス
 */

public class HttpRequestTask extends AsyncTask<File, Void, HttpResponse> {

    private HttpRequest mApiRequest = null;
    private OnResponseListener mListener = null;

    public HttpRequestTask(int method, final String url, OnResponseListener listener) {
        mApiRequest = new HttpRequest(method, url);
        mListener = listener;
    }

    public HttpRequestTask(int method, final String url, List<HttpParameter> params, OnResponseListener listener) {
        mApiRequest = new HttpRequest(method, url, params);
        mListener = listener;
    }

    public HttpRequestTask setContent(String contentType, byte[] requestBody) {
        mApiRequest.setContent(requestBody, contentType);
        return this;
    }

    public HttpRequestTask setContent(List<HttpParameter> params) {
        mApiRequest.setContent(params);
        return this;
    }

    public HttpRequestTask setAuthorization(String user, String password) {
        mApiRequest.setAuthorization(user, password);
        return this;
    }

    public HttpRequestTask setTimeout(int timeoutSec) {
        // Timeoutは ミリ秒単位で指定するので、1000倍する
        mApiRequest.setTimeout(timeoutSec * 1000);
        return this;
    }

    public HttpRequestTask ignoreCertificateSsl(boolean ignore) {
        mApiRequest.ignoreCertificateSsl(ignore);
        return this;
    }

    public HttpRequestTask syncCookie(String url) {
        mApiRequest.syncCookie(url);
        return this;
    }

    public HttpRequestTask addRequestHeaders(Map<String, String> extraHeaders) {
        mApiRequest.addRequestHeaders(extraHeaders);
        return this;
    }


    @Override
    protected HttpResponse doInBackground(File... params) {
        return mApiRequest.execute(params.length >= 1 ? params[0] : null);
    }

    @Override
    protected void onPostExecute(HttpResponse response) {
        if (mListener != null && !isCancelled()) {
            mListener.onResponse(response);
        }
    }

    @Override
    protected void onCancelled() {
        // AsyncTask.cancel() がコールされると呼び出される。メインスレッドとは 実行されるスレッドが異なるので、
        // ここで ネットワーク処理を実行する (NetworkOnMainThreadExceptionは発生しない)

        // AndroidHttpClient版では、通信処理が 隠蔽されているので abort()メソッドを呼び出して、通信を中断させる
        if (mApiRequest != null) mApiRequest.abort();
    }

    public interface OnResponseListener {
        void onResponse(HttpResponse response);
    }

}
