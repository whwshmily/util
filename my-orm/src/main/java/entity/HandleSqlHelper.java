package entity;

import java.util.List;

public class HandleSqlHelper {
    private String sql;
    private List<String> dataList;

    public HandleSqlHelper() {
    }

    public HandleSqlHelper(String sql, List<String> dataList) {
        this.sql = sql;
        this.dataList = dataList;
    }

    public List<String> getDataList() {
        return dataList;
    }

    public void setDataList(List<String> dataList) {
        this.dataList = dataList;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
