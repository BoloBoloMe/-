package com.bolo.downloader.groundcontrol.file.map;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

/**
 * 文件地图顶层接口，定义了管理本地文件的操作
 */
public interface FileMap {
    /**
     * 新增文件
     *
     * @param file 文件
     * @return true-新增成功，false-新增失败
     */
    boolean add(File file);

    /**
     * 删除文件
     *
     * @param file 文件
     * @return true-删除成功，false-删除失败
     */
    boolean delete(File file);

    /**
     * 查找文件
     *
     * @param fileFilter 文件过滤器
     * @return 文件列表
     */
    List<File> find(FileFilter fileFilter);
}
