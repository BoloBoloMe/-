package com.bolo.downloader.respool.test.db.writebuff;


import com.bolo.downloader.respool.db.buff.SynchronizedCycleWriteBuff;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试写缓冲
 */
public class CapabilityTest {
    private static int workThreadNum = 300;
    private static int dataCount = 100;
    private static int dataCountSum = workThreadNum * dataCount;
    private static CyclicBarrier cyclicBarrier = new CyclicBarrier(workThreadNum + 1);
    private static ExecutorService workers = Executors.newCachedThreadPool();
    private static SynchronizedCycleWriteBuff writeBuff = new SynchronizedCycleWriteBuff(1000, 100, 10);

    public static void main(String[] args) throws BrokenBarrierException, InterruptedException, IOException {
        final AtomicInteger threadNum = new AtomicInteger(0);
        // 工作线程分别往writeBuff中写入数据
        for (int i = 1; i <= workThreadNum; i++) {
            workers.execute(() -> {
                // 准备测试数据
                String[] datas = new String[dataCount];
                int min = dataCount * threadNum.incrementAndGet();
                for (int dc = 0; dc < dataCount; dc++) datas[dc] = "" + (min + dc);
                try {
                    // 等待系统就绪后写入缓冲
                    System.out.println("线程" + Thread.currentThread().getName() + "测试数据准备完毕");
                    cyclicBarrier.await();
                    for (String data : datas) writeBuff.put(data, data);
                    cyclicBarrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // 主线程从writeBuff中获取数据，并检查数据是否正确
        long startTime = System.currentTimeMillis();
        cyclicBarrier.await();
        System.out.println("开始操作缓存");
        final AtomicInteger count = new AtomicInteger(0);
        while (count.get() < dataCountSum) {
            writeBuff.write(new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                }

                @Override
                public void flush() throws IOException {
                    count.incrementAndGet();
                }

                @Override
                public void close() throws IOException {

                }
            });
        }
        cyclicBarrier.await();
        long endTime = System.currentTimeMillis();
        System.out.println("工作线程运行完毕，运行时间:" + (endTime - startTime) / 1000 + "秒");
        System.out.println("缓冲链表节点使用率：" + writeBuff.usageReport(false));
        System.out.println("程序执行完毕");
    }
}