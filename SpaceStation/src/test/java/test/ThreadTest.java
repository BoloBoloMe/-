package test;

public class ThreadTest {
    public static void main(String[] args) {
        try {
            Thread thread = new Thread(() -> {
                System.out.println("当前线程id：" + Thread.currentThread().getId());
                System.out.flush();
            });
            System.out.println("第一次启动线程.");
            System.out.flush();
            thread.start();
            System.out.println("第二次启动线程.");
            System.out.flush();
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
