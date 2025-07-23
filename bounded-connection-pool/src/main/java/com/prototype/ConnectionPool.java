package com.prototype;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool {
    private final BlockingQueue<String> pool;

    public ConnectionPool(int size) {
        pool = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            pool.add("Connection-" + (i + 1));
        }
    }

    public String acquire() throws InterruptedException {
        return pool.take();  // blocks if no connection is available
    }

    public void release(String connection) throws InterruptedException {
        pool.put(connection); // blocks if pool is already full
    }

    public int availableConnections() {
        return pool.size();
    }
}
