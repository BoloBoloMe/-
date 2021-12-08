package com.bolo.downloader.groundcontrol.file.map.impl;

import com.bolo.downloader.groundcontrol.file.map.FileMap;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

/**
 * 全盘扫描文件地图
 */
public class FullScanFileMap implements FileMap {
    @Override
    public boolean add(File file) {
        return false;
    }

    @Override
    public boolean delete(File file) {
        return false;
    }

    @Override
    public List<File> find(FileFilter fileFilter) {
        return null;
    }
}
