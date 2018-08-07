package com.insprout.okblib.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by okubo on 2018/02/05.
 * View関連の 一般的な操作を まとめる
 */

public class UiUtils {

    /**
     * リソースIDで指定されるViewの 有効/無効を設定する
     * @param activity Viewを表示しているActivity
     * @param id ViewのリソースID
     * @param enabled 設定する値 (true/false)
     */
    public static void enableView(Activity activity, int id, boolean enabled) {
        View view = activity.findViewById(id);
        if (view != null) {
            view.setEnabled(enabled);
        }
    }

    /**
     * リソースIDで指定されるViewの 表示状態を設定する
     * @param activity Viewを表示しているActivity
     * @param id ViewのリソースID
     * @param visibility 設定する値 (View.VISIBLE / View.INVISIBLE / View.GONE)
     */
    public static void setVisibility(Activity activity, int id, int visibility) {
        View view = activity.findViewById(id);
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    /**
     * リソースIDで指定されるViewの 選択状態を設定する
     * @param activity Viewを表示しているActivity
     * @param id ViewのリソースID
     * @param selected 設定する値 (true/false)
     */
    public static void setSelected(Activity activity, int id, boolean selected) {
        View view = activity.findViewById(id);
        if (view != null) {
            view.setSelected(selected);
        }
    }

    /**
     * リソースIDで指定されるViewの 表示テキストを設定する
     * @param activity Viewを表示しているActivity
     * @param id ViewのリソースID
     * @param textId 設定する文字列のリソースID
     */
    public static void setText(Activity activity, int id, int textId) {
        View view = activity.findViewById(id);
        if (view instanceof TextView) {
            ((TextView)view).setText(textId);
        }
    }

    /**
     * リソースIDで指定されるView(TextView / EditText / Button / RadioButton)の 表示テキストを設定する
     * @param activity Viewを表示しているActivity
     * @param id ViewのリソースID
     * @param text 設定する文字列
     */
    public static void setText(Activity activity, int id, CharSequence text) {
        View view = activity.findViewById(id);
        if (view instanceof TextView) {
            // EditText / Buttonは TextViewを継承したクラスなので、一括して処理
            ((TextView)view).setText(text);
        }
    }

    /**
     * リソースIDで指定されるViewの テキストを取得する
     * @param activity Viewを表示しているActivity
     * @param id ViewのリソースID
     * @return 取得された文字列
     */
    public static String getText(Activity activity, int id) {
        View view = activity.findViewById(id);
        if (view instanceof EditText) {
            // EditTextの getText()メソッドは、Editable型を返す
            return ((EditText)view).getText().toString();
        }
        if (view instanceof TextView) {
            return ((TextView)view).getText().toString();
        }
        return null;
    }

//    public static void setDrawables(Activity activity, int id, int foregroundResId, int backgroundResId) {
//        View view = activity.findViewById(id);
//        if (view instanceof ImageButton) {
//            ((ImageButton)view).setImageResource(foregroundResId);
//            ((ImageButton)view).setBackgroundResource(backgroundResId);
//        }
//    }

    /**
     * ViewのBitmapを返す。Bitmapは不要になったら呼び出し側で recycle()を実行し
     * メモリーリークしないようにすること。
     * @param view Bitmapを取得するView
     * @return viewのBitmap
     */
    public static Bitmap getBitmap(View view) {
        if (view == null) return null;

        Bitmap image = null;
        view.setDrawingCacheEnabled(true);
        // Viewのキャッシュを取得
        Bitmap cache = view.getDrawingCache();
        if (cache != null) {
            // View#getDrawingCacheはシステムの持っている Bitmap の参照を返す。
            // システム側から recycle されるので Bitmap#createBitmapで作り直す必要がある。
            image = Bitmap.createBitmap(cache);
        }
        view.setDrawingCacheEnabled(false);
        return image;
    }

    /**
     * asset内のテキストファイルから テキストを取得する
     * @param context コンテキスト
     * @param assetFile assetsフォルダの ファイル名
     * @return 読みだされたテキスト (ファイルが空でなければ、末尾は必ず改行が付く)
     */
    public static String getAssetText(Context context, String assetFile) {
        StringBuilder builder = new StringBuilder();
        AssetManager assetManager = context.getResources().getAssets();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(assetFile)))) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                builder.append(line).append("\n");
            }

        } catch (IOException e) {
            return null;
        }

        return builder.toString();
    }

}
