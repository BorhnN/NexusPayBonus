package me.borhnn.bonusfornexuspay;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    public static final double BONUS_TO_MONEY = 10.0 / 3;
    private static final int READ_SMS = 5556;
    private static final String TAG = MainActivity.class.getSimpleName();
    Handler handler;
    private ArrayList<BonusData> allBonuses;
    private PackageManager pm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(getResources().getColor(android.R.color.white));
        }
        allBonuses = new ArrayList<>();
        pm = this.getPackageManager();
        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Thread thread = new Thread() {
            @Override
            public void run() {
                readSms();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showCounter();
                    }
                });
            }

        };
        thread.start();
    }

    @SuppressLint("SetTextI18n")
    private void showCounter() {
        float totalMonth = 0;
        float totalDay = 0;

        for (BonusData bonus : allBonuses) {
            if (bonus.getIssueDate().get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) {
                totalMonth += bonus.bonusAmount;
            }
            if (bonus.getIssueDate().get(Calendar.DATE) == Calendar.getInstance().get(Calendar.DATE)) {
                totalDay = bonus.bonusAmount;
            }
        }
        TextView textBonusMonth = findViewById(R.id.text_bonus_month);
        TextView textBonusDay = findViewById(R.id.text_bonus_day);
        TextView textMoneyMonth = findViewById(R.id.text_money_month);
        TextView textMoneyDay = findViewById(R.id.text_money_day);
        float bonusDayLeft = 200 - totalDay;
        float bonusMonthLeft = 500 - totalMonth;
        textBonusMonth.setText("এই মাসে বোনাস পেয়েছেন: ৳ " + totalMonth);
        textBonusDay.setText("আজকে বোনাস পেয়েছেন: ৳ " + totalDay);
        textMoneyDay.setText("আজকে বোনাসের জন্য খরচ সর্বোচ্চ: ৳ " + (Math.min(bonusDayLeft, bonusMonthLeft) * BONUS_TO_MONEY));
        textMoneyMonth.setText("এই মাসে বোনাসের জন্য খরচ সর্বোচ্চ: ৳ " + (bonusMonthLeft * BONUS_TO_MONEY));
    }


    private void readSms() {
        String perms = Manifest.permission.READ_SMS;
        if (pm.checkPermission(perms, BuildConfig.APPLICATION_ID) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "readSms: perm granted running");
            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
            Pattern pattern = Pattern.compile("address:16216 .* NexusPay Loyalty Card (\\**\\d*).* credited\\(Cash Back-Purchase\\) by BDT (\\d*.\\d{2}) on (\\d*-\\d*-\\d* \\d*:\\d*:\\d* [AP]M)\\.");
            if (cursor != null) {
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
                            //                        Log.d(TAG, "readSms: " + bonusData);
                            allBonuses.add(bonusData);
                        }

                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "readSms: No perm requesting");
                this.requestPermissions(new String[]{perms}, READ_SMS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            readSms();
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
