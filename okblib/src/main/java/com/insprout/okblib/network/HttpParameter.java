package com.insprout.okblib.network;

import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by okubo on 2018/02/21.
 * HttpRequestクラスで使用する パラメータ格納クラス
 */

public class HttpParameter {
    private final static String DEFAULT_CHARSET = "UTF-8";
    private final static String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private String mName = null;
    private String mValue = "";
    private File mFile = null;
    private String mMimeType = null;


    public HttpParameter(String name, String value) {
        mName = name;
        setValue(value);
    }

    public HttpParameter(String name, File value) {
        mName = name;
        setValue(value);
        mMimeType = null;
    }

    public HttpParameter(String name, File value, String mimeType) {
        mName = name;
        setValue(value);
        mMimeType = mimeType;
    }

    public String getName() {
        return mName;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = (value != null ? value : "");
        mFile = null;
    }

    public void setValue(File value) {
        mValue = (value != null ? value.getPath() : "");
        mFile = value;
    }

    public boolean hasFile() {
        return (mFile != null);
    }

    public File getFile() {
        return mFile;
    }

    public String getMimeType() {
        if (mFile == null) return null;
        if (mMimeType != null) return mMimeType;
        return getMimeTypeFromExtension(mFile);
    }

    public String toQueryString() {
        return toQueryString(mName, mValue, DEFAULT_CHARSET);
    }

    @Override
    public String toString() {
        if (mName == null) return "";
        return mName + "=" + mValue;
    }


    ////////////////////////////////////////////////////////////////////////
    //
    // static クラス
    //

    public static String toQueryString(List<HttpParameter> params, String charset) {
        if (params == null) return "";

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (HttpParameter param : params) {
            if (param == null) continue;
            String name = encode(param.getName(), charset);
            // 名前がカラの場合は スキップする
            if (name.isEmpty()) continue;
            if (count++ >= 1) builder.append("&");
            builder.append(name).append("=").append(encode(param.getValue(), charset));
        }
        return builder.toString();
    }

    public static String toQueryString(String name, String value) {
        return toQueryString(name, value, DEFAULT_CHARSET);
    }

    public static String toQueryString(String name, String value, String charset) {
        if (name == null || name.isEmpty()) return "";
        return encode(name, charset) + "=" + encode(value, charset);
    }

    public static byte[] getBytes(List<HttpParameter> params, String charset) {
        String queryString = toQueryString(params, charset);
        // queryStringを エンコードして 送信データを作成
        try {
            return queryString.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            return queryString.getBytes();
        }
    }

    public static boolean hasFile(List<HttpParameter> params) {
        if (params == null) return false;
        for (HttpParameter param : params) {
            if (param != null && param.hasFile()) return true;
        }
        return false;
    }

    private static String encode(String text, String charset) {
        if (text == null || text.isEmpty()) return "";
        try {
            return URLEncoder.encode(text, charset);
        } catch (UnsupportedEncodingException e) {
            // エンコードに失敗した場合は その元の文字列を返す
            return text;
        }
    }

    /**
     * ファイルの拡張子から そのファイルの mimeTypeを返す
     * MimeTypeMap.getFileExtensionFromUrl(url) は日本語や 空白の入ったファイル名ではうまく動作しないので、
     * 拡張子のみを抜き出してから、呼び出す
     * @param file ファイル
     * @return mimeType
     */
    public static String getMimeTypeFromExtension(File file) {
        //MimeTypeMap.getFileExtensionFromUrl(url) は日本語や 空白の入ったファイル名ではうまく動作しないので、自分で拡張子のみを取得する
        if (file == null) return CONTENT_TYPE_OCTET_STREAM;
        String filename = file.getName();
        int ptr = filename.lastIndexOf(".");
        if (ptr < 0) return CONTENT_TYPE_OCTET_STREAM;
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(filename.substring(ptr+1).toLowerCase());
    }

}
