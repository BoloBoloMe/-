package test;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

public class ThreadPoolTest {
    public void testFixedThreadPool() {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);

        System.out.println("第一次调用线程池");
        threadPool.submit(() -> {
            System.out.println("第一次调用线程池-线程id:" + Thread.currentThread().getId());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("第二次调用线程池");
        threadPool.submit(() -> {
            System.out.println("第二次调用线程池-线程id:" + Thread.currentThread().getId());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("第三次调用线程池");
        threadPool.submit(() -> {
            System.out.println("第三次调用线程池-线程id:" + Thread.currentThread().getId());
        });
        System.out.println("主线程结束");
//        while (true) ;
    }

    public void testScheduledThreadPool() {
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(1);
        threadPool.scheduleAtFixedRate(() -> System.out.println("执行ScheduledThreadTask"), 5,10, SECONDS);
//        while (true) ;
    }
}
