#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>

#include <aidl/android/hardware/counter/BnCounter.h>
#include <aidl/android/hardware/counter/ICounter.h>

#include "Counter.h"

using aidl::android::hardware::counter::Counter;

int main(int /*argc*/, char** /*argv*/) {
    ABinderProcess_setThreadPoolMaxThreadCount(4);

    std::shared_ptr<Counter> service = ::ndk::SharedRefBase::make<Counter>();
    const std::string instance = std::string(Counter::descriptor) + "/default";

    binder_status_t status = AServiceManager_addService(
            service->asBinder().get(), instance.c_str());
    if (status != STATUS_OK) {
        LOG(FATAL) << "Failed to register " << instance << ": " << status;
    }

    LOG(INFO) << "Counter HAL service ready at " << instance;
    ABinderProcess_joinThreadPool();
    return EXIT_FAILURE;
}
