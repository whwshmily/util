package pool;

import util.JDBCUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyPool {
    private static List<Connection> pool;
    private static final String INIT_SIZE = JDBCUtil.getValue("initSize");
    private static final String WAIT_TIME = JDBCUtil.getValue("waitTime");

    static {
        pool = new ArrayList<Connection>();
        for (int i = 0; i < Integer.parseInt(INIT_SIZE); i++) {
            pool.add(new MyConnection());
        }
    }

    private MyPool() {
    }

    private static synchronized Connection createConnection() {
        for (int i = 0; i < pool.size(); i++) {
            MyConnection conn = (MyConnection) pool.get(i);
            if (!conn.isFlag()) {
                conn.setFlag(true);
                return conn;
            }
        }
        return null;
    }

    public static Connection getConnection() {
        Connection conn = createConnection();
        if (conn == null) {
            try {
                Thread.sleep(Integer.parseInt(WAIT_TIME));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            conn = createConnection();
        }
        return conn;
    }

    public static void close(Connection conn, PreparedStatement psta, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (psta != null) {
                psta.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
