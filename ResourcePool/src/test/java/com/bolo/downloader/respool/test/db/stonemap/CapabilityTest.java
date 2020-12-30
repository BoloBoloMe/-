package com.bolo.downloader.respool.test.db.stonemap;


import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.db.buff.SynchronizedCycleWriteBuff;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试持久化字典API
 */
public class CapabilityTest {
    private static int workThreadNum = 300;
    private static int dataCount = 10000;
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(workThreadNum + 1);
    private static ExecutorService workers = Executors.newCachedThreadPool();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static AtomicInteger rewrite = new AtomicInteger(0);
    private static AtomicInteger flush = new AtomicInteger(0);


    public static void main(String[] args) {
        Map<String, String> map = new StoneMap("D:\\MyResource\\Desktop\\data\\", 3, new SynchronizedCycleWriteBuff(1000, 1, 2));
        ((StoneMap) map).loadDbFile();
        // 工作线程:使用 map
        for (int i = 0; i < workThreadNum; i++) {
            workers.execute(() -> {
                try {
                    Random random = new Random(Thread.currentThread().getId());
                    cyclicBarrier.await();
                    for (int count = 0; count < dataCount; count++) map.put("k_" + random.nextInt(), "V_V_V_V");
                    cyclicBarrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        // StoneMap 专用同步线程：异步刷新数据文件
        scheduler.scheduleWithFixedDelay(() -> {
            StoneMap stoneMap = (StoneMap) map;
//            if (stoneMap.modify() > 100) {
//                stoneMap.rewriteDbFile();
//                rewrite.incrementAndGet();
//                System.out.println("已执行 " + rewrite.get() + " 次重写数据文件");
//            } else {
            stoneMap.flushWriteBuff();
            flush.incrementAndGet();
            System.out.println("已执行 " + flush.get() + " 次刷新日志文件缓冲");
//            }
            System.out.println("map 状态：" + map.toString());
        }, 1, 1, TimeUnit.SECONDS);

        // 主线程:记录测试数据
        try {
            long allDataCount = dataCount * workThreadNum;
            System.out.println("键值对数量：" + allDataCount);
            System.out.println("线程数：" + workThreadNum);
            long startTime = System.currentTimeMillis();
            cyclicBarrier.await();
            cyclicBarrier.await();
            long endTime = System.currentTimeMillis();
            long time = (endTime - startTime) / 1000;
            System.out.println("map size :" + map.size());
            System.out.println("耗时 :" + time);
            System.out.println(allDataCount / time + " 个每毫秒");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}