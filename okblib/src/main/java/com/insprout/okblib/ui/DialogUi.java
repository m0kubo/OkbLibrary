package com.insprout.okblib.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * Created by okubo on 2018/03/05.
 * AlertDialogを利用するための クラス
 */

public class DialogUi {

    //////////////////////////
    //
    // public 定数
    //

    // ダイアログのタイプ
    public static final int STYLE_ALERT_DIALOG = -1;           // 通常ダイアログスタイル
    public static final int STYLE_PROGRESS_DIALOG = 0;         // ProgressDialog風 スタイル

    // interface用 public 定数
    public static final int EVENT_BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
    public static final int EVENT_BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;
    public static final int EVENT_BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL;
    public static final int EVENT_DIALOG_CREATED = -100;
    public static final int EVENT_DIALOG_SHOWN = -101;



    //////////////////////////
    //
    // 内部使用 private 定数
    //

    // 引数受け渡し用のキー
    private static final String KEY_REQUEST_CODE = "dialog.REQUEST_CODE";
    private static final String KEY_DIALOG_TITLE = "dialog.TITLE";
    private static final String KEY_DIALOG_TEXT = "dialog.TEXT";
    private static final String KEY_DIALOG_LAYOUT = "dialog.LAYOUT_ID";
    private static final String KEY_DIALOG_ICON = "dialog.ICON_ID";
    private static final String KEY_DIALOG_POSITIVE_BUTTON_TEXT = "button.positive.TEXT";
    private static final String KEY_DIALOG_NEGATIVE_BUTTON_TEXT = "button.negative.TEXT";
    private static final String KEY_DIALOG_NEUTRAL_BUTTON_TEXT = "button.neutral.TEXT";
    private static final String KEY_CHOICE_TYPE = "choice.TYPE";
    private static final String KEY_CHOICE_ARRAY = "choice.ARRAY";
    private static final String KEY_CHOICE_SELECTED = "choice.SELECTED";
    private static final String KEY_CHOICE_SELECTED_ITEMS = "choice.SELECTED_ITEMS";
    private static final String KEY_PROGRESS_STYLE = "progress.STYLE";

    // ListViewの選択方法
    private static final int LIST_TYPE_NO_CHOICE = 0;           // ListView (checkBoxなし)
    private static final int LIST_TYPE_SINGLE_CHOICE = 1;       // ListView (単一選択)
    private static final int LIST_TYPE_MULTI_CHOICE = 2;        // ListView (複数選択)

    // パラメータ省略時のデフォルト値
    private static final int REQUEST_CODE_DEFAULT = -1;
    private static final int ID_LAYOUT_DEFAULT = -1;
    private static final int ID_STRING_DEFAULT_OK = android.R.string.ok;
    private static final int ID_STRING_DEFAULT_CANCEL = android.R.string.cancel;

    // Fragmentの タグの接頭句
    private static final String TAG_PREFIX = "DIALOG_";


    //////////////////////////
    //
    // interface
    //

    public interface DialogEventListener {
        /**
         * AlertDialogの イベントを通知する
         *
         * whichは DialogInterface.OnClickListenerなどで返されるwhichの値
         * (POSITIVEボタン押下：-1、NEGATIVEボタン押下：-2、NEUTRALボタン押下：-3、リスト項目番号：0以上の整数)に
         * 加え、DialogFragment created：-100、DialogFragment shown：-101 が返される
         *
         * Listenerに渡される viewは Dialogの形態によって内容が異なる
         * - メッセージと ダイアログ標準ボタンのみの場合、常にnull
         * - 選択リストが設定されている場合、選択リストのListViewオブジェクト
         * - カスタムViewが設定されている場合、そのViewオブジェクト
         */
        void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view);
    }


    /**
     * Dialog作成時に与えたリクエストコードを指定して Dialogを 閉じる
     * @param activity 呼び出すActivity
     * @param requestCode 作成時に設定した リクエストコード
     */
    public static void dismissDialog(Activity activity, int requestCode) {
        FragmentManager manager = getFragmentManager(activity);
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(getFragmentTag(requestCode));
        if (dialog != null) dialog.dismissAllowingStateLoss();
    }

    public static String getFragmentTag(int requestCode) {
        return TAG_PREFIX + Integer.toHexString(requestCode);
    }


    //////////////////////////
    //
    // private functions

    private static FragmentManager getFragmentManager(Activity activity) {
        // Fragment関連の import宣言  API level11以降のみのサポートの場合
        return activity.getFragmentManager();
    }
    // Support Libraryの FragmentActivity もしくは AppCompatActivityを使用する場合
// （AppCompatActivityは FragmentActivityを継承している）
//    private static FragmentManager getFragmentManager(Activity activity) {
//        //Fragment関連の import宣言  API level10以前をサポートする場合 (support-v4ライブラリ必須)
//        if (activity instanceof FragmentActivity) {
//            return ((FragmentActivity)activity).getSupportFragmentManager();
//        }
//        return null;
//    }

    private static String getString(Context context, int resourceId) {
        if (resourceId <= 0) return null;
        return context.getString(resourceId);
    }

    private static String[] getStringArrays(Context context, int resourceId) {
        if (resourceId <= 0) return null;
        return context.getResources().getStringArray(resourceId);
    }


    ////////////////////////////////////
    //
    // Builderクラス
    //

    public static class Builder {
        private Activity mActivity;

        private int mRequestCode = REQUEST_CODE_DEFAULT;
        private int mLayoutId = ID_LAYOUT_DEFAULT;
        private int mDialogStyle = STYLE_ALERT_DIALOG;

        private String mTitle = null;
        private String mMessage = null;
        private String mLabelPositive = null;
        private String mLabelNegative = null;
        private String mLabelNeutral = null;

        private int mListType = LIST_TYPE_SINGLE_CHOICE;
        private String[] mListItems = null;
        private int mCheckedItem = -1;
        private boolean[] mCheckedList = null;
        private int mIconId = -1;


        public Builder(Activity activity) {
            mActivity = activity;
        }

        public Builder(Activity activity, int dialogStyle) {
            mActivity = activity;
            mDialogStyle = dialogStyle;
        }

        public Builder setRequestCode(int requestCode) {
            mRequestCode = requestCode;
            return this;
        }

        public Builder setView(int layoutId) {
            mLayoutId = layoutId;
            return this;
        }

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setTitle(int titleId) {
            return setTitle(getString(mActivity, titleId));
        }

        public Builder setMessage(String message) {
            mMessage = message;
            return this;
        }

        public Builder setMessage(int messageId) {
            return setMessage(getString(mActivity, messageId));
        }

        public Builder setIcon(int iconId) {
            mIconId = iconId;
            return this;
        }

        public Builder setPositiveButton(String text) {
            mLabelPositive = text;
            return this;
        }

        public Builder setPositiveButton(int textId) {
            return setPositiveButton(getString(mActivity, textId));
        }

        public Builder setPositiveButton() {
            return setPositiveButton(getString(mActivity, ID_STRING_DEFAULT_OK));
        }

        public Builder setNegativeButton(String text) {
            mLabelNegative = text;
            return this;
        }

        public Builder setNegativeButton(int textId) {
            return setNegativeButton(getString(mActivity, textId));
        }

        public Builder setNegativeButton() {
            return setNegativeButton(getString(mActivity, ID_STRING_DEFAULT_CANCEL));
        }

        public Builder setNeutralButton(String text) {
            mLabelNeutral = text;
            return this;
        }

        public Builder setNeutralButton(int textId) {
            return setNeutralButton(getString(mActivity, textId));
        }

        public Builder setItems(String[] items) {
            mListType = LIST_TYPE_NO_CHOICE;
            mListItems = items;
            return this;
        }

        public Builder setItems(int itemsId) {
            return setItems(getStringArrays(mActivity, itemsId));
        }

        public Builder setSingleChoiceItems(String[] items, int checkedItem) {
            mListType = LIST_TYPE_SINGLE_CHOICE;
            mListItems = items;
            mCheckedItem = checkedItem;
            return this;
        }

        public Builder setSingleChoiceItems(int itemsId, int checkedItem) {
            return setSingleChoiceItems(getStringArrays(mActivity, itemsId), checkedItem);
        }

        public Builder setMultiChoiceItems(String[] items, boolean[] checkedItems) {
            mListType = LIST_TYPE_MULTI_CHOICE;
            mListItems = items;
            mCheckedList = checkedItems;
            return this;
        }

        public Builder setMultiChoiceItems(int itemsId, boolean[] checkedItems) {
            return setMultiChoiceItems(getStringArrays(mActivity, itemsId), checkedItems);
        }

        public DialogFragment create() {
            DialogUiFragment dialogFragment = new DialogUiFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, mRequestCode);
            args.putInt(KEY_DIALOG_LAYOUT, mLayoutId);
            args.putInt(KEY_PROGRESS_STYLE, mDialogStyle);
            // ダイアログ共通設定
            args.putString(KEY_DIALOG_TITLE, mTitle);
            args.putString(KEY_DIALOG_TEXT, mMessage);
            args.putInt(KEY_DIALOG_ICON, mIconId);
            args.putString(KEY_DIALOG_POSITIVE_BUTTON_TEXT, mLabelPositive);
            args.putString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT, mLabelNegative);
            args.putString(KEY_DIALOG_NEUTRAL_BUTTON_TEXT, mLabelNeutral);
            // ListViewダイアログ用設定
            args.putInt(KEY_CHOICE_TYPE, mListType);
            args.putStringArray(KEY_CHOICE_ARRAY, mListItems);
            args.putInt(KEY_CHOICE_SELECTED, mCheckedItem);
            args.putBooleanArray(KEY_CHOICE_SELECTED_ITEMS, mCheckedList);

            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        public DialogFragment show() {
            DialogFragment dialogFragment = create();
            //FragmentManager magager = mActivity.getFragmentManager();
            FragmentManager manager = getFragmentManager(mActivity);
            dialogFragment.show(manager, getFragmentTag(mRequestCode));
            return dialogFragment;
        }

    }


    ////////////////////////////////////
    //
    // 基本DialogFragmentクラス
    //

    public static class DialogUiFragment extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnShowListener, DialogInterface.OnMultiChoiceClickListener {
        private final static float DIP_PADDING_PROGRESS = 15.0f;

        private AlertDialog mAlertDialog = null;
        private int mStyle = STYLE_ALERT_DIALOG;
        private View mCustomView = null;
        private DialogEventListener mListener = null;
        private int mRequestCode;
        private String[] mChoiceList;


        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            if (context instanceof DialogEventListener) {
                mListener = (DialogEventListener) context;
            }
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if (activity instanceof DialogEventListener) {
                mListener = (DialogEventListener) activity;
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mRequestCode = getArguments().getInt(KEY_REQUEST_CODE);
            mStyle = getArguments().getInt(KEY_PROGRESS_STYLE, STYLE_ALERT_DIALOG);
            String title = getArguments().getString(KEY_DIALOG_TITLE);
            String message = getArguments().getString(KEY_DIALOG_TEXT);
            int iconId = getArguments().getInt(KEY_DIALOG_ICON);
            String buttonOk = getArguments().getString(KEY_DIALOG_POSITIVE_BUTTON_TEXT);
            String buttonCancel = getArguments().getString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT);
            String buttonNeutral = getArguments().getString(KEY_DIALOG_NEUTRAL_BUTTON_TEXT);

            mChoiceList = getArguments().getStringArray(KEY_CHOICE_ARRAY);
            int listType = getArguments().getInt(KEY_CHOICE_TYPE, LIST_TYPE_NO_CHOICE);
            int selected = getArguments().getInt(KEY_CHOICE_SELECTED, -1);
            boolean[] selectedArray = getArguments().getBooleanArray(KEY_CHOICE_SELECTED_ITEMS);
            int layoutId = getArguments().getInt(KEY_DIALOG_LAYOUT, ID_LAYOUT_DEFAULT);
            LayoutInflater inflater;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (title != null) builder.setTitle(title);
            if (iconId > 0) builder.setIcon(iconId);
            if (mStyle == STYLE_PROGRESS_DIALOG) {
                builder.setView(buildProgressDialog(getActivity(), message));
                // messageは ProgressDialog(風の)Viewで表示するので、AlertDialogオリジナルの setMessage()を行わないようにする。
                message = null;

            } else if (layoutId != ID_LAYOUT_DEFAULT
                    && (inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)) != null) {
                // カスタムViewを設定
                mCustomView = inflater.inflate(layoutId, null);
                builder.setView(mCustomView);

            } else if (mChoiceList != null) {
                // setMessage()を実行すると setSingleChoiceItems()等が無視されるので、
                // setSingleChoiceItems()等を呼び出す場合は、setMessage()を行わないようにする。
                message = null;

                // 選択リストを設定
                switch (listType) {
                    case LIST_TYPE_SINGLE_CHOICE:
                        // checkboxつきListViewを表示
                        builder.setSingleChoiceItems(mChoiceList, selected, this);
                        break;
                    case LIST_TYPE_MULTI_CHOICE:
                        if (selectedArray != null && selectedArray.length < mChoiceList.length) {
                            // multiChoiceListの場合、初期選択状態を示す配列は選択項目数分指定していないと例外でおちる
                            // 不足している場合は落ちないように、配列の要素を増やす
                            boolean[] selectedNew = new boolean[ mChoiceList.length ];
                            System.arraycopy(selectedArray, 0, selectedNew, 0, selectedArray.length);
                            selectedArray = selectedNew;
                        }
                        builder.setMultiChoiceItems(mChoiceList, selectedArray, this);
                        break;
                    default:
                        // checkboxなしListViewを表示
                        builder.setItems(mChoiceList, this);
                        break;
                }
            }
            if (message != null) builder.setMessage(message);   // messageと カスタムViewは両立する

            // ダイアログボタンを設定
            if (buttonOk != null) builder.setPositiveButton(buttonOk, this);
            if (buttonCancel != null) builder.setNegativeButton(buttonCancel, this);
            if (buttonNeutral != null) builder.setNeutralButton(buttonNeutral, this);
            this.setCancelable(buttonCancel != null);        // キャンセルボタンがない場合は、Backキーによるキャンセルも無効
            mAlertDialog = builder.create();

            mAlertDialog.setCanceledOnTouchOutside(false);
            // Dialogが createされた事をListenerに通知する。(主にカスタムViewの初期化処理のため)
            callbackToListener(EVENT_DIALOG_CREATED);
            mAlertDialog.setOnShowListener(this);

            return mAlertDialog;
        }

        @Override
        public void dismiss() {
            dismissAllowingStateLoss();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            callbackToListener(which);
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int which, boolean b) {
            callbackToListener(which);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            callbackToListener(EVENT_BUTTON_NEGATIVE);
        }

        private void callbackToListener(int which) {
            if (mListener == null) return;

            View viewCallback = null;
            // Dialogの形態によって、callbackで返す オブジェクトを切り替える
            if (mCustomView != null) {
                // カスタムlayoutが指定されている場合は、そのViewを返す
                viewCallback = mCustomView;

            } else if (mChoiceList != null) {
                viewCallback = mAlertDialog.getListView();
            }
            mListener.onDialogEvent(mRequestCode, mAlertDialog, which, viewCallback);
        }

        @Override
        public void onShow(DialogInterface dialogInterface) {
            callbackToListener(EVENT_DIALOG_SHOWN);
        }

        private View buildProgressDialog(Context context, String message) {
            int pxPadding = (int)(DIP_PADDING_PROGRESS * getResources().getDisplayMetrics().density);

            // ダイアログのレイアウトを設定
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setVerticalGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);

            // プログレス表示 設定
            ProgressBar progress = new ProgressBar(context);
            layout.addView(progress);

            // メッセージ 設定
            TextView tv = new TextView(context);
            tv.setText(message);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setPadding(pxPadding, 0, 0, 0);          // progress表示と メッセージの間もマージンをあける
            layout.addView(tv);

            return layout;
        }
    }

}
