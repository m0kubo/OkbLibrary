package com.insprout.okblib.network;

import android.os.Build;
import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * Created by okubo on 2018/02/21.
 * Http通信を実行する
 */

public class HttpRequest {

    public final static String ENCODING = "UTF-8";
    private final static String RESPONSE_HEADER_LOCATION = "Location";


    // ネットワーク通信用 定数
    public final static int METHOD_GET = 0;
    public final static int METHOD_POST = 1;
    public final static int METHOD_PUT = 2;
    public final static int METHOD_DELETE = 3;
    public final static int METHOD_PATCH = 4;

    public final static int STATUS_ERROR_TIMEOUT = 0;
    public final static int STATUS_ERROR_INTERNAL = -1;
    public final static int STATUS_ERROR_FILEIO = -2;
    public final static int STATUS_INTERRUPTED = -3;
    public final static int STATUS_ERROR_PERMISSION = -4;

    private final static int MAX_TRY_CONNECTION_COUNT = 3;
    private final static int TIMEOUT_CONNECTION_MILLI_SEC = 6000;
    private final static int TIMEOUT_MILLI_SEC = 30000;
    private final static int TIMEOUT_FILE_TRANSFER = 300000;    // 300,000ミリ秒 = 5分

    private final static int BUFF_SIZE = 16 * 1024;
    private final static int COUNT_CHECK_INTERRUPT = (1024 * 1024) / BUFF_SIZE;


    private int mMethodType = METHOD_GET;
    private String mRequestUrl;
    private List<HttpParameter> mRequestQueryParams = null;
    private IRequestBody mRequestBody = null;
    private Map<String, String> mExtraHeaders = new HashMap<>();
    private String mUserAgent = null;
    private String mUserPassword = null;
    private int mTimeoutMilliSec = -1;
    private boolean mIgnoreCertificateSsl = false;              // 自己署名証明書（オレオレ証明書）サイト接続フラグ


    public HttpRequest(int method, String url) {
        mMethodType = method;
        mRequestUrl = url;
    }

    public HttpRequest(int method, String url, List<HttpParameter> params) {
        this(method, url);
        setContent(params);
    }

    // これ以前にsetContent()メソッドで設定した内容は上書きされる
    public HttpRequest setContent(List<HttpParameter> params) {

        if (!hasRequestBody()) {
            // GETなどの場合は Fileは無視する
            // paramsは uriに queryStringとして付加する
            mRequestQueryParams = params;
            mRequestBody = null;

        } else if (HttpParameter.hasFile(params)) {
            // 送信データにFileが含まれている場合は、multipart用の 送信データを作成する
            mRequestQueryParams = null;
            try {
                mRequestBody = new MultipartEntity(params);

            } catch (Exception e) {
                mRequestBody = null;
            }

        } else {
            // paramsのみの場合は、x-www-form-urlencodedで送信する
            mRequestQueryParams = null;
            mRequestBody = new RequestBodyEntity(params);
        }

        return this;
    }

    // request bodyで 任意のデータを送信する場合
    // これ以前にsetContent()メソッドで設定した内容は上書きされる
    public HttpRequest setContent(byte[] requestBody, String contentType) {
        mRequestQueryParams = null;
        mRequestBody = new RequestBodyEntity(requestBody, contentType);
        return this;
    }

    public HttpRequest setAuthorization(String account, String password) {
        if (account != null) {
            mUserPassword = account + ":" + (password != null ? password : "");
        }
        return this;
    }

    // User Agentを指定
    public HttpRequest setUserAgent(String userAgent) {
        mUserAgent = userAgent;
        return this;
    }

    // タイムアウト時間を指定
    public HttpRequest setTimeout(int timeoutMilliSec) {
        mTimeoutMilliSec = timeoutMilliSec;
        return this;
    }

    public HttpRequest addRequestHeaders(Map<String, String> extraHeaders) {
        if (extraHeaders != null) mExtraHeaders.putAll(extraHeaders);
        return this;
    }

    public HttpRequest setIgnoreCertificateSsl(boolean ignore) {
        mIgnoreCertificateSsl = ignore;
        return this;
    }


    private String toMethodString(int type) {
        switch (type) {
            case METHOD_POST:
                return "POST";

            case METHOD_PUT:
                return "PUT";

            case METHOD_DELETE:
                return "DELETE";

            case METHOD_PATCH:
                return "PATCH";

            //case METHOD_GET:
            default:
                return "GET";
        }
    }

    private boolean hasRequestBody() {
        switch(mMethodType) {
            case METHOD_POST:
            case METHOD_PUT:
            case METHOD_PATCH:
                return true;

            default:
                return false;
        }
    }

    private URL getURL(String spec, List<HttpParameter> params) throws MalformedURLException {
        URL url;
        url = new URL(spec);
        if (params != null && !params.isEmpty()) {
            // 追加の queryパラメータが指定されている場合は、それを付加してURLオブジェクトを作成する
            // 最初のURLオブジェクトは、既にqueryパートが存在するかどうかの判別に使用する
            url = new URL(spec + (url.getQuery() != null ? "&" : "?") + HttpParameter.toQueryString(params, ENCODING));
        }
        return url;
    }

    // タイムアウト時間の設定
    private int getTimeout(File... output) {
        // TIMEOUT時間が定されていれば、その時間を使用する
        if (mTimeoutMilliSec >= 0) {
            return mTimeoutMilliSec;

        } else if (output.length >= 1 && output[0] != null) {
            // 受信に ファイルが指定されている場合は、Socket readの TIMEOUT時間を長くする
            return TIMEOUT_FILE_TRANSFER;

        } else {
            return TIMEOUT_MILLI_SEC;
        }
    }

    public void abort() {
        // HttpURLConnectionの場合、Thread.interrupt() で 割り込み可能なので、Abort処理は行わない
        // AndroidHttpClient通信版との互換性のため、メソッドはのこしておく
    }

    public  HttpResponse execute(File... output) {
        if (mRequestUrl == null || mRequestUrl.isEmpty()) {
            // リクエストURLが未指定の場合は 400 Bad Requestを返しておく
            return new HttpResponse(400, null, null, null);
        }
        HttpResponse response = executeRequest(mRequestUrl, output);

        // status 307 Temporary Redirectに対応
        // 302 / 303は HttpURLConnectionで対応されている
        if (response.getHttpStatus() == 307 || response.getHttpStatus() == 308) {
            String redirectUrl = response.getResponseHeader(RESPONSE_HEADER_LOCATION);
            if (redirectUrl != null && !redirectUrl.isEmpty()) {
                response = executeRequest(redirectUrl, output);
            }
        }
        return response;
    }

    private HttpResponse executeRequest(String requestUrl, File... output) {
        HttpURLConnection urlConnection = null;
        int responseCode = 0;
        String responseBody = "";                   // レスポンスを 文字列で返す際の変数
        byte[] responseBytes = null;                // レスポンスを バイト配列で返す際の保存先
        InputStream responseStream = null;          // 受信したデータ(レスポンス)の input stream
        DataOutputStream outDataStream = null;      // 受信したデータを 書き出す output stream
        Map<String, String> responseHeaders = null;
        File responseFile = (output.length >= 1 ? output[0] : null);    // レスポンスをFileで返す際の出力先ファイル

        try {
            URL url = getURL(requestUrl, mRequestQueryParams);
            if (mUserPassword == null) {
                // ユーザ/パスワードが未設定の場合は URLからの取得も試みる
                mUserPassword = url.getUserInfo();
            }
            urlConnection = (HttpURLConnection)url.openConnection();

            if (mIgnoreCertificateSsl && urlConnection instanceof HttpsURLConnection) {
                // 自己署名証明書（オレオレ証明書）サイトに接続させるためのパッチ
                ignoreCertificateSsl((HttpsURLConnection)urlConnection);
            }

            // タイムアウト時間の設定
            urlConnection.setReadTimeout(getTimeout(output));
            urlConnection.setConnectTimeout(TIMEOUT_CONNECTION_MILLI_SEC);

            // User-Agent設定
            if (mUserAgent != null) urlConnection.setRequestProperty("User-Agent", mUserAgent);

            urlConnection.setRequestMethod(toMethodString(mMethodType));
            for(Map.Entry<String, String> header : mExtraHeaders.entrySet()) {
                urlConnection.setRequestProperty(header.getKey(), (header.getValue() != null ? header.getValue() : ""));
            }

            if (mUserPassword != null) {
                urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(mUserPassword.getBytes(), Base64.NO_WRAP));
            }
            urlConnection.setRequestProperty( "Accept-Encoding", "" );
            // httpUrlConnectionで EOFExceptionが 発生する問題に対応
            if (Build.VERSION.SDK_INT > 13) urlConnection.setRequestProperty("Connection", "close");

            if (mRequestBody != null) {
                urlConnection.setRequestProperty("Content-Type", mRequestBody.getContentType());
                // 送信データサイズをセット
                urlConnection.setFixedLengthStreamingMode(mRequestBody.getContentLength());
                // 出力を行うように設定
                urlConnection.setDoOutput(true);
            }

            // httpレスポンスを受け取るように設定
            urlConnection.setDoInput(true);


            // http リクエスト コネクション
            int tryCount = MAX_TRY_CONNECTION_COUNT;
            boolean connected = false;
            do {
                try {
                    // コネクションの確立を試みる
                    tryCount--;
                    urlConnection.connect();
                    connected = true;

                } catch (SocketTimeoutException e) {
                    // コネクションタイムアウトをチェックする
                    if (tryCount <= 0) throw e;
                    // 規定回数に達していない場合は、リトライする
                }
            } while (!connected);

            // 送信するデータがあれば送る
            if (mRequestBody != null) {
                outDataStream = new DataOutputStream(urlConnection.getOutputStream());
                // multipartデータを request bodyで 送信する
                mRequestBody.writeTo(outDataStream);
                outDataStream.flush();
                outDataStream.close();
                outDataStream = null;
            }

            // HTTPリクエストの レスポンスを 受け取る
            try {
                responseCode = urlConnection.getResponseCode();
            }
            catch (IOException e) {
                // apiサーバが status 401の際に 「WWW-Authenticate: ～」をつけてレスポンスを返さない件に対応
                // 認証系のエラーの場合、401エラーとして継続してみる
                // 別のエラーの場合は、上位のエラーハンドラーにまかせる
                String errMsg = e.getMessage();
                if (errMsg != null && errMsg.toLowerCase(Locale.ENGLISH).contains("authentication")) {
                    responseCode = 401;
                } else {
                    throw e;
                }
            }
            // レスポンスヘッダを取得する
            responseHeaders = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : urlConnection.getHeaderFields().entrySet()) {
                List<String> values = entry.getValue();
                responseHeaders.put(entry.getKey(), (values.size() == 0 ? "" : values.get(0)));
            }

            // レスポンスの入力ストリームを取得
            if (responseCode >= 400 && responseCode <= 599) {
                responseStream = urlConnection.getErrorStream();
            } else {
                responseStream = urlConnection.getInputStream();
            }

            // 指定によって、responseの出力形式を切り替える
            if (output.length == 0) {
                // responseBodyとして 返す（文字列）
                responseBody = toString(responseStream);
            } else if (responseFile != null) {
                // 出力先に Fileが指定されていた場合は、そこに書き出す
                toFile(responseFile, responseStream);
            } else {
                // rawデータを メモリで返す（バイト配列）
                responseBytes = toBytes(responseStream);
            }

        } catch (InterruptedException | SocketException e) {
            responseCode = STATUS_INTERRUPTED;

        } catch (SocketTimeoutException e) {
            responseCode = STATUS_ERROR_TIMEOUT;

        } catch (SecurityException e ) {
            responseCode = STATUS_ERROR_PERMISSION;

        } catch (IOException e ) {
            responseCode = STATUS_ERROR_INTERNAL;

        } finally {
            if (urlConnection != null) urlConnection.disconnect();
            urlConnection = null;
            try {
                if (outDataStream != null) outDataStream.close();
            } catch (IOException e) {
                // close処理が完了しなくても無視する
            }
            try {
                if (responseStream != null) responseStream.close();
            } catch (IOException e) {
                // close処理が完了しなくても無視する
            }
            if (responseCode != 200) {
                // 失敗した受信ファイルを削除
                if (responseFile != null && responseFile.exists()) responseFile.delete();
            }
        }

        return new HttpResponse(responseCode, responseBody, responseBytes, responseHeaders);
    }

    private String toString(InputStream is) throws InterruptedException, IOException {
        char[] buffer = new char[ BUFF_SIZE ];
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, ENCODING), BUFF_SIZE);      // BufferedReaderのデフォルトバッファサイズは 8k
        StringBuilder sb = new StringBuilder();

        try {
            int count = 0;
            int size;
            while (0 <= (size = reader.read(buffer))) {
                sb.append(buffer, 0, size);
                // interruptのチェック用処理
                if (++count >= COUNT_CHECK_INTERRUPT) {
                    // 割り込みを受け付けるように sleep()を呼んでおく
                    Thread.sleep(0, 1);
                    count = 0;
                }
            }

        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // close処理が完了しなくても無視する
            }
        }

        return sb.toString();
    }

    private byte[] toBytes(InputStream is) throws InterruptedException, IOException {
        byte[] buffer = new byte[ BUFF_SIZE ];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedInputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = new BufferedInputStream(is, BUFF_SIZE);
            outputStream = new BufferedOutputStream(bytes);

            int count = 0;
            int size;
            while ((size = inputStream.read(buffer, 0, BUFF_SIZE)) != -1) {
                outputStream.write(buffer, 0, size);
                // interruptのチェック用処理
                if (++count >= COUNT_CHECK_INTERRUPT) {
                    // 割り込みを受け付けるように sleep()を呼んでおく
                    Thread.sleep(0, 1);
                    count = 0;
                }
            }
            outputStream.flush();

        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                // close処理が完了しなくても無視する
            }
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                // close処理が完了しなくても無視する
            }
        }

        return bytes.toByteArray();
    }

    private void toFile(File file, InputStream is) throws InterruptedException, IOException, SecurityException {
        // 出力先に Fileが指定されていた場合は、そこに書き出す
        byte[] buffer = new byte[ BUFF_SIZE ];
        DataInputStream inResStream = null;
        FileOutputStream outResStream = null;

        try {
            inResStream = new DataInputStream(is);
            outResStream = new FileOutputStream(file);

            int count = 0;
            int size;
            while((size = inResStream.read(buffer)) != -1) {
                outResStream.write(buffer, 0, size);
                // interruptのチェック用処理
                if (++count >= COUNT_CHECK_INTERRUPT) {
                    // 割り込みを受け付けるように sleep()を呼んでおく
                    Thread.sleep(0, 1);
                    count = 0;
                }
            }
            inResStream.close();
            inResStream = null;

        } finally {
            try {
                if (outResStream != null) outResStream.close();
            } catch (IOException e) {
                // close処理が完了しなくても無視する
            }
            try {
                if (inResStream != null) inResStream.close();
            } catch (IOException e) {
                // close処理が完了しなくても無視する
            }
        }
    }

    // 自己署名証明書（オレオレ証明書）サイトに接続させるためのパッチ処理
    private void ignoreCertificateSsl(HttpsURLConnection httpsConnection)  {
        // 証明書チェーンの検証をスキップ
        KeyManager[] keyManagers = null;
        TrustManager[] transManagers = {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }
                }
        };

        try {
            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(keyManagers, transManagers, new SecureRandom());

            // 証明書に書かれているCommon NameとURLのホスト名が一致していることの検証をスキップ
            httpsConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            // 自己署名証明書を エラーとしない SSLSocketFactoryを設定する
            httpsConnection.setSSLSocketFactory(sslcontext.getSocketFactory());

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            //e.printStackTrace();
        }
    }


    //////////////////////////////////////////////////////////////////////
    //
    // Interface
    //

    private interface IRequestBody {

        String getContentType();

        int getContentLength();

        void writeTo(OutputStream outStream) throws IOException, InterruptedException;
    }


    private class RequestBodyEntity implements IRequestBody {
        private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
        private final static String CONTENT_TYPE_WWW_FORM = "application/x-www-form-urlencoded";

        private byte[] mRequestBody;
        private String mContentType;

        // form-urlencoded用 コンストラクタ
        public RequestBodyEntity(List<HttpParameter> params) {
            mRequestBody = HttpParameter.getBytes(params, ENCODING);
            mContentType = CONTENT_TYPE_WWW_FORM;
        }

        // rawデータ用 コンストラクタ
        public RequestBodyEntity(byte[] requestBody, String contentType) {
            mRequestBody = requestBody;
            mContentType = (contentType != null ? contentType : CONTENT_TYPE_OCTET_STREAM);
        }

        @Override
        public String getContentType() {
            return mContentType;
        }

        @Override
        public int getContentLength() {
            return (mRequestBody != null ? mRequestBody.length : 0);
        }

        @Override
        public void writeTo(OutputStream outStream) throws IOException, InterruptedException {
            if (outStream != null) outStream.write(mRequestBody);
        }
    }


    private class MultipartEntity implements IRequestBody {
        private final String mBoundary;
        private final String mBoundaryStart;
        private final String mBoundaryEnd;
        private byte[] mBytesBoundaryEnd;

        private ByteArrayOutputStream mFormDataPart = new ByteArrayOutputStream();
        private List<FilePartEntity> mFilePart = new ArrayList<>();


        public MultipartEntity(List<HttpParameter> params) throws UnsupportedEncodingException, IOException {
            // バウンダリー文字列生成
            mBoundary = UUID.randomUUID().toString();
            // 終端用バウンダリー設定
            mBoundaryStart = "--" + mBoundary + "\r\n";
            mBoundaryEnd = "--" + mBoundary + "--\r\n";
            mBytesBoundaryEnd = mBoundaryEnd.getBytes(ENCODING);

            addEntities(params);
        }

        // マルチパート用 Content-Typeを返す
        public String getContentType() {
            return "multipart/form-data; boundary=" + mBoundary;
        }

        // multipartデータのサイズを返す
        public int getContentLength() {
            // 送信するパートが 何もなければ、0を返す
            if (!hasEntity()) return 0;

            int length = mFormDataPart.toByteArray().length + mBytesBoundaryEnd.length;
            for (FilePartEntity fileEntity : mFilePart) {
                length += fileEntity.getContentLength();
            }
            return length;
        }

        // multipartデータの 出力を行う
        public void writeTo(OutputStream outStream) throws IOException, InterruptedException {
            // 送信するパートが 何もなければ、0サイズの書き込みを行う。終了バウンダリーの書き込みも行わない
            if (!hasEntity()) {
                outStream.write(new byte[0]);
                return;
            }
            // form dataパートの出力
            outStream.write(mFormDataPart.toByteArray());
            // Fileパートの 出力
            for (FilePartEntity fileEntity : mFilePart) {
                fileEntity.writeTo(outStream);
            }
            // multipart ENDデータを出力
            outStream.write(mBytesBoundaryEnd);
        }


        public void addEntities(List<HttpParameter> params) throws IOException {
            if (params == null) return;
            for (HttpParameter param : params) {
                addEntity(param);
            }
        }

        public void addEntity(HttpParameter param) throws IOException {
            if (param == null) return;

            if (param.hasFile()) {
                // form data パートの登録を行う
                mFilePart.add(new FilePartEntity(param.getName(), param.getFile(), param.getMimeType()));

            } else {
                // form data パートの登録を行う
                // keyが指定されているか確認する
                String key = param.getName();
                if (key == null || key.length() == 0) return;

                String value = param.getValue();
                mFormDataPart.write(mBoundaryStart.getBytes(ENCODING));
                mFormDataPart.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n").getBytes(ENCODING));
                mFormDataPart.write(("Content-Type: text/plain; charset=" + ENCODING + "\r\n").getBytes(ENCODING));
                mFormDataPart.write("Content-Transfer-Encoding: 8bit\r\n\r\n".getBytes(ENCODING));
                mFormDataPart.write((value!=null ? value : "").getBytes(ENCODING));
                mFormDataPart.write(("\r\n").getBytes(ENCODING));
            }
        }


        private boolean hasEntity() {
            return (mFormDataPart.size() >= 1 || mFilePart.size() >= 1);
        }

        private class FilePartEntity {
            private byte[] mPartHeader;
            private byte[] mPartBottom;
            private File mFile = null;
            private long mFileLength = 0;

            /**
             * コンストラクタ
             * @param key パラメータ名
             * @param file 送信するFile
             * @param mimeType 送信するファイルの contentType
             * @throws IOException
             */
            public FilePartEntity (String key, File file, String mimeType) throws IOException {
                // keyが指定されているか確認する
                if (key == null || key.length() == 0) return;
                if (file == null) return;
                mFile = file;
                mFileLength = file.length();

                // マルチパート間の ヘッダー部分を登録
                String header = mBoundaryStart
                        + "Content-Disposition: form-data; name=\""+ key +"\"; filename=\"" + file.getName() + "\"\r\n"
                        + "Content-Type: " + mimeType + "\r\n"
                        + "Content-Transfer-Encoding: binary\r\n\r\n" ;
                mPartHeader = header.getBytes(ENCODING);
                mPartBottom = ("\r\n").getBytes(ENCODING);
            }

            /**
             * 送信する コンテンツのバイト数を返す
             * @return バイト数
             */
            public long getContentLength() {
                if (mFile == null) return 0;
                return mPartHeader.length + mFileLength + mPartBottom.length;
            }

            public void writeTo(OutputStream outStream) throws IOException, InterruptedException, SecurityException {
                // multipartの Fileデータを送信する
                BufferedInputStream inDataStream = null;
                byte[] buffer = new byte[ BUFF_SIZE ];

                if (mFile == null) return;
                outStream.write(mPartHeader);
                try {
                    inDataStream = new BufferedInputStream(new FileInputStream(mFile), BUFF_SIZE);
                    int count;
                    while ((count = inDataStream.read(buffer, 0, BUFF_SIZE)) != -1) {
                        outStream.write(buffer, 0, count);
                        // 割り込みを受け付けるように sleep()を呼んでおく
                        Thread.sleep(0, 10);
                    }

                } finally {
                    // streamの終了処理をおこなっておく
                    if (inDataStream != null) inDataStream.close();
                }
                outStream.write(mPartBottom);
            }
        }

    }

}
