package com.insprout.okblib.network;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by okubo on 2018/02/21.
 * HttpRequestクラスで使用する パラメータ格納クラス
 */

public class HttpParameter {
    private final static String DEFAULT_CHARSET = "UTF-8";

    private String mName = null;
    private String mValue = "";


    public HttpParameter(String name, String value) {
        mName = name;
        mValue = value;
    }

    public HttpParameter(String name, Object value) {
        mName = name;
        mValue = value.toString();
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = value;
    }

    public void setValue(Object value) {
        mValue = value.toString();
    }

    public static String toQueryString(List<HttpParameter> params, String charset) {
        if (params == null) return "";

        StringBuilder builder = new StringBuilder();
        boolean flag1st = true;
        for (HttpParameter param : params) {
            String name = encode(param.getName(), charset);
            // 名前がカラの場合は スキップする
            if (name.isEmpty()) continue;
            if (flag1st) {
                flag1st = false;
            } else {
                builder.append("&");
            }
            builder.append(name).append("=").append(encode(param.getValue(), charset));

        }
        return builder.toString();
    }

    public static String toQueryString(String name, String value, String charset) {
        if (name == null || name.isEmpty()) return "";
        return encode(name, charset) + "=" + encode(value, charset);
    }

    public String toQueryString() {
        return toQueryString(mName, mValue, DEFAULT_CHARSET);
    }

    @Override
    public String toString() {
        if (mName == null) return "";
        return mName + "=" + mValue;
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
}
