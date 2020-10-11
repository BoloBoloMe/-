package com.bolo.downloader;

import lombok.Getter;
import lombok.Setter;


/**
 * 任务链表
 * 单向环形链表,需保证元素唯一（以url作为判断标准）
 */
public class TaskList {
    private Task head = new Task(null);
    private Task cursor = head;

    {
        cursor.next = head;
    }

    public boolean add(String url) {
        return true;
    }

    public Task next() {
        return cursor.next;
    }


    @Getter
    @Setter
    public static class Task {
        private Task next;
        private String url;
        private int taskId;
        /**
         * 任务状态,0:待执行,1:执行中,2:执行成功,3:执行失败
         */
        private int status;

        public Task(String url) {
            this.url = url;
            this.status = 0;
            taskId = hashCode();
        }
    }
}
