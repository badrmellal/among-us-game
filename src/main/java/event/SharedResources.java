package event;

import java.util.concurrent.locks.ReentrantLock;

public class SharedResources {
    private static final ReentrantLock clientsLock = new ReentrantLock();
    private static final ReentrantLock playersLock = new ReentrantLock();

    public static void withClientsLock(Runnable action) {
        clientsLock.lock();
        try {
            action.run();
        } finally {
            clientsLock.unlock();
        }
    }

    public static void withPlayersLock(Runnable action) {
        playersLock.lock();
        try {
            action.run();
        } finally {
            playersLock.unlock();
        }
    }
}
