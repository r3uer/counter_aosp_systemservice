package android.counter;

import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.content.Context;
import android.os.ICounterService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

@SystemService(Context.COUNTER_SERVICE)
public final class CounterManager {

    private static final String TAG = "CounterManager";

    private final ICounterService mService;
    private final Context mContext;

    public CounterManager(Context context, ICounterService service) {
        mContext = context;
        mService = service;
    }

    public static ICounterService getService() {
        return ICounterService.Stub.asInterface(
                ServiceManager.getServiceOrThrow(Context.COUNTER_SERVICE));
    }

    @RequiresPermission(android.Manifest.permission.USE_COUNTER_SERVICE)
    public int getCount() {
        try {
            return mService.getCount();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @RequiresPermission(android.Manifest.permission.USE_COUNTER_SERVICE)
    public void increment(int amount) {
        try {
            mService.increment(amount);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @RequiresPermission(android.Manifest.permission.USE_COUNTER_SERVICE)
    public void reset() {
        try {
            mService.reset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int add(int a, int b) {
        try {
            return mService.add(a, b);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyEvent(String event) {
        try {
            mService.notifyEvent(event);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyEvent failed: " + e);
        }
    }
}
