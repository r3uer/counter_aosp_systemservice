package com.example.counterclient;

import android.app.Activity;
import android.counter.CounterManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "CounterClient";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CounterManager cm = (CounterManager) getSystemService(COUNTER_SERVICE);
        if (cm == null) {
            Log.e(TAG, "CounterManager unavailable");
            finish();
            return;
        }

        cm.increment(5);
        cm.increment(3);
        int count = cm.getCount();
        Log.i(TAG, "count=" + count);
        cm.notifyEvent("client connected");

        TextView tv = new TextView(this);
        tv.setText("counter=" + count + " (a+b=" + cm.add(7, 8) + ")");
        setContentView(tv);
    }
}
