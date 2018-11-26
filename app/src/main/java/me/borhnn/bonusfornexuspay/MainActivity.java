package me.borhnn.bonusfornexuspay;

import android.Manifest;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity {

    private static final int READ_SMS = 5556;
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextViewHandler textViewHandler;
    private ArrayList<BonusData> allBonuses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.text);
        textViewHandler = new TextViewHandler(textView);
        allBonuses = new ArrayList<>();
        readSms();
        float total = 0;

        for (BonusData bonus : allBonuses) {
            total += bonus.bonusAmount;
        }
        textView.setText("৳ " + total);
    }

    @AfterPermissionGranted(READ_SMS)
    private void readSms() {
        String perms = Manifest.permission.READ_SMS;
        if (EasyPermissions.hasPermissions(this, perms)) {
            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
            Pattern pattern = Pattern.compile("address:16216 .* NexusPay Loyalty Card (\\**\\d*).* credited\\(Cash Back-Purchase\\) by BDT (\\d*.\\d{2}) on (\\d*-\\d*-\\d* \\d*:\\d*:\\d* [AP]M)\\.");
            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    String msgData = "";
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                    }

                    Matcher matcher = pattern.matcher(msgData);
                    if (matcher.find()) {
                        BonusData bonusData = new BonusData();
                        bonusData.setCardNumber(matcher.group(1));
                        bonusData.setBonusAmount(Float.parseFloat(matcher.group(2)));
                        bonusData.setIssueDate(matcher.group(3));
                        Log.d(TAG, "readSms: " + bonusData);
                        allBonuses.add(bonusData);
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

    private class BonusData {
        String cardNumber;
        float bonusAmount;
        Date issueDate;

        public BonusData(String cardNumber, float bonusAmount, Date issueDate) {
            this.cardNumber = cardNumber;
            this.bonusAmount = bonusAmount;
            this.issueDate = issueDate;
        }

        public BonusData(String cardNumber, String bonusAmount, String issueDate) {

        }

        public BonusData() {
        }

        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        public void setBonusAmount(float bonusAmount) {
            this.bonusAmount = bonusAmount;
        }

        public void setIssueDate(String issueDate) {
        }

        public void setIssueDate(Date issueDate) {
            this.issueDate = issueDate;
        }

        @Override
        public String toString() {
            return "BonusData{" +
                    "cardNumber='" + cardNumber + '\'' +
                    ", bonusAmount=" + bonusAmount +
                    ", issueDate=" + issueDate +
                    '}';
        }
    }
}
