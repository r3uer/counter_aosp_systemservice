# AOSP CounterService — Complete System Service Template

A minimal, end-to-end reference for adding a new system service to AOSP,
plus a paired **AIDL HAL** with default vendor implementation. The two are
designed to compose: a Java system service running in `system_server` can
delegate work to a C++ HAL running in the vendor partition.

It demonstrates:

- **AIDL** (stable, vintf) interface compiled with the modern `aidl_interface` rule.
- **Java in-process server** that lives in `system_server` (the same pattern as `BatteryStatsService`).
- **Public Java client API** (`CounterManager`) annotated with `@SystemService` and a `Context.COUNTER_SERVICE` constant.
- **Shell command** (`cmd counter ...`) for adb testing.
- **SELinux** file/property contexts.
- **Build wiring** (`Android.bp` for service, client, and AIDL).
- **Integration patches** to the four `frameworks/base` files you must touch.
- **Example client** activity.
- **Paired AIDL HAL** (`hardware/interfaces/counter/`) with default C++ impl,
  NDK + Java backends, VINTF manifest, init rc, sepolicy, and a C++ test
  client.
- **Java HAL wrapper** (`CounterHalClient`) the system service uses to talk
  to the HAL.

This template is intentionally Java-only and in-process. See *Going native* below
for converting it to a separate C++ binder process.

---

## Layout

```
aosp-counter-service/
├── README.md
├── system/counter/                                  # framework-side system service
│   ├── METADATA
│   ├── aidl/
│   │   ├── Android.bp                               # aidl_interface rule
│   │   └── android/os/ICounterService.aidl
│   ├── service/
│   │   ├── Android.bp
│   │   └── java/com/android/server/counter/
│   │       ├── CounterService.java                  # the Binder stub impl
│   │       └── CounterShellCommand.java             # adb shell cmd counter ...
│   ├── client/
│   │   ├── Android.bp
│   │   └── java/android/counter/CounterManager.java
│   └── sepolicy/
│       ├── Android.bp
│       ├── counter_service.te
│       ├── counter_service_contexts
│       └── counter_service_property_contexts
├── hardware/interfaces/counter/                      # vendor-side AIDL HAL
│   ├── Android.bp                                   # top-level (no targets)
│   ├── METADATA
│   ├── aidl/
│   │   ├── Android.bp                               # aidl_interface (NDK + Java)
│   │   └── android/hardware/counter/ICounter.aidl
│   ├── default/
│   │   ├── Android.bp                               # impl lib + binder binary
│   │   ├── Counter.h
│   │   ├── Counter.cpp                              # BnCounter impl
│   │   ├── service.cpp                              # binder service main
│   │   └── android.hardware.counter-service.rc
│   ├── sepolicy/
│   │   ├── Android.bp
│   │   ├── hal_counter_default.te
│   │   ├── hal_counter_default_contexts
│   │   └── hal_counter_default_property_contexts
│   ├── manifests/
│   │   ├── Android.bp
│   │   └── Android.hardware.counter-service.xml     # VINTF fragment
│   ├── tools/
│   │   ├── Android.bp
│   │   └── hal_counter_client.cpp                   # C++ test client
│   └── java/
│       ├── Android.bp
│       └── android/hardware/counter/CounterHalClient.java
├── frameworks-base-patches/
│   ├── AndroidManifest.xml.snippet                  # permission
│   ├── Context.java.snippet                         # COUNTER_SERVICE constant
│   ├── SystemServer.java.snippet                    # start the service
│   ├── SystemServiceRegistry.java.snippet           # register the client
│   ├── services.Android.bp.snippet                  # depend on counter-service
│   └── current.txt.snippet                          # API surface
└── examples/
    ├── MainActivity.java                            # sample app
    └── cmd_counter_examples.txt
```

---

## One-time integration into AOSP

Copy the modules into your AOSP tree:

```bash
cp -r aosp-counter-service/system/counter                   $AOSP/system/
cp -r aosp-counter-service/hardware/interfaces/counter     $AOSP/hardware/interfaces/
cp -r aosp-counter-service/frameworks-base-patches/*        $AOSP/frameworks/base/
```

Then apply the four frameworks/base edits described in the snippets:

1. **`frameworks/base/core/java/android/content/Context.java`** — add the
   `COUNTER_SERVICE` constant (see `Context.java.snippet`).
2. **`frameworks/base/core/res/AndroidManifest.xml`** — add the
   `USE_COUNTER_SERVICE` permission (see `AndroidManifest.xml.snippet`).
3. **`frameworks/base/api/current.txt`** — add the permission surface
   (see `current.txt.snippet`).
4. **`frameworks/base/core/java/android/app/SystemServiceRegistry.java`** —
   register the `CounterManager` factory (see `SystemServiceRegistry.java.snippet`).
5. **`frameworks/base/services/java/com/android/server/SystemServer.java`** —
   start the service in `startOtherServices()` (see `SystemServer.java.snippet`).
6. **`frameworks/base/services/Android.bp`** — add `counter-service` to the
   `services` library deps (see `services.Android.bp.snippet`).

---

## Build

```bash
# 1) Framework system service
m counter-service counter-client
m services

# 2) HAL
m android.hardware.counter-aidl
m android.hardware.counter-impl
m android.hardware.counter-service
m hal_counter_client
m android.hardware.counter-java

# 3) (Optional) Build a sample app that uses it
m -j$(nproc)
```

If you are adding a new public API to the framework, also rebuild the
framework stubs:

```bash
m framework-doc-stubs
./out/host/linux-x86/bin/apkanalyzer ...
```

---

## Run / verify on a device

```bash
adb root
adb shell stop
adb shell start

# Confirm the service is published
adb shell service list | grep counter

# Drive the shell command
adb shell cmd counter get
adb shell cmd counter inc 10
adb shell cmd counter get           # -> 10
adb shell cmd counter add 3 4       # -> 7
adb shell cmd counter reset
```

From an app:

```java
CounterManager cm = getSystemService(COUNTER_SERVICE);
cm.increment(1);
int n = cm.getCount();
```

---

## Going native (separate process)

The in-process pattern is enough for most services. Switch to a separate C++
process when you need isolation, larger heap, or direct HAL access. To convert:

1. **Enable NDK/C++ backends** in `system/counter/aidl/Android.bp`:

   ```python
   ndk: { enabled: true, },
   cpp: { enabled: true, },
   ```

2. **Add an NDK impl** in `system/counter/native/CounterServiceImpl.cpp`
   that extends the generated `Bn*` and overrides each method.
3. **Add a binary target** (`cc_binary { name: "counter_service" }`) with a
   `main.cpp` that calls `ABinderProcess_setThreadPoolMaxThreadCount(...)` and
   `AServiceManager_addService(...)`.
4. **Add an `init` rc** (`counter.rc`) declaring the service class and
   `seclabel`, plus the matching `counter_service.te` policy and file context.
5. **Remove the `Lifecycle` registration** from `SystemServer` (the init rc
   starts the process instead). Keep the `ServiceManager.addService` in C++.

The `sepolicy/` and `init/` pieces in this template are stub references for
that path; tighten them to match your device's policy.

---

## The paired HAL

`hardware/interfaces/counter/` is a complete **AIDL HAL** that mirrors the
system service. The framework service can call the HAL via
`CounterHalClient`, with a software fallback if the HAL is absent on a device.

Architecture:

```
App / framework code
        │
        ▼
CounterService.java        (Java, runs in system_server, @SystemService)
        │ uses
        ▼
CounterHalClient.java      (Java, looks up "android.hardware.counter.ICounter/default")
        │ AIDL (NDK)
        ▼
/vendor/bin/hw/android.hardware.counter-service
        │
        ▼
Counter.cpp (BnCounter impl, in vendor partition)
```

To use it from `CounterService.java`:

```java
import android.hardware.counter.CounterHalClient;

private final CounterHalClient mHal = CounterHalClient.get();

@Override
public void increment(int amount) {
    enforcePermission();
    if (mHal.isAvailable()) {
        mHal.increment(amount);
    } else {
        mCount.addAndGet(amount);  // software fallback
    }
}
```

Build and test on device:

```bash
m android.hardware.counter-aidl android.hardware.counter-impl \
  android.hardware.counter-service hal_counter_client

adb push $ANDROID_PRODUCT_OUT/system/bin/hal_counter_client /data/local/tmp/
adb shell /data/local/tmp/hal_counter_client get
adb shell /data/local/tmp/hal_counter_client inc 7
adb shell /data/local/tmp/hal_counter_client add 2 3
adb shell /data/local/tmp/hal_counter_client version
```

Confirm VINTF sees the HAL:

```bash
adb shell dumpsys android.hardware.counter.ICounter/default | head -n 20
adb shell lshal | grep counter
```

See `hardware/interfaces/counter/README.md` for the full HAL walkthrough —
VINTF manifest wiring, init rc, SELinux types, going to multiple instances,
VTS setup, and naming cheatsheet.

---

## Naming cheatsheet (search/replace when you fork this)

| Symbol                            | Where it shows up                              |
| --------------------------------- | ---------------------------------------------- |
| `counter` / `Counter` / `COUNTER` | Filenames, service name, permission name, etc. |
| `ICounterService`                 | AIDL file + generated stub                     |
| `CounterService`                  | Java server implementation                    |
| `CounterManager`                  | Public client API                             |
| `USE_COUNTER_SERVICE`             | Permission name                                |
| `Context.COUNTER_SERVICE`         | System service key                            |
| `android.os.ICounterService-aidl` | `aidl_interface` target name                  |
| `counter-service` / `counter-client` | Java library targets                       |

After renaming, run:

```bash
git ls-files | xargs sed -i 's/Counter/YourName/g; s/counter/yourname/g; s/COUNTER/YOURNAME/g'
```

---

## Things this template intentionally does **not** do

- **VINTF manifest entry** — only needed if the service is part of a HAL.
- **APEX packaging** — recommended only if you want to ship the service as
  a mainline module.
- **HIDL / AIDL HAL** — out of scope for an in-process Java service.
- **Unit tests** — add an `Android.bp` `java_test` module next to `service/`
  using `android.test`/`androidx.test` as appropriate.

Add these on top of the existing skeleton as your service grows.
