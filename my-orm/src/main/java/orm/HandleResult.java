package orm;

import entity.HandleSqlHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandleResult {
    /**
     * 处理返回结果  对结果进行判断  基本类型  还是map  对象
     *
     * @param rs  结果集
     * @param cla 返回值类型
     * @param <T> 返回值
     * @return 返回结果
     */
    <T> T handleResult(ResultSet rs, Class cla) throws Exception {
        //是基本类型
        if (handleType(cla)) {
            return (T) rs.getObject(1);
        } else {
            Object object = null;
            //是map
            if (cla == Map.class) {
                object = new HashMap();
                handleMap(object, rs);
            } else {
                object = cla.newInstance();
                if (object instanceof Map) {
                    handleMap(object, rs);
                } else {
                    //是实体对象
                    handleEntityObj(object, rs);
                }
            }
            return (T) object;
        }
    }

    /**
     * 当返回值是map集合的时候  处理结果
     *
     * @param object 返回的结果
     * @param rs     结果集
     */
    private void handleEntityObj(Object object, ResultSet rs) throws Exception {
        Class cla = object.getClass();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            try {
                Field field = cla.getDeclaredField(columnName);
                String methodName = "set" + columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
                Method method = cla.getDeclaredMethod(methodName, field.getType());
                method.invoke(object, rs.getObject(columnName));
            } catch (NoSuchFieldException e) {

            }
        }
    }

    /**
     * 当返回值是map集合的时候  处理结果
     *
     * @param object 返回的结果
     * @param rs     结果集
     */
    private void handleMap(Object object, ResultSet rs) throws SQLException {
        Map map = (Map) object;
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            Object obj = rs.getObject(columnName);
            map.put(columnName, obj);
        }

    }

    //==============================================================

    /**
     * 对sql中的？ 进行赋值 根据解析出来的数据进行赋值
     *
     * @param psta     预处理参数
     * @param dataList 需要进行赋值的数据  按照顺序
     * @param object   需要赋值的变量的值  只能是三种类型  基本类型  map集合  对象
     */
    void handleParam(PreparedStatement psta, List<String> dataList, Object object) throws Exception {
        if (psta == null || object == null || dataList == null || dataList.size() == 0) {
            return;
        }
        Class obj = object.getClass();
        //当object是基本类型
        if (handleType(obj)) {
            psta.setObject(1, object);
        } else if (object instanceof Map) {
            //当object是map 键必须是sql语句和参数名字一致 否则就会报错
            for (int i = 0; i < dataList.size(); i++) {
                Map map = (Map) object;
                psta.setObject(i + 1, map.get(dataList.get(i)));
            }
        } else {
            //当object是对象的时候  属性名字必须和SQL语句中参数名字一致 否则就会报错
            for (int i = 0; i < dataList.size(); i++) {
                String fieldName = dataList.get(i);
                String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                Method method = obj.getMethod(methodName);
                psta.setObject(i + 1, method.invoke(object));
            }
        }
    }

    /**
     * 对sql的处理  把定义号的 sql 解析成预处理的sql
     * 例如 update xxx set name =#{name} where id = #{id}
     * 解析成 update xxx set name = ? where id = ?
     *
     * @param sql 按照规则写的sql
     * @return 处理的封装  处理的sql  和解析出来的参数name id
     */
     HandleSqlHelper handleSql(String sql) {
        StringBuilder sb = new StringBuilder();
        List<String> dataList = new ArrayList<String>();
        HandleSqlHelper handleSqlHelper = null;
        int left = sql.indexOf("#{");
        int right = sql.indexOf("}");
        if (left != -1 && right != -1 && right > left) {
            while (left != -1 && right != -1 && right > left) {
                sb.append(sql.substring(0, left));
                sb.append("?");
                dataList.add(sql.substring(left + 2, right));
                sql = sql.substring(right + 1);
                left = sql.indexOf("#{");
                right = sql.indexOf("}");
            }
        }
        sb.append(sql);
        handleSqlHelper = new HandleSqlHelper(sb.toString(), dataList);
        return handleSqlHelper;
    }

    private boolean handleType(Class cla) {
        return cla == int.class || cla == Integer.class || cla == String.class || cla == float.class
                || cla == Float.class || cla == double.class || cla == Double.class || cla == short.class ||
                cla == Short.class || cla == char.class || cla == Character.class || cla == byte.class
                || cla == Byte.class || cla == long.class || cla == Long.class;
    }
}
