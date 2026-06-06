#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>

#include <aidl/android/hardware/counter/ICounter.h>

#include <iostream>
#include <string>

using aidl::android::hardware::counter::ICounter;

namespace {

void printUsage(const char* argv0) {
    std::cout << "Usage: " << argv0
              << " <get|inc N|reset|add A B|notify MSG|version>\n"
              << "  get        Print current count\n"
              << "  inc N      Increment count by N\n"
              << "  reset      Reset count to 0\n"
              << "  add A B    Print A + B (no state change)\n"
              << "  notify M   Fire-and-forget notify\n"
              << "  version    Print interface version\n";
}

}  // namespace

int main(int argc, char** argv) {
    if (argc < 2) {
        printUsage(argv[0]);
        return 2;
    }

    ABinderProcess_setThreadPoolMaxThreadCount(1);

    std::shared_ptr<ICounter> hal = ICounter::fromServiceName("default");
    if (hal == nullptr) {
        LOG(ERROR) << "Counter HAL default service not available";
        return 3;
    }

    const std::string cmd = argv[1];

    if (cmd == "get") {
        int32_t v = 0;
        auto st = hal->getCount(&v);
        if (!st.isOk()) {
            LOG(ERROR) << "getCount failed: " << st.getDescription();
            return 4;
        }
        std::cout << v << "\n";
        return 0;
    }
    if (cmd == "inc") {
        if (argc < 3) {
            printUsage(argv[0]);
            return 2;
        }
        int32_t n = std::stoi(argv[2]);
        auto st = hal->increment(n);
        if (!st.isOk()) {
            LOG(ERROR) << "increment failed: " << st.getDescription();
            return 4;
        }
        std::cout << "ok\n";
        return 0;
    }
    if (cmd == "reset") {
        auto st = hal->reset();
        if (!st.isOk()) {
            LOG(ERROR) << "reset failed: " << st.getDescription();
            return 4;
        }
        std::cout << "ok\n";
        return 0;
    }
    if (cmd == "add") {
        if (argc < 4) {
            printUsage(argv[0]);
            return 2;
        }
        int32_t a = std::stoi(argv[2]);
        int32_t b = std::stoi(argv[3]);
        int32_t sum = 0;
        auto st = hal->add(a, b, &sum);
        if (!st.isOk()) {
            LOG(ERROR) << "add failed: " << st.getDescription();
            return 4;
        }
        std::cout << sum << "\n";
        return 0;
    }
    if (cmd == "notify") {
        if (argc < 3) {
            printUsage(argv[0]);
            return 2;
        }
        hal->notifyEvent(argv[2]);
        return 0;
    }
    if (cmd == "version") {
        std::cout << ICounter::descriptor << " v" << ICounter::VERSION << "\n";
        return 0;
    }

    printUsage(argv[0]);
    return 2;
}
