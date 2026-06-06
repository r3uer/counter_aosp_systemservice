package android.hardware.counter;

@VintfStability
interface ICounter {
    int getCount();

    void increment(int amount);

    void reset();

    int add(int a, int b);

    oneway void notifyEvent(String event);
}
