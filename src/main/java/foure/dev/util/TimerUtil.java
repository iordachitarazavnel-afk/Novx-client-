package foure.dev.util;

/**
 * TimerUtil — simple delay helper used by combat modules.
 */
public class TimerUtil {
    private long lastMs = System.currentTimeMillis();

    public boolean delay(float ms) {
        return (System.currentTimeMillis() - lastMs) > ms;
    }

    public float getElapsed() {
        return System.currentTimeMillis() - lastMs;
    }

    public void reset() {
        lastMs = System.currentTimeMillis();
    }
}

