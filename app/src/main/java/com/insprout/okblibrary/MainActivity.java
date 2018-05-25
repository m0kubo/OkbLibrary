package com.insprout.okblibrary;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.insprout.okblib.network.HttpRequest;
import com.insprout.okblib.network.HttpRequestTask;
import com.insprout.okblib.network.HttpResponse;
import com.insprout.okblib.ui.DialogUi;
import com.insprout.okblib.util.UiUtils;

public class MainActivity extends Activity implements DialogUi.DialogEventListener {
    private final static int RC_DLG_BUTTON3 = 300;
    private final static int RC_DLG_CUSTOM = 201;
    private final static int RC_DLG_CUSTOM2 = 202;
    private final static int RC_DLG_LIST_TAP = 400;
    private final static int RC_DLG_LIST_SINGLE_CHOICE = 401;
    private final static int RC_DLG_LIST_MULTI_CHOICE = 402;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btn_request:
                httpRequest();
                break;

            case R.id.btn_dialog1:
                new DialogUi.Builder(this)
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle("タイトル")
                        .setMessage("メッセージ")
                        .setPositiveButton(android.R.string.ok)
                        .show();
                break;

            case R.id.btn_dialog3:
                // DialogUiのリスナーは Activityに DialogUi.DialogEventListenerインターフェースを実装します
                // リスナーは Activityに １つの実装となるので ダイアログが複数ある場合は、requestCodeを指定してそれで判別する
                // requestCodeは 正の値を指定する
                new DialogUi.Builder(this)
                        .setTitle("3ボタンのダイアログ")
                        .setMessage("メッセージ")
                        .setPositiveButton("はい")
                        .setNegativeButton("いいえ")
                        .setNeutralButton("中立")
                        .setRequestCode(RC_DLG_BUTTON3)
                        .show();
                break;

            case R.id.btn_dialog_list:
                new DialogUi.Builder(this)
                        .setTitle("タイトル")
                        .setNegativeButton(android.R.string.cancel)
                        .setItems(R.array.choice_list)
                        .setRequestCode(RC_DLG_LIST_TAP)
                        .show();
                break;

            case R.id.btn_dialog_list_single:
                new DialogUi.Builder(this)
                        .setTitle("タイトル")
                        .setPositiveButton(android.R.string.ok)
                        .setNegativeButton(android.R.string.cancel)
                        .setSingleChoiceItems(R.array.choice_list, -1)
                        .setRequestCode(RC_DLG_LIST_SINGLE_CHOICE)
                        .show();
                break;

            case R.id.btn_dialog_list_multi:
                new DialogUi.Builder(this)
                        .setTitle("タイトル")
                        .setPositiveButton(android.R.string.ok)
                        .setNegativeButton(android.R.string.cancel)
                        .setMultiChoiceItems(R.array.choice_list, null)
                        .setRequestCode(RC_DLG_LIST_MULTI_CHOICE)
                        .show();
                break;

            case R.id.btn_dialog_custom:
                new DialogUi.Builder(this)
                        .setTitle("カスタムダイアログ")
                        .setMessage("NumberPicker使用例")
                        .setView(R.layout.dlg_number_picker)
                        .setPositiveButton("OK")
                        .setNegativeButton("キャンセル")
                        .setRequestCode(RC_DLG_CUSTOM)
                        .show();
                break;

            case R.id.btn_dialog_custom2:
                new DialogUi.Builder(this)
                        .setTitle("カスタムダイアログ")
                        .setMessage("EditText使用例")
                        .setView(R.layout.dlg_edittext)
                        .setPositiveButton("OK")
                        .setNegativeButton("キャンセル")
                        .setRequestCode(RC_DLG_CUSTOM2)
                        .show();
                break;
        }
    }

    private void httpRequest() {
        String url = UiUtils.getText(this, R.id.et_url);
        if (url == null || url.isEmpty()) return;

        final DialogFragment progressDialog = new DialogUi.Builder(this, DialogUi.STYLE_PROGRESS_DIALOG).setMessage("message").show();
        // httpリクエストの responseが返るまで、ボタンを無効にしておく
        UiUtils.enableView(this, R.id.btn_request, false);
        new HttpRequestTask(HttpRequest.METHOD_GET, url, null, new HttpRequestTask.OnResponseListener() {
            @Override
            public void onResponse(HttpResponse response) {
                progressDialog.dismissAllowingStateLoss();
                UiUtils.enableView(MainActivity.this, R.id.btn_request, true);

                String msg;
                int httpStatus = response.getHttpStatus();
                if (httpStatus == 200) {
                    msg = response.getResponseBody();
                } else {
                    msg = "http status: " + httpStatus;
                }
                new DialogUi.Builder(MainActivity.this).setTitle("通信結果").setMessage(msg).setPositiveButton().show();
            }
        }).execute();
    }

    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        // 複数のダイアログのリスナーとなるので、どのダイアログからのコールバックかを requestCodeで判別する
        switch(requestCode) {
            case RC_DLG_BUTTON3:
                switch (which) {
                    case DialogUi.EVENT_BUTTON_POSITIVE:
                        Toast.makeText(this, "「はい」ボタンが押されました", Toast.LENGTH_SHORT).show();
                        break;
                    case DialogUi.EVENT_BUTTON_NEGATIVE:
                        Toast.makeText(this, "「いいえ」ボタンが押されました", Toast.LENGTH_SHORT).show();
                        break;
                    case DialogUi.EVENT_BUTTON_NEUTRAL:
                        Toast.makeText(this, "「中立」ボタンが押されました", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;

            case RC_DLG_LIST_TAP:
                if (which >= 0) {
                    // リストから選択された
                    Object selected = ((ListView)view).getItemAtPosition(which);
                    Toast.makeText(this, "選択：" + selected.toString(), Toast.LENGTH_SHORT).show();
                }
                break;

            case RC_DLG_LIST_SINGLE_CHOICE:
                switch (which) {
                    case DialogUi.EVENT_DIALOG_SHOWN:
                        // 例として、項目が選択されるまで、OKボタンを無効にする
                        // Dialog#getButton()は ダイアログがshowされるまでnullなので、EVENT_DIALOG_CREATEでは使用しない
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(((ListView)view).getCheckedItemPosition() >= 0);
                        break;

                    case DialogUi.EVENT_BUTTON_POSITIVE:
                        String selectItem = "（なし）";
                        int position = ((ListView)view).getCheckedItemPosition();
                        if (position >= 0) {
                            selectItem = ((ListView)view).getItemAtPosition(position).toString();
                        }
                        Toast.makeText(this, "選択：" + selectItem, Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        if (which >= 0) {
                            // リストの項目がタップされた
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(((ListView)view).getCheckedItemPosition() >= 0);
                        }
                }
                break;

            case RC_DLG_LIST_MULTI_CHOICE:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    String selectItems = "（なし）";

                    if (((ListView)view).getCheckedItemCount() > 0) {
                        selectItems = "";
                        int selectCount = 0;
                        SparseBooleanArray positions = ((ListView) view).getCheckedItemPositions();
                        // SparseBooleanArrayは全件分の情報があるのではなく、選択操作があった項目のみの情報
                        // (初期値で選択指定された項目もを含む)
                        for (int i = 0; i < positions.size(); i++) {
                            // 一度選択してその後解除した項目も含まれるので、選択状態は確認する必要がある
                            int key = positions.keyAt(i);
                            if (positions.get(key)) {
                                if (selectCount++ > 0) selectItems += "、";
                                selectItems += ((ListView) view).getItemAtPosition(key).toString();
                            }
                        }
                    }
                    Toast.makeText(this, "選択：" + selectItems, Toast.LENGTH_SHORT).show();

                } else if (which == DialogUi.EVENT_DIALOG_SHOWN || which >= 0) {
                    // 実装例として、項目が選択されるまで、OKボタンを無効にする

                    // 選択されていない場合は OKボタンを無効にする
                    // Dialog#getButton()は ダイアログがshowされるまでnullなので、EVENT_DIALOG_CREATEでは使用しない
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(((ListView)view).getCheckedItemCount() > 0);
                }
                break;

            case RC_DLG_CUSTOM:
                NumberPicker picker = view.findViewById(R.id.number_piker);
                switch (which) {
                    // 必要があれば、カスタムダイアログの初期化処理を行う
                    case DialogUi.EVENT_DIALOG_CREATED:
                        String[] selection = getResources().getStringArray(R.array.choice_list);
                        picker.setMaxValue(selection.length - 1);               // 選択最大値設定
                        picker.setMinValue(0);                                  // 選択最小値設定
                        picker.setDisplayedValues(selection);                   // 表示用ラベル設定
                        picker.setWrapSelectorWheel(true);                     // ホイール循環設定
                        //picker.setValue(selection.length - 1);                  // リストの最後の項目を選択
                        break;

                    // OKボタン押下処理
                    case DialogUi.EVENT_BUTTON_POSITIVE:
                        int index = picker.getValue() - picker.getMinValue();
                        Toast.makeText(this, "選択：" + picker.getDisplayedValues()[ index ], Toast.LENGTH_SHORT).show();
                        break;
                }
                break;

            case RC_DLG_CUSTOM2:
                EditText editText = view.findViewById(R.id.et_dlg);
                switch (which) {
                    case DialogUi.EVENT_DIALOG_SHOWN:
                        if (editText != null) {
                            // EditTextがカラの場合 OKボタンを無効にする
                            // Dialog#getButton()は、Dialogが show()された後でないと、Buttonのインスタンスが取得できないので、
                            // DialogUi.EVENT_DIALOG_SHOWNイベントで呼び出す
                            final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            button.setEnabled(editText.getText().length() > 0);
                            editText.addTextChangedListener(new TextWatcher() {
                                @Override
                                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                }

                                @Override
                                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                }

                                @Override
                                public void afterTextChanged(Editable editable) {
                                    button.setEnabled(editable.length() > 0);
                                }
                            });
                            // キーボードを自動で開く
                            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (inputMethodManager != null) {
                                inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                        break;

                    case DialogUi.EVENT_BUTTON_POSITIVE:
                        // 入力されたタグを登録
                        if (editText != null) {
                            Toast.makeText(this, "入力：" + editText.getText(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                break;
        }
    }
}
