package me.borhnn.bonusfornexuspay;

import android.Manifest;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity {

    private static final int READ_SMS = 5556;
    private static final String TAG = MainActivity.class.getSimpleName();
    private ArrayList<BonusData> allBonuses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.text);
        allBonuses = new ArrayList<>();
        readSms();
        float total = 0;

        for (BonusData bonus : allBonuses) {
            if (bonus.getIssueDate().get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                total += bonus.bonusAmount;
            }
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
                    StringBuilder msgData = new StringBuilder();
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        msgData.append(" ").append(cursor.getColumnName(idx)).append(":").append(cursor.getString(idx));
                    }

                    Matcher matcher = pattern.matcher(msgData.toString());
                    if (matcher.find()) {
                        BonusData bonusData = new BonusData();
                        bonusData.setCardNumber(matcher.group(1));
                        bonusData.setBonusAmount(Float.parseFloat(matcher.group(2)));
                        bonusData.setIssueDate(matcher.group(3));
                        Log.d(TAG, "readSms: " + bonusData);
                        allBonuses.add(bonusData);
                    }

                } while (cursor.moveToNext());
            }
            cursor.close();
        } else {
            EasyPermissions.requestPermissions(this, "", READ_SMS, perms);
        }
    }

    private class BonusData {
        String cardNumber;
        float bonusAmount;
        Calendar issueDate;

        BonusData() {
        }

        void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }

        void setBonusAmount(float bonusAmount) {
            this.bonusAmount = bonusAmount;
        }

        void setIssueDate(String issueDate) {
            SimpleDateFormat parser = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa", Locale.getDefault());
            Calendar date = Calendar.getInstance();
            try {
                date.setTime(parser.parse(issueDate));
                setIssueDate(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Calendar getIssueDate() {
            return issueDate;
        }

        void setIssueDate(Calendar issueDate) {
            this.issueDate = issueDate;
        }

        @NonNull
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
