package util;

import java.util.HashMap;
import java.util.Map;

public class MySpring {
    private static Map beanMap = new HashMap();

    private MySpring() {
    }

    public synchronized static <T> T getBean(String name) {
        T t = (T) beanMap.get(name);
        if (t == null) {
            try {
                Class cla = Class.forName(name);
                t = (T) cla.newInstance();
                beanMap.put(name, t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return t;
    }

}
