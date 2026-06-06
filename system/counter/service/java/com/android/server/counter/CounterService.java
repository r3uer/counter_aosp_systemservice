package com.android.server.counter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.ICounterService;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.util.Slog;

import com.android.server.SystemService;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

public final class CounterService extends ICounterService.Stub {

    private static final String TAG = "CounterService";
    private static final String SERVICE_NAME = "counter";

    public static final class Lifecycle extends SystemService {
        private final CounterService mService;

        public Lifecycle(Context context) {
            super(context);
            mService = new CounterService(context);
        }

        @Override
        public void onStart() {
            publishBinderService(SERVICE_NAME, mService);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase == SystemService.PHASE_BOOT_COMPLETED) {
                Slog.i(TAG, "CounterService boot completed");
            }
        }
    }

    private final Context mContext;
    private final AtomicInteger mCount = new AtomicInteger(0);

    CounterService(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mCount.get();
    }

    @Override
    public void increment(int amount) {
        enforcePermission();
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        mCount.addAndGet(amount);
        Slog.d(TAG, "increment(" + amount + ") -> " + mCount.get());
    }

    @Override
    public void reset() {
        enforcePermission();
        mCount.set(0);
        Slog.d(TAG, "reset()");
    }

    @Override
    public int add(int a, int b) {
        return Math.addExact(a, b);
    }

    @Override
    public void notifyEvent(String event) {
        Slog.d(TAG, "notifyEvent(" + event + ") from uid=" + Binder.getCallingUid());
    }

    private void enforcePermission() {
        mContext.enforceCallingOrSelfPermission(
                android.Manifest.permission.USE_COUNTER_SERVICE, TAG);
    }

    @Override
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err,
            String[] args, ShellCallback callback, ResultReceiver result) {
        new CounterShellCommand(this).exec(this, in, out, err, args, callback, result);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mContext.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission denied");
            return;
        }
        pw.println("CounterService state:");
        pw.println("  count=" + mCount.get());
        pw.println("  impl=" + getClass().getName());
    }
}
