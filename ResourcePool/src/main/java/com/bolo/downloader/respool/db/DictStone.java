package com.bolo.downloader.respool.db;

import java.util.Map;

/**
 * 可持久化的字典——键值对形式的简单NoSQL数据库，关键字不可重复
 */
public interface DictStone<K, V> {
    /**
     * 插入键值对
     *
     * @return 插入键值对数量
     */
    String put(K key, V value);

    /**
     * 批量添加键值对
     *
     * @return 插入键值对数量
     */
    void putAll(Map<K, V> map);

    /**
     * 删除键值对
     *
     * @return 返回主键关联的值
     */
    V remove(K key);

    /**
     * 删除所有键值对
     */
    void removeAll();

    /**
     * 返回关键字关联的值
     *
     * @param key
     */
    V get(K key);

    /**
     * 获取键值对数量
     */
    int size();


    /**
     * 将字典内容刷新到数据文件
     */
    void save();
}
