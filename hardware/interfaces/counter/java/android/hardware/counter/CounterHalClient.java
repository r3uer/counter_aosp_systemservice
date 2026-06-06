package android.hardware.counter;

import android.annotation.Nullable;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;
import android.util.Log;

import java.util.concurrent.locks.ReentrantLock;

public final class CounterHalClient {

    private static final String TAG = "CounterHalClient";
    private static final String DEFAULT_INSTANCE = "default";
    private static final String SERVICE_NAME =
            "android.hardware.counter.ICounter/" + DEFAULT_INSTANCE;

    @Nullable
    private static CounterHalClient sInstance;
    private static final ReentrantLock sLock = new ReentrantLock();

    @Nullable
    private final ICounter mHal;
    private final boolean mAvailable;

    private CounterHalClient(@Nullable ICounter hal, boolean available) {
        mHal = hal;
        mAvailable = available;
    }

    public static CounterHalClient get() {
        sLock.lock();
        try {
            if (sInstance != null) return sInstance;
            IBinder b = ServiceManager.checkService(SERVICE_NAME);
            if (b == null) {
                sInstance = new CounterHalClient(null, false);
                return sInstance;
            }
            sInstance = new CounterHalClient(ICounter.Stub.asInterface(b), true);
            return sInstance;
        } finally {
            sLock.unlock();
        }
    }

    public static CounterHalClient getOrWait() throws ServiceNotFoundException {
        CounterHalClient cached = get();
        if (cached.isAvailable()) return cached;
        IBinder b = ServiceManager.waitForService(SERVICE_NAME);
        if (b == null) {
            throw new ServiceNotFoundException(SERVICE_NAME);
        }
        ICounter hal = ICounter.Stub.asInterface(b);
        sLock.lock();
        try {
            sInstance = new CounterHalClient(hal, true);
            return sInstance;
        } finally {
            sLock.unlock();
        }
    }

    public boolean isAvailable() {
        return mAvailable && mHal != null;
    }

    public int getCount() {
        if (!isAvailable()) {
            throw new IllegalStateException("Counter HAL not available");
        }
        try {
            return mHal.getCount();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void increment(int amount) {
        requireHal();
        try {
            mHal.increment(amount);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reset() {
        requireHal();
        try {
            mHal.reset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int add(int a, int b) {
        if (!isAvailable()) {
            throw new IllegalStateException("Counter HAL not available");
        }
        try {
            return mHal.add(a, b);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void notifyEvent(String event) {
        if (!isAvailable()) {
            Log.w(TAG, "notifyEvent dropped (HAL unavailable): " + event);
            return;
        }
        try {
            mHal.notifyEvent(event);
        } catch (RemoteException e) {
            Log.w(TAG, "notifyEvent failed: " + e);
        }
    }

    private void requireHal() {
        if (!isAvailable()) {
            throw new IllegalStateException("Counter HAL not available");
        }
    }
}
