package me.borhnn.bonusfornexuspay;

import android.Manifest;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity {

    private static final int READ_SMS = 5556;
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextViewHandler textViewHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.text);
        textViewHandler = new TextViewHandler(textView);
        readSms();
    }

    @AfterPermissionGranted(READ_SMS)
    private void readSms() {
        String perms = Manifest.permission.READ_SMS;
        if (EasyPermissions.hasPermissions(this, perms)) {
            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
            Pattern pattern = Pattern.compile("NexusPay Loyalty Card .* credited\\(Cash Back-Purchase\\) by BDT (\\d*.\\d{2})");
            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    String msgData = "";
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                    }
                    if (msgData.contains("address:16216")) {
                        Log.d(TAG, "readSms: " + msgData);
                        Matcher matcher = pattern.matcher(msgData);
                        if (matcher.find()) {
                            textViewHandler.addText(matcher.group(1));
                        }
                    }
                } while (cursor.moveToNext());
            } else {
                // empty box, no SMS
            }
        } else {
            EasyPermissions.requestPermissions(this, "", READ_SMS, perms);
        }
    }

    private class TextViewHandler {
        private final TextView textView;

        public TextViewHandler(TextView textView) {
            this.textView = textView;
        }

        public void addText(String s) {
            textView.setText(textView.getText() + "\n" + s);
        }
    }
}
