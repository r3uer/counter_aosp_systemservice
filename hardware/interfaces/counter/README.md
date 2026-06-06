# Counter HAL — AIDL HAL Reference

A complete, minimal **AIDL HAL** in the modern Android style (NDK backend,
VINTF stability, default vendor implementation). It pairs with the
`system/counter` Java system service in this template so you can see the full
**framework service → HAL → vendor impl** path.

```
App / framework code
        │
        ▼
CounterService.java        (Java, runs in system_server, @SystemService)
        │ uses
        ▼
CounterHalClient.java      (Java, looks up "android.hardware.counter.ICounter/default")
        │ AIDL
        ▼
/vendor/bin/hw/android.hardware.counter-service
        │
        ▼
Counter.cpp (BnCounter impl, in vendor partition)
```

---

## Layout

```
hardware/interfaces/counter/
├── Android.bp                          # top-level (no targets; subdirs own build)
├── METADATA
├── aidl/
│   ├── Android.bp                      # aidl_interface (NDK + Java, vintf)
│   └── android/hardware/counter/ICounter.aidl
├── default/
│   ├── Android.bp
│   ├── Counter.h                       # BnCounter impl header
│   ├── Counter.cpp                     # BnCounter impl
│   ├── service.cpp                     # binder service main
│   └── android.hardware.counter-service.rc
├── sepolicy/
│   ├── Android.bp
│   ├── hal_counter_default.te
│   ├── hal_counter_default_contexts
│   └── hal_counter_default_property_contexts
├── manifests/
│   ├── Android.bp
│   └── Android.hardware.counter-service.xml
├── tools/
│   ├── Android.bp
│   └── hal_counter_client.cpp          # adb-style C++ test client
└── java/
    ├── Android.bp
    └── android/hardware/counter/CounterHalClient.java
```

---

## Where things go in AOSP

| Local path                                                     | AOSP target                       |
| -------------------------------------------------------------- | --------------------------------- |
| `hardware/interfaces/counter/`                                 | `hardware/interfaces/counter/`    |
| `frameworks-base-patches/Context.java.snippet`                 | `frameworks/base/core/java/android/content/Context.java` |
| `frameworks-base-patches/SystemServer.java.snippet`            | `frameworks/base/services/java/com/android/server/SystemServer.java` |

If you're vendoring this, put `default/`, `sepolicy/`, and `manifests/`
contents under `vendor/<vendor>/<chipset>/` instead of AOSP-tree-wide.

---

## Build

```bash
m android.hardware.counter-aidl          # NDK + Java stubs
m android.hardware.counter-impl          # vendor C++ library
m android.hardware.counter-service       # binder binary
m hal_counter_client                     # C++ test client
m android.hardware.counter-java          # Java client library
```

Vendor partition placement (`relative_install_path: "hw"`) puts the binary at
`/vendor/bin/hw/android.hardware.counter-service`.

---

## Wire it into a device

1. **Init rc** — either drop `android.hardware.counter-service.rc` in your
   vendor `init.<board>.rc`, or rely on the `init_rc` build attribute which
   Soong automatically combines into the vendor init image.

2. **VINTF** — copy `manifests/Android.hardware.counter-service.xml` into your
   device config:

   ```
   device/<vendor>/<product>/manifest.xml   (or as a fragment)
   ```

   The fragment declares the `default` instance of `ICounter` is available.

3. **Sepolicy** — install the `.te` / contexts files into your vendor sepolicy
   build. Use the `hal_counter_default` macros for allow rules and reference
   `hal_counter_default_exec` as the binary's file label.

4. **Verify the service is up:**

   ```bash
   adb shell service list | grep android.hardware.counter
   adb shell lshal | grep android.hardware.counter
   ```

---

## Talk to it from the shell (host build / C++ test client)

```bash
adb push $ANDROID_PRODUCT_OUT/system/bin/hal_counter_client /data/local/tmp/
adb shell /data/local/tmp/hal_counter_client get
adb shell /data/local/tmp/hal_counter_client inc 10
adb shell /data/local/tmp/hal_counter_client get         # -> 10
adb shell /data/local/tmp/hal_counter_client add 2 3     # -> 5
adb shell /data/local/tmp/hal_counter_client notify hi
adb shell /data/local/tmp/hal_counter_client version
```

---

## Talk to it from the framework (Java)

```java
import android.hardware.counter.CounterHalClient;

CounterHalClient hal = CounterHalClient.get();
if (hal.isAvailable()) {
    hal.increment(5);
    int n = hal.getCount();
} else {
    // HAL not present on this device — fall back to local state
}
```

`CounterHalClient.getOrWait()` blocks until the HAL is published (use it
during boot, not in latency-sensitive code).

---

## Pairing with the CounterService

In `CounterService.Lifecycle.onStart()` you can warm the HAL:

```java
import android.hardware.counter.CounterHalClient;

private final CounterHalClient mHal = CounterHalClient.get();

@Override
public void onStart() {
    publishBinderService(SERVICE_NAME, mService);
    Slog.i(TAG, "Counter HAL available=" + mHal.isAvailable());
}
```

Then in the server impl, route writes to the HAL when present:

```java
@Override
public void increment(int amount) {
    enforcePermission();
    if (mHal.isAvailable()) {
        mHal.increment(amount);
    } else {
        mCount.addAndGet(amount);
    }
}
```

This is the canonical pattern for system services that wrap a HAL: keep a
software fallback in case the device build doesn't ship the HAL.

---

## Going beyond a "default" impl

- **Multiple instances** — declare additional `<interface>` elements in the
  VINTF manifest (e.g. `default` and `fast`) and start the binary with
  `--instance fast` to register at `<descriptor>/fast`.
- **HAL death recovery** — call
  `ICounter::fromServiceName("default", /*retry=*/true)` in C++ or wire a
  `IBinder.DeathRecipient` in Java.
- **VTS** — add a `vts/` directory with `VtsHalCounterTargetTest.cpp` and a
  matching `vts/Android.bp` (not bundled in this template).

---

## Naming cheatsheet (search/replace when you fork)

| Symbol                               | Where                                           |
| ------------------------------------ | ----------------------------------------------- |
| `ICounter` / `BnCounter`             | AIDL interface + generated stub                 |
| `Counter`                            | C++ implementation class                       |
| `android.hardware.counter`           | Package + VINTF `<name>`                       |
| `android.hardware.counter-impl`      | Vendor C++ library                             |
| `android.hardware.counter-service`   | Binder binary name                             |
| `android.hardware.counter-aidl`      | AIDL build target                              |
| `android.hardware.counter-java`      | Java client library                            |
| `hal_counter_default` / `hal_counter_default_exec` | SELinux types                  |
| `vendor.counter-hal-default`         | Init service name                              |
