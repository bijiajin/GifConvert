package getting.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Looper {

    public static void prepare() {
        if (LOOPERS.get() != null) {
            throw new IllegalStateException("can not call Looper.prepare() twice");
        }

        LOOPERS.set(new Looper());
        loop();
    }

    private final static ThreadLocal<Looper> LOOPERS = new ThreadLocal<>();

    private Looper() {
    }

    private static void loop() {
        final Looper looper = myLoop();
        Thread thread = new Thread() {

            @Override
            public void run() {
                while (true) {
                    Message message;
                    synchronized (looper.lock) {
                        try {
                            if (looper.messages.isEmpty()) {
                                looper.lock.wait();
                                continue;
                            }

                            final long waitTime = looper.messages.get(0).getTimeRunAt() - System.currentTimeMillis();
                            if (waitTime > 0) {
                                looper.lock.wait(waitTime);
                                continue;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        message = looper.messages.remove(0);
                    }

                    message.getRunnable().run();
                }
            }

        };
        thread.setDaemon(true);
        thread.start();
    }

    public static void postMessage(Message message) {
        final Looper looper = myLoop();
        synchronized (looper.lock) {
            looper.messages.add(message);
            Collections.sort(looper.messages);
            looper.lock.notifyAll();
        }
    }

    public static void removeMessage(Object id) {
        final Looper looper = myLoop();
        synchronized (looper.lock) {
            Iterator<Message> iterator = looper.messages.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getId() == id) {
                    iterator.remove();
                }
            }

            looper.lock.notifyAll();
        }
    }

    private static Looper myLoop() {
        if (LOOPERS.get() == null) {
            throw new IllegalStateException("call Looper.prepare() first");
        }

        return LOOPERS.get();
    }

    private final Object lock = new Object();
    private final List<Message> messages = new ArrayList<>();

}
