import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jslee on 15/8/1.
 */
public class JavaMain {
    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException {
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        final Object o = new Object();
        for (int i = 0; i < 10; i++) {
            final int id = i;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep((long) (100*Math.random()));
                    } catch (InterruptedException e) {
                    }
                    synchronized (o) {
                        try {
                            System.out.println("I wait!! : " + id);
                            o.wait();
                            System.out.println("I free!! : " + id);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }).start();
        }
        Thread.sleep(1000);
        for (int i = 0; i < 10; i++) {
            synchronized (o) {
                o.notify();
            }
            Thread.sleep(1000);
        }
        System.exit(-1);
    }
}
