package com.insprout.okblib.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by okubo on 2018/02/01.
 * androidのバージョンによる apiの違いを吸収する
 */

public class SdkUtils {

    //////////////////////////////////////////////////////////////////////////
    //
    // Runtime Permission関連
    //

    /**
     * 指定された パーミッションが付与されているか確認し、権限がない場合は指定されたrequestCodeで権限付与画面を呼び出す。
     * ただし、requestCodeが -1の場合は、権限付与画面は呼び出さない
     *
     * @param activity    この機能を利用するアクティビティ
     * @param permissions 確認するパーミッション(複数)
     * @param requestCode 権限付与画面をよびだす際の、リクエストコード。(onRequestPermissionsResult()で判別する)
     * @return true: すでに必要な権限は付与されている。false: 権限が不足している。
     */
    public static boolean requestRuntimePermissions(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT < 23) return true;

        // リクエストされたパーミッションの内、許可されてないものを調べる
        List<String> deniedPermissions = getDeniedPermissions(activity, permissions);
        if (deniedPermissions.size() == 0) return true;

        // リクエストコードが指定されている場合は、権限付与画面を呼び出す
        if (requestCode != -1) {
            // 許可のないパーミッションに対して 権限付与画面を呼び出す
            String[] requestPermissions = new String[ deniedPermissions.size() ];
            deniedPermissions.toArray(requestPermissions);
            activity.requestPermissions(requestPermissions, requestCode);
        }
        return false;       // 権限不足
    }

    /**
     * 指定された RUNTIMEパーミッションの内、許可されていないものを返す。
     * @param context  コンテキスト
     * @param permissions 確認するパーミッション(複数)
     * @return 許可されていないバーミッションのリスト。すべて許可されている場合はサイズ0のリストを返す。(nullを返すことはない)
     */
    private static List<String> getDeniedPermissions(Context context, String[] permissions) {
        List<String> deniedPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT < 23) return deniedPermissions;
        if (permissions == null || permissions.length == 0) return deniedPermissions;

        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // 許可のないpermissionを記録
                deniedPermissions.add(permission);
            }
        }
        return deniedPermissions;
    }

    /**
     * 指定された パーミッションが付与されているか確認する
     * @param context  コンテキスト
     * @param permissions 確認するパーミッション(複数)
     * @return true: すでに必要な権限は付与されている。false: 権限が不足している。
     */
    public static boolean checkSelfPermission(Context context, String[] permissions) {
        return (getDeniedPermissions(context, permissions).size() == 0);
    }


    /**
     * onRequestPermissionsResult()で返された結果から、権限がすべて与えられたかチェックする
     * @param grantResults チェックする権限
     * @return true: 必要な権限は全て付与されている。false: 権限が不足している。
     */
    public static boolean isGranted(int[] grantResults) {
        if (grantResults == null) return false;
        for (int result : grantResults) {
            // 必要な PERMISSIONは付与されなかった
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }
        // 必要なPERMISSIONがすべて付与されている
        return true;
    }

    /**
     * 指定のパーミッションが、以前にユーザから「今後は確認しない」にチェックが付けられたかどうかをチェックする
     * @param activity 呼び出しているActivity
     * @param permissions チェックするパーミッション(複数)
     * @return true: 指定のパーミッションは 全て「今後は確認しない」にチェック無し。(確認画面を表示してよい)  false: 1つ以上のものが「今後は確認しない」にチェックされた
     */
    public static boolean isPermissionRationale(Activity activity, String[] permissions) {
        if (Build.VERSION.SDK_INT >= 23) {
            for (String permission : permissions) {
                if (!activity.shouldShowRequestPermissionRationale(permission)) {
                    // 以前に 「今後は確認しない」にチェックが付けられた permission
                    return false;
                }
            }
        }
        return true;
    }

// AndroidManifest.xmlに android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS パーミッションが 指定されていると、
// Google playに submitできなくなるので、以下のメソッドは破棄
//    /**
//     * DOZEの無効化画面を呼び出す。（既に無効化設定されている場合は何もしない）
//     *
//     * この機能を利用するには、AndroidManifest.xmlに
//     * "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" パーミッションの利用許可が必要
//     * @param context コンテキスト
//     */
//    public static void requestDisableDozeModeIfNeeded(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            // DOZEの無効化設定リクエスト
//            String packageName = context.getPackageName();
//            PowerManager powerManager = context.getSystemService(PowerManager.class);
//            if (powerManager == null) return;
//            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
//                // request disabling doze
//                // ユーザに 指定のアプリを Doze無効にしてもらう
//
//                @SuppressLint("BatteryLife")
//                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                intent.setData(Uri.parse("package:" + packageName));
//                context.startActivity(intent);
//            }
//        }
//    }


    //////////////////////////////////////////////////////////////////////////
    //
    // リソース系  (主にandroidのバージョンによる apiの違いを吸収するために用意)
    //

    /**
     * 指定されたリソースIDから Color値を返す
     * @param context コンテキスト
     * @param resourceId 取得するColorのリソースID
     * @return 取得されたColor値
     */
    @SuppressWarnings("deprecation")
    public static int getColor(Context context, int resourceId) {
        if (Build.VERSION.SDK_INT >= 23) {
            //API level 23以降は Contextから カラー値を参照する
            return context.getColor(resourceId);

        } else {
            // Resources経由の カラー値取得は、API level 23以降は 非推奨
            return context.getResources().getColor(resourceId);
        }
    }

    /**
     * 指定されたリソースIDから Drawableを取得
     * @param context コンテキスト
     * @param resourceId 取得するDrawableのリソースID
     * @return 取得されたDrawable
     */
    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(Context context, int resourceId) {
        if (Build.VERSION.SDK_INT >= 21) {
            //API level 21以降は Contextから Drawableを参照する
            return context.getDrawable(resourceId);

        } else {
            // Resources経由の Drawable取得は、API level 21以降は 非推奨
            return context.getResources().getDrawable(resourceId);
        }
    }

    /**
     * asset内のテキストファイルから テキストを取得する
     * @param context コンテキスト
     * @param assetFile assetsフォルダの ファイル名
     * @return 読みだされたテキスト (ファイルが空でなければ、末尾は必ず改行が付く)
     */
    public static String getAssetText(Context context, String assetFile) {
        BufferedReader bufferedReader = null;
        StringBuilder builder = new StringBuilder();

        try {
            AssetManager assetManager = context.getResources().getAssets();
            bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(assetFile)));
            String line;
            while((line = bufferedReader.readLine()) != null) {
                builder.append(line).append("\n");
            }

        } catch (IOException e) {
            return null;

        } finally {
            try {
                if (bufferedReader != null) bufferedReader.close();
            } catch (IOException ignored) {
            }
        }

        return builder.toString();
    }


    //////////////////////////////////////////////////////////////////////////
    //
    // Service関連  (notification関連も含む)
    //

    /**
     * 指定のServiceを起動する
     * @param context コンテキスト
     * @param serviceClass 起動する Serviceクラス
     */
    public static void startService(Context context, Class serviceClass) {
        // サービス起動
        Intent serviceIntent = new Intent(context, serviceClass);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }


    /**
     * notificationチャンネルを登録する (android8.0以降用)
     * @param context コンテキスト
     * @param channelId チャンネルID
     * @param channelNameId 端末の設定画面で表示されるチャンネルの表記。(多言語対応のため文字列ではなく、リソースIDで指定する)
     * @param importance 重要度の初期設定値
     */
    public static void registerNotificationChannel(Context context, String channelId, int channelNameId, int importance) {
        if (context == null) return;

        // 以下は android8.0 以降のみ有効
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = context.getString(channelNameId);
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);       // ロック画面で通知を表示するかどうか

            // 端末にチャンネルを登録する。端末設定で表示される
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * notificationチャンネルを登録する (android8.0以降用)
     * @param context コンテキスト
     * @param channelId チャンネルID
     * @param channelNameId 端末の設定画面で表示されるチャンネルの表記。(多言語対応のため文字列ではなく、リソースIDで指定する)
     */
    public static void registerNotificationChannel(Context context, String channelId, int channelNameId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNotificationChannel(context, channelId, channelNameId, NotificationManager.IMPORTANCE_DEFAULT);
        }
    }

}
