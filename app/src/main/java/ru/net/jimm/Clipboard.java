package ru.net.jimm;

import android.content.Context;
import android.text.ClipboardManager;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 03.04.13 15:10
 *
 * @author vladimir
 */
public class Clipboard {
    private JimmActivity activity;
    private final Object lock = new Object();

    public Clipboard() {
    }

    void setActivity(JimmActivity activity) {
        this.activity = activity;
    }

    public String get() {
        final AtomicReference<String> text = new AtomicReference<>();
        text.set(null);
        activity.runOnUiThread(() -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                text.set(clipboard.hasText() ? clipboard.getText().toString() : null);
                synchronized (lock) {
                    lock.notify();
                }
            } catch (Throwable e) {
                jimm.modules.DebugLog.panic("get clipboard", e);
                // do nothing
            }
        });
        if (!activity.isActivityThread()) try {
            synchronized (lock) {
                lock.wait();
            }
            //Thread.sleep(100);
        } catch (Exception ignored) {
        }
        return text.get();
    }

    public void put(final String text) {
        activity.runOnUiThread(() -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } catch (Throwable e) {
                jimm.modules.DebugLog.panic("set clipboard", e);
                // do nothing
            }
        });
    }
}
