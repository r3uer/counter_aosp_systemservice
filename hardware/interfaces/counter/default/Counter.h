#pragma once

#include <aidl/android/hardware/counter/BnCounter.h>

#include <atomic>
#include <memory>

namespace aidl::android::hardware::counter {

class Counter : public BnCounter {
public:
    Counter() = default;
    ~Counter() override = default;

    ::ndk::ScopedAStatus getCount(int32_t* _aidl_return) override;
    ::ndk::ScopedAStatus increment(int32_t amount) override;
    ::ndk::ScopedAStatus reset() override;
    ::ndk::ScopedAStatus add(int32_t a, int32_t b, int32_t* _aidl_return) override;
    ::ndk::ScopedAStatus notifyEvent(const std::string& in_event) override;

    ::ndk::ScopedAStatus getInterfaceVersion(int32_t* _aidl_return) override;
    ::ndk::ScopedAStatus getInterfaceHash(std::string* _aidl_return) override;

private:
    std::atomic<int32_t> mCount{0};
};

}  // namespace aidl::android::hardware::counter
