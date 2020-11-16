package test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolTest {
    public static void main(String[] args) {
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
    }
}
