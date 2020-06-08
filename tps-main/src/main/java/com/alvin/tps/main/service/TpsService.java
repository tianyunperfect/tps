package com.alvin.tps.main.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class TpsService {

    /**
     * 线程数量
     */
    private Integer n_threads;

    /**
     * 30 秒总时间
     */
    private Integer n_totalTime;

    /**
     * 用原子变量来统计执行时间，便于作原子递减
     */
    private AtomicInteger totalTime;

    /**
     * 用于统计执行的事物总数，用原子方式累加记录
     */
    private AtomicLong totalExecCount;
    private AtomicLong totalExecTime;

    /**
     * 需要到等到所有线程都在同一起跑线，才开始统计计数，类似于发令枪
     */
    private CyclicBarrier barrier;

    /**
     * 执行时间到达时，所有的线程需要依次退出，主线程才开始统计执行事物总数
     */
    private CountDownLatch countDownLatch;

    /**
     * 线程执行标记 , 用volatile修饰，使变量修改具有线程可见性
     */
    public volatile boolean running = false;

    /**
     * 用线程池来执行统计
     */
    private ExecutorService executorService;

    public TpsService() {
    }

    public TpsService(Integer n_threads, Integer n_totalTime) throws IOException {

        this.n_threads = n_threads;
        this.n_totalTime = n_totalTime;

        this.totalTime = new AtomicInteger(this.n_totalTime);
        this.totalExecCount = new AtomicLong(0L);
        this.totalExecTime = new AtomicLong(0L);
        this.barrier = new CyclicBarrier(this.n_threads);
        this.countDownLatch = new CountDownLatch(this.n_threads);

    }

    class Worker implements Runnable {
        private Job job;
        // 每个线程执行的事物统计量
        int innerCount = 0;

        public Worker(Job job) {
            this.job = job;
        }

        @Override
        public void run() {
            try {
                barrier.await(); // 等到所有线程都在起跑线
                while (running) {
                    try {
                        // 单个job可能会超时，或者报异常
                        long start = System.currentTimeMillis();
                        this.job.execute();
                        totalExecTime.addAndGet(System.currentTimeMillis() - start);
                        innerCount++;
                    } catch (Exception e) {
                        System.out.println("线程Id：" + Thread.currentThread().getId() + " " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("线程Id：" + Thread.currentThread().getId() + " " + e.getMessage());
            } finally {
                System.out.println("线程Id：" + Thread.currentThread().getId() + " 执行事物次数为：" + innerCount);
                totalExecCount.getAndAdd(innerCount);
                // 线程结束后，依次计数, 便于主线程继续执行
                countDownLatch.countDown();
            }


        }
    }

    public void run(Job job) throws Exception {
        this.running = true;

        executorService = Executors.newFixedThreadPool(this.n_threads); // 新建固定大小线程的池子
        for (int i = 0; i < this.n_threads; i++) {
            executorService.submit(new Worker(job)); // 提交线程到池子中
        }
        // 还需要一个线程，用于周期检查执行时间是否到达
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (totalTime.decrementAndGet() == 0) { // 执行时间递减到0
                    running = false; // 告诉线程，时间到了，所有线程不再执行
                    scheduledExecutorService.shutdownNow();
                }
            }
        }, 1L, 1L, TimeUnit.SECONDS);

        // 主线程等到所有的线程都退出，则开始统计
        countDownLatch.await();

        long totalExeCount = totalExecCount.get();
        System.out.println(this.n_threads + " 个线程，" + this.n_totalTime + " 秒内总共执行的事物数量：" + totalExeCount);

        long tps = totalExeCount / this.n_totalTime;
        long perSecond = totalExecTime.get() / totalExeCount;
        System.out.println("===============================================");
        System.out.println("TPS: " + tps + "；平均每次耗时ms：" + perSecond);
        System.out.println("===============================================");
        executorService.shutdownNow(); // 关闭线程池
    }

}
