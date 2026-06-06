package android.os;

interface ICounterService {
    const int VERSION = 1;

    int getCount();

    void increment(int amount);

    void reset();

    int add(int a, int b);

    oneway void notifyEvent(String event);
}
