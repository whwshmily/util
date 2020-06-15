package pool;

import util.JDBCUtil;

import java.lang.reflect.ParameterizedType;
import java.sql.*;

public class MyConnection extends MiddleConnection {
    private static final String URL = JDBCUtil.getValue("url");
    private static final String USER_NAME = JDBCUtil.getValue("userName");
    private static final String PASSWORD = JDBCUtil.getValue("password");
    private static final String DRIVER = JDBCUtil.getValue("driver");
    private boolean flag;
    private Connection conn;

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    {
        try {
            conn = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    @Override
    public Statement createStatement() throws SQLException {
        return conn.createStatement();
    }

    @Override
    public void close() throws SQLException {
        flag = false;
    }


}
