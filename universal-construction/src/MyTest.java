import java.util.ArrayList;
import java.util.List;

/**
 * 2019-10-21 : 23:45
 *
 * @author Nikita Savinov
 */

public class MyTest {

    private static volatile List<Integer> result = new ArrayList<>();

    private static class MyThread implements Runnable {

        private int a;

        MyThread(int a) {
            this.a = a;
        }

        @Override
        public void run() {
            Solution a = new Solution();
            result.add(a.getAndAdd(this.a));
//            result.add(a.getAndAdd(this.a + 1));
//            result.add(a.getAndAdd(this.a + 2));
        }
    }

    public static void main(String[] args) {
        MyThread a = new MyThread(10);
        MyThread b = new MyThread(20);
        MyThread c = new MyThread(30);
        a.run();
        b.run();
        c.run();
        System.out.println(result);
    }

}
