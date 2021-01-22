package test;


import com.bolo.downloader.util.FixedCachedThreadPool;

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
        threadPool.scheduleAtFixedRate(() -> System.out.println("执行ScheduledThreadTask"), 5, 10, SECONDS);
//        while (true) ;
    }

    public void testFixedCachedThreadPool() {
        ExecutorService threadPool = FixedCachedThreadPool.newFixedCachedThreadPool(2);
        for (int i = 0; i < 10; i++) {
            threadPool.submit(() -> {
                System.out.println("执行线程：" + Thread.currentThread().getName());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            if (i == 4) {
                try {
                    System.out.println("主线程休眠两分钟");
                    Thread.sleep(120000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new ThreadPoolTest().testFixedCachedThreadPool();
    }
}
