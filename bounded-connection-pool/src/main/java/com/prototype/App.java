package com.prototype;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        ConnectionPool pool = new ConnectionPool(3); // 3 connections

        Runnable task = () -> {
            try {
                String conn = pool.acquire();  // acquire connection
                System.out.println(System.currentTimeMillis() + " - " +Thread.currentThread().getName() + " acquired " + conn);
                Thread.sleep(1000);            // simulate work
                pool.release(conn);            // release back
                System.out.println(System.currentTimeMillis() + " - " +Thread.currentThread().getName() + " released " + conn);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < 5; i++) {
            new Thread(task, "Thread-" + (i + 1)).start();
        }
    }

}
