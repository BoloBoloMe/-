package com.bolo.downloader.bean;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * 任务列表
 * <p>
 * 一个URL对应一个下载任务，列表中的URL不能重复
 */
public class TaskList {
    private ConcurrentLinkedQueue<String> pending = new ConcurrentLinkedQueue<>();
    private Map<String, Integer> history = new ConcurrentHashMap<>();
    public static final Integer PENDING = 0;
    public static final Integer SUCCEED = 1;
    public static final Integer FAIL = 2;


    /**
     * 添加任务
     *
     * @param url
     * @return true:添加成功，false:任务重复，不可添加
     */
    public boolean add(String url) {
        if (history.containsKey(url)) {
            return false;
        }
        history.put(url, PENDING);
        pending.add(url);
        return true;
    }

    /**
     * 结束任务
     */
    public void closure(String url, boolean isSucceed) {
        history.replace(url, isSucceed ? SUCCEED : FAIL);
    }

    /**
     * 清空列表
     */
    public void clear() {
        pending.clear();
        history.clear();
    }

    /**
     * 列表查询
     */
    public Map<String, Integer> list() {
        return new HashMap<>(history);
    }

    /**
     * 是否还有待处理的任务
     */
    public boolean hasNextPending() {
        return !pending.isEmpty();
    }

    /**
     * 锁定并返回下一个待处理的任务
     *
     * @return url, 当没有待处理的任务时返回空字符串
     */
    public String lockNextPending() {
        return pending.poll();
    }
}
