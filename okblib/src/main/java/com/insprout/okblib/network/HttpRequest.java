package com.insprout.okblib.network;

import android.annotation.SuppressLint;
import android.os.Build;
import android.webkit.MimeTypeMap;

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
    public final static String CONTENT_TYPE_WWW_FORM = "application/x-www-form-urlencoded";
    public final static String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";
    public final static String CONTENT_TYPE_JSON = "application/json";

    public final static String ENCODING = "UTF-8";
    private final static String RESPONSE_HEADER_LOCATION = "Location";


    private final static boolean IGNORE_CERTIFICATE_SSL = true;      // 自己署名証明書（オレオレ証明書）サイト接続フラグ

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


    private HttpURLConnection mUrlCon = null;
    private Map<String, String> mExtraHeaders = null;
    private int mMethodType = METHOD_GET;
    private String mRequestUrl;
    private String mContentType = CONTENT_TYPE_WWW_FORM;
    private byte[] mRequestBody = null;
    private MultipartEntity mMultipartEntity = null;

    private int mTimeoutMilliSec = -1;




    public HttpRequest(int method, String url, List<HttpParameter> params) {
        this(method, url, params, null, null);
    }

    public HttpRequest(int method, String url, List<HttpParameter> params, Map<String, String> extraHeaders) {
        this(method, url, params, null, extraHeaders);
    }

    // タイムアウト時間を指定
    public HttpRequest(int method, String url, List<HttpParameter> params, Map<String, String> extraHeaders, int timeoutMilliSec) {
        this(method, url, params, null, extraHeaders);
        mTimeoutMilliSec = timeoutMilliSec;
    }

    // Fileを Multipart形式で送信する場合の コンストラクタ
    public HttpRequest(int method, String url, List<HttpParameter> params, List<HttpParameter> sendFiles, Map<String, String> extraHeaders) {

        mMethodType = method;
        mRequestUrl = url;
        String queryString = HttpParameter.toQueryString(params, ENCODING);

        if (hasResponseBody(mMethodType)) {
            // request bodyで データ送信を行うための準備をする
            if (sendFiles != null && sendFiles.size() > 0) {
                // multipart用の 送信データを作成する
                try {
                    mMultipartEntity = new MultipartEntity();
                    mMultipartEntity.addFormDataPart(params);
                    mMultipartEntity.addFilePart(sendFiles);

                } catch (Exception e) {
                    mMultipartEntity = null;
                }

            } else {
                // queryStringのみの場合は、x-www-form-urlencodedで送信する
                // queryStringを エンコードして 送信データを作成
                try {
                    mRequestBody = queryString.getBytes(ENCODING);
                } catch (UnsupportedEncodingException e) {
                    mRequestBody = queryString.getBytes();
                }
            }

        } else {
            if (!queryString.isEmpty()) {
                // uriに queryStringを付加する
                mRequestUrl += "?" + queryString;
            }
        }

        // セッションIDの設定
        mExtraHeaders = extraHeaders;

    }

    // rawデータをrequest bodyで送信する場合の コンストラクタ
    public HttpRequest(int method, String url, String contentType, byte[] requestBody, Map<String, String> apiExtraHeaders) {
        this(method, url, null, (List<HttpParameter>)null, apiExtraHeaders);
        // requestBodyは メソッドが POSTまたは PUTのときのみ有効
        if (hasResponseBody(method)) {
            if (requestBody != null) {
                mRequestBody = requestBody;
            } else {
                mRequestBody = new byte[0];
            }
            if (contentType != null) mContentType = contentType;
        }
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

    private boolean hasResponseBody(int method) {
        switch(method) {
            case METHOD_POST:
            case METHOD_PUT:
            case METHOD_PATCH:
                return true;

            default:
                return false;
        }
    }

    public void abort() {
        // HttpURLConnectionの場合、Thread.interrupt() で 割り込み可能なので、Abort処理は行わない
        // AndroidHttpClient通信版との互換性のため、メソッドはのこしておく
    }

    public  HttpResponse execute(File... output) {
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
        int responseCode = 0;
        String responseBody = "";                   // レスポンスを 文字列で返す際の変数
        File responseFile = null;                   // レスポンスをFileで返す際の出力先ファイル
        byte[] responseBytes = null;                // レスポンスを バイト配列で返す際の保存先
        InputStream responseStream = null;          // 受信したデータ(レスポンス)の input stream
        DataOutputStream outDataStream = null;      // 受信したデータを 書き出す output stream
        Map<String, String> responseHeaders = null;

        try {
            URL url;
            // http requestの レスポンス出力先にファイルが指定されている場合
            if (output.length >= 1) responseFile = output[0];

            url = new URL(requestUrl);
            mUrlCon = (HttpURLConnection)url.openConnection();

            // タイムアウト時間の設定
            if (mTimeoutMilliSec < 0) {
                // TIMEOUT時間が明示的に指定されていなければ、デフォルトの時間を使用する
                if (responseFile != null) {
                    // 受信に ファイルが指定されている場合は、Socket readの TIMEOUT時間を長く設定する
                    mTimeoutMilliSec = TIMEOUT_FILE_TRANSFER;
                } else {
                    mTimeoutMilliSec = TIMEOUT_MILLI_SEC;
                }
            }
            mUrlCon.setReadTimeout(mTimeoutMilliSec);
            mUrlCon.setConnectTimeout(TIMEOUT_CONNECTION_MILLI_SEC);

            // User-Agent設定
//            mUrlCon.setRequestProperty("User-Agent", HTTP_USER_AGENT);

            mUrlCon.setRequestMethod(toMethodString(mMethodType));
            if (mExtraHeaders != null) {
                for(Map.Entry<String, String> header : mExtraHeaders.entrySet()) {
                    mUrlCon.setRequestProperty(header.getKey(), (header.getValue() != null ? header.getValue() : ""));
                }
            }

            mUrlCon.setRequestProperty( "Accept-Encoding", "" );
            // httpUrlConnectionで EOFExceptionが 発生する問題に対応
            if (Build.VERSION.SDK_INT > 13) mUrlCon.setRequestProperty("Connection", "close");

            if (mMultipartEntity != null) {
                // multipart用のヘッダを宣言
                mUrlCon.setRequestProperty("Content-Type", mMultipartEntity.getContentType());
                // 送信データサイズをセット
                mUrlCon.setFixedLengthStreamingMode(mMultipartEntity.getContentLength());
                // 出力を行うように設定
                mUrlCon.setDoOutput(true);

            } else if (mRequestBody != null) {
                mUrlCon.setRequestProperty("Content-Type", mContentType);
                // 送信データサイズをセット
                mUrlCon.setFixedLengthStreamingMode(mRequestBody.length);
                // 出力を行うように設定
                mUrlCon.setDoOutput(true);
            }

            // httpレスポンスを受け取るように設定
            mUrlCon.setDoInput(true);

            // 自己署名証明書（オレオレ証明書）サイトに接続させるためのパッチ
            if (IGNORE_CERTIFICATE_SSL && mUrlCon instanceof HttpsURLConnection) ignoreCertificateSsl((HttpsURLConnection)mUrlCon);


            // http リクエスト コネクション
            int tryCount = MAX_TRY_CONNECTION_COUNT;
            boolean connected = false;
            do {
                try {
                    // コネクションの確立を試みる
                    tryCount--;
                    mUrlCon.connect();
                    connected = true;

                } catch (SocketTimeoutException e) {
                    // コネクションタイムアウトをチェックする
                    if (tryCount <= 0) throw e;
                    // 規定回数に達していない場合は、リトライする
                }
            } while (!connected);

            // 送信するデータがあれば、送る
            if (mMultipartEntity != null) {
                outDataStream = new DataOutputStream(mUrlCon.getOutputStream());
                // multipartデータを request bodyで 送信する
                mMultipartEntity.writeTo(outDataStream);
                outDataStream.flush();
                outDataStream.close();
                outDataStream = null;
            } else if (mRequestBody != null) {
                outDataStream = new DataOutputStream(mUrlCon.getOutputStream());
                // rawデータを request bodyで 送信する
                outDataStream.write(mRequestBody);
                outDataStream.flush();
                outDataStream.close();
                outDataStream = null;
            }

            // HTTPリクエストの レスポンスを 受け取る
            try {
                responseCode = mUrlCon.getResponseCode();
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
            for (Map.Entry<String, List<String>> entry : mUrlCon.getHeaderFields().entrySet()) {
                List<String> values = entry.getValue();
                responseHeaders.put(entry.getKey(), (values.size() == 0 ? "" : values.get(0)));
            }

            // レスポンスの入力ストリームを取得
            if (responseCode >= 400 && responseCode <= 599) {
                responseStream = mUrlCon.getErrorStream();
            } else {
                responseStream = mUrlCon.getInputStream();
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

        } catch (InterruptedException e) {
            responseCode = STATUS_INTERRUPTED;

        } catch (SocketException e) {
            responseCode = STATUS_INTERRUPTED;

        } catch (SocketTimeoutException e) {
            responseCode = STATUS_ERROR_TIMEOUT;

        } catch (SSLHandshakeException e) {
            responseCode = STATUS_ERROR_INTERNAL;

        } catch (SecurityException e ) {
            responseCode = STATUS_ERROR_PERMISSION;

        } catch (FileNotFoundException e ) {
            responseCode = STATUS_ERROR_INTERNAL;

        } catch (IOException e ) {
            responseCode = STATUS_ERROR_INTERNAL;

        } catch (Exception e ) {
            responseCode = STATUS_ERROR_INTERNAL;

        } finally {
            if (mUrlCon != null) mUrlCon.disconnect();
            mUrlCon = null;
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

    @SuppressLint("TrulyRandom")
    private void ignoreCertificateSsl(HttpsURLConnection httpsConnection) throws NoSuchAlgorithmException, KeyManagementException {
        //証明書情報　全て空を返す
        KeyManager[] keyMgr = null;
        TrustManager[] trustMgr = {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }
                }
        };
        SSLContext sslcontext = SSLContext.getInstance("SSL");
        sslcontext.init(keyMgr, trustMgr, new SecureRandom());

        //ホスト名の検証　常にtrueを返す
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        // 自己署名証明書を エラーとしない SSLSocketFactoryを設定する
        httpsConnection.setSSLSocketFactory(sslcontext.getSocketFactory());
    }


    private class MultipartEntity {
        private final String mBoundary;
        private final String mBoundaryStart;
        private final String mBoundaryEnd;
        private byte[] mBytesBoundaryEnd;

        private ByteArrayOutputStream mFormDataPart = new ByteArrayOutputStream();
        private List<FilePartEntity> mFilePart = new ArrayList<>();


        public MultipartEntity() throws UnsupportedEncodingException {
            // バウンダリー文字列生成
            mBoundary = UUID.randomUUID().toString();
            // 終端用バウンダリー設定
            mBoundaryStart = "--" + mBoundary + "\r\n";
            mBoundaryEnd = "--" + mBoundary + "--\r\n";
            mBytesBoundaryEnd = mBoundaryEnd.getBytes(ENCODING);
        }

        // マルチパート用 Content-Typeを返す
        public String getContentType() {
            return "multipart/form-data; boundary=" + mBoundary;
        }

        // multipartデータのサイズを返す
        public int getContentLength() {
            // 送信するパートが 何もなければ、0を返す
            if (isNoPart()) return 0;

            int length = mFormDataPart.toByteArray().length + mBytesBoundaryEnd.length;
            for (FilePartEntity fileEntity : mFilePart) {
                length += fileEntity.getContentLength();
            }
            return length;
        }

        // multipartデータの 出力を行う
        public void writeTo(OutputStream outStream) throws IOException, InterruptedException {
            // 送信するパートが 何もなければ、0サイズの書き込みを行う。終了バウンダリーの書き込みも行わない
            if (isNoPart()) {
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

        // form data パート（複数パート分）の 登録を行う
        public void addFormDataPart(List<HttpParameter> params) throws IOException {
            if (params == null) return;
            for (HttpParameter param : params) {
                addFormDataPart(param.getName(), param.getValue());
            }
        }

        // form data パート（1パート分）の 登録を行う
        public void addFormDataPart(String key, String value) throws IOException {
            // keyが指定されているか確認する
            if (key == null || key.length() == 0) return;

            mFormDataPart.write(mBoundaryStart.getBytes(ENCODING));
            mFormDataPart.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n").getBytes(ENCODING));
            mFormDataPart.write(("Content-Type: text/plain; charset=" + ENCODING + "\r\n").getBytes(ENCODING));
            mFormDataPart.write("Content-Transfer-Encoding: 8bit\r\n\r\n".getBytes(ENCODING));
            mFormDataPart.write((value!=null ? value : "").getBytes(ENCODING));
            mFormDataPart.write(("\r\n").getBytes(ENCODING));
        }

        // file パート（複数パート分）の 登録を行う
        public void addFilePart(List<HttpParameter> files) throws IOException {
            if (files == null) return;
            for (HttpParameter param : files) {
                String key = param.getName();
                String path = param.getValue();
                File file = new File(path);
                addFilePart(key, file);
            }
        }

        // file パート（1パート分）の 登録を行う
        public void addFilePart(String key, File value) throws IOException {
            String filename = value.getName();
            String mimeType = getMimeTypeFromExtension(filename);
            if (mimeType == null || mimeType.length() == 0) mimeType = CONTENT_TYPE_OCTET_STREAM;

            mFilePart.add(new FilePartEntity(key, value, mimeType));
        }

        private boolean isNoPart() {
            if (mFormDataPart.size() == 0 && mFilePart.size() == 0) return true;
            return false;
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

    /**
     * ファイルの拡張子から そのファイルの mimeTypeを返す
     * MimeTypeMap.getFileExtensionFromUrl(url) は日本語や 空白の入ったファイル名ではうまく動作しないので、
     * 拡張子のみを抜き出してから、呼び出す
     * @param path ファイルパス
     * @return mimeType
     */
    public static String getMimeTypeFromExtension(String path) {
        //MimeTypeMap.getFileExtensionFromUrl(url) は日本語や 空白の入ったファイル名ではうまく動作しないので、自分で拡張子のみを取得する
        File file = new File(path);
        String filename = file.getName();
        int ptr = filename.lastIndexOf(".");
        String ext = (ptr >= 0 ? filename.substring(ptr+1) : "");
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
    }

}
