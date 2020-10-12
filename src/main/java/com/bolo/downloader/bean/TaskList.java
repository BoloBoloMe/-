package com.bolo.downloader.bean;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


/**
 * 任务列表
 * <p>
 * 一个URL对应一个下载任务，列表中的URL不能重复
 */
public class TaskList {
    private LinkedList<String> pending = new LinkedList<>();
    private Map<String, Boolean> assured = new HashMap<>();

    /**
     * 添加任务
     *
     * @param url
     * @return true:添加成功，false:任务重复，不可添加
     */
    public boolean add(String url) {
        if (assured.containsKey(url) || pending.contains(url)) {
            return false;
        }
        pending.addLast(url);
        return true;
    }

    /**
     * 结束任务
     */
    public void closure(String url, boolean isSucceed) {
        assured.put(url, isSucceed);
    }

    /**
     * 清空列表
     */
    public void clear() {
        pending.clear();
        assured.clear();
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
        return pending.pop();
    }
}
