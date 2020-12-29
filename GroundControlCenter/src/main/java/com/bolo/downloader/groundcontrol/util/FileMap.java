package com.bolo.downloader.groundcontrol.util;

import com.bolo.downloader.groundcontrol.ClientBootstrap;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * 文件地图
 * 提供扫描指定目录的文件列表，按条件查找文件等功能
 */
public class FileMap {
    private static final MyLogger log = LoggerFactory.getLogger(FileMap.class);
    private static File[] paths = {};
    private static Map<String, String> labelMap = new HashMap<>();

    /**
     * 标签列表
     * [label_1,label2...]
     */
    private static String labelListJson = "[]";
    /**
     * 全量的文件列表JSON字符串
     * fileList:[{name:"aaa.web",label:["video","film"]}]
     */
    private static volatile String fullFilesJson = "[]";
    /**
     * 文件名映射至文件绝对路径的Map
     * <name,path>
     */
    private static final ConcurrentHashMap<String, String> nameToPath = new ConcurrentHashMap<>();
    /**
     * 最近添加的文件列表
     * [name1,name2...]
     */
    private static volatile String newFileList = "[]";
    /**
     * 最近访问的文件队列
     * [name1,name2...]
     */
    private static final ConcurrentLinkedQueue<String> lruQueue = new ConcurrentLinkedQueue<>();
    /**
     * 最近访问的文件队列最大长度
     */
    private static final int LRU_QUEUE_SIZE_MAX = 10;
    /**
     * 最近添加的文件队列最大长度
     */
    private static final int NEW_QUEUE_SIZE_MAX = 10;

    /**
     * 盲盒游戏
     * <p>
     * 随机返回文件列表中的文件名
     */
    public static String gamble() {
        Random random = new Random();
        random.setSeed(random.hashCode());
        int min = 1, max = nameToPath.size();
        int s = random.nextInt(max) % (max - min + 1) + min;
        Set<String> keys = nameToPath.keySet();
        int num = 1;
        for (String key : keys) {
            if (num++ == s) {
                return key;
            }
        }
        return "";
    }

    /**
     * 返回全量的列表数据
     *
     * @return full file list data
     * <p>
     * 返回的Json字符串格式：
     * {
     * labels:["video","audio","film""],
     * fileList:[{name:"file_name",label:["video","music"]}],
     * newFile:["file_1","file_2"],
     * lruList:[file_1,file_2...]
     * }
     */
    public static String fullListJson() {
        flush();
        return "{\"labels\":" + labelListJson + ',' +
                "\"fileList\":" + fullFilesJson + ',' +
                "\"newFile\":" + newFileList + ',' +
                "\"lruList\":" + lruJson() + "}";

    }

    /**
     * 根据文件名查找文件的绝对路径
     */
    public static String findByFileName(String name) {
        String path = nameToPath.get(name);
        if (path == null) {
            return null;
        }
        // 更新LRU队列
        if (!lruQueue.remove(name) && lruQueue.size() >= LRU_QUEUE_SIZE_MAX) {
            lruQueue.poll();
        }
        lruQueue.add(name);
        return path;
    }

    volatile private static long lastFlushTime = 0;

    public static void flush() {
        if (lastFlushTime - (lastFlushTime = ClientBootstrap.getSystemTime()) < -180000L) {
            return;
        }
        if (paths.length == 0) {
            String mediaPaths = ConfFactory.get("mediaPath");
            if (null != mediaPaths) {
                String[] pathArr = mediaPaths.split(",");
                paths = new File[pathArr.length];
                for (int i = 0; i < pathArr.length; i++) {
                    paths[i] = new File(pathArr[i]);
                }
            }
            if (labelMap.size() == 0) {
                StringBuilder labelJson = new StringBuilder().append("[\"video\",\"music\",");
                // label1==path1,label2==path2
                String labelConf = ConfFactory.get("labelMap");
                if (labelConf != null) {
                    String[] labels = labelConf.split(",");
                    int cutIndex;
                    for (String labelEntry : labels) {
                        cutIndex = labelEntry.indexOf("==");
                        String label = labelEntry.substring(0, cutIndex);
                        labelMap.put(label, labelEntry.substring(cutIndex + 2));
                        labelJson.append('"').append(label).append("\",");
                    }
                }
                labelJson.deleteCharAt(labelJson.length() - 1).append(']');
                labelListJson = labelJson.toString();
            }
        }
        ClientBootstrap.uniteAsynExecutor.submit(() -> {
            log.info("扫描文件目录");
            // 更新文件列表
            nameToPath.clear();
            scan(paths);
            // 更新文件列表JSON
            StringBuilder json = new StringBuilder().append('[');
            for (Map.Entry<String, String> nameAndPath : nameToPath.entrySet()) {
                json.append("{\"name\":\"").append(nameAndPath.getKey()).append('"');
                // 添加文件标签
                json.append(",\"labels\":[");
                String lowerCaseName = nameAndPath.getKey().toLowerCase();
                if (isVideo(lowerCaseName)) {
                    json.append("\"video\",");
                } else if (isAudio(lowerCaseName)) {
                    json.append("\"music\",");
                }
                List<String> labels = getLabel(nameAndPath.getValue());
                if (labels != null) for (String label : labels) json.append('"').append(label).append("\",");
                json.deleteCharAt(json.length() - 1);
                json.append(']');
                json.append("},");
            }
            json.deleteCharAt(json.length() - 1).append(']');
            // 更新最新文件列表
            updateNewFileList();
            fullFilesJson = json.toString();
        });
    }


    private static void scan(File[] paths) {
        for (File target : paths) {
            String name = target.getName();
            if (target.isDirectory()) {
                File[] child = target.listFiles();
                if (child != null) {
                    scan(child);
                }
            } else if (!nameToPath.containsKey(name) && !target.isHidden() && target.isFile() && (isAudio(name) || isVideo(name))) {
                nameToPath.put(name, target.getAbsolutePath());
            }
        }
    }

    private static volatile long lastTime = 0;

    private static void updateNewFileList() {
        LinkedList<FileAttributeHolder> sortList = new LinkedList<>();
        for (Map.Entry<String, String> nameAndPath : nameToPath.entrySet()) {
            sortList.add(new FileAttributeHolder(nameAndPath.getKey(), getFileCreateTime(nameAndPath.getValue())));
        }
        if (sortList.size() > 0) {
            Collections.sort(sortList);
            long currLastTime = sortList.getLast().getCreateTime();
            if (lastTime < currLastTime) {
                lastTime = currLastTime;
                // 有新文件,更新列表
                StringBuilder newFileListJson = new StringBuilder().append('[');
                for (int i = 0; i < NEW_QUEUE_SIZE_MAX && sortList.size() > 0; i++) {
                    newFileListJson.append('"').append(sortList.pollLast().getFilePath()).append("\",");
                }
                newFileListJson.deleteCharAt(newFileListJson.length() - 1).append(']');
                newFileList = newFileListJson.toString();
            }
        }
    }

    /**
     * 返回最近播放的文件列表
     */
    private static String lruJson() {
        StringBuilder json = new StringBuilder().append('[');
        if (lruQueue.size() > 0) {
            for (String name : lruQueue) {
                json.append('"').append(name).append('"').append(',');
            }
            json.deleteCharAt(json.length() - 1);
        }
        json.append(']');
        return json.toString();
    }


    private final static String VIDEO_NAME_PATTERN = ".+(\\.mp4|\\.webm|.ogg){1}";
    private final static String AUDIO_NAME_PATTERN = ".+(\\.mp3|\\.flac){1}";

    public static boolean isVideo(String name) {
        return Pattern.matches(VIDEO_NAME_PATTERN, name);
    }

    public static boolean isAudio(String name) {
        return Pattern.matches(AUDIO_NAME_PATTERN, name);
    }

    private static List<String> getLabel(String filePath) {
        ArrayList<String> result = new ArrayList<>();
        for (Map.Entry<String, String> labelEntry : labelMap.entrySet()) {
            if (filePath.contains(labelEntry.getValue())) {
                result.add(labelEntry.getKey());
            }
        }
        return result.size() > 0 ? result : null;
    }

    private static Long getFileCreateTime(String filePath) {
        File file = new File(filePath);
        try {
            Path path = Paths.get(filePath);
            BasicFileAttributeView basicview = Files.getFileAttributeView(path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            BasicFileAttributes attr = basicview.readAttributes();
            return attr.creationTime().toMillis();
        } catch (Exception e) {
            return file.lastModified();
        }
    }

    private static class FileAttributeHolder implements Comparable<FileAttributeHolder> {
        private String filePath;
        private long createTime;

        FileAttributeHolder(String filePath, long createTime) {
            this.filePath = filePath;
            this.createTime = createTime;
        }

        public String getFilePath() {
            return filePath;
        }

        public long getCreateTime() {
            return createTime;
        }

        @Override
        public int compareTo(FileAttributeHolder other) {
            long result = this.getCreateTime() - other.getCreateTime();
            if (result == 0) {
                return 0;
            } else if (result < 0) {
                return -1;
            } else {
                return 1;
            }
        }
    }

}
