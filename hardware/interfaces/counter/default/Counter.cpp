#include "Counter.h"

#include <android-base/logging.h>
#include <android/binder_ibinder.h>
#include <android/binder_status.h>

#include <climits>
#include <cstdint>
#include <string>

namespace aidl::android::hardware::counter {

::ndk::ScopedAStatus Counter::getCount(int32_t* _aidl_return) {
    if (_aidl_return == nullptr) {
        return ::ndk::ScopedAStatus::fromExceptionCode(EX_NULL_POINTER);
    }
    *_aidl_return = mCount.load(std::memory_order_acquire);
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Counter::increment(int32_t amount) {
    if (amount < 0) {
        return ::ndk::ScopedAStatus::fromExceptionCode(EX_ILLEGAL_ARGUMENT);
    }
    int32_t prev = mCount.fetch_add(amount, std::memory_order_acq_rel);
    int32_t now = prev + amount;
    LOG(INFO) << "Counter::increment(" << amount << ") " << prev << " -> " << now;
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Counter::reset() {
    int32_t prev = mCount.exchange(0, std::memory_order_acq_rel);
    LOG(INFO) << "Counter::reset() prev=" << prev;
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Counter::add(int32_t a, int32_t b, int32_t* _aidl_return) {
    if (_aidl_return == nullptr) {
        return ::ndk::ScopedAStatus::fromExceptionCode(EX_NULL_POINTER);
    }
    int64_t sum = static_cast<int64_t>(a) + static_cast<int64_t>(b);
    if (sum > INT32_MAX || sum < INT32_MIN) {
        return ::ndk::ScopedAStatus::fromExceptionCode(EX_ILLEGAL_ARGUMENT);
    }
    *_aidl_return = static_cast<int32_t>(sum);
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Counter::notifyEvent(const std::string& in_event) {
    LOG(INFO) << "Counter::notifyEvent(\"" << in_event << "\")";
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Counter::getInterfaceVersion(int32_t* _aidl_return) {
    *_aidl_return = ICounter::VERSION;
    return ::ndk::ScopedAStatus::ok();
}

::ndk::ScopedAStatus Counter::getInterfaceHash(std::string* _aidl_return) {
    *_aidl_return = ICounter::HASH;
    return ::ndk::ScopedAStatus::ok();
}

}  // namespace aidl::android::hardware::counter
