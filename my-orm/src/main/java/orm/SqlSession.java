package orm;

import MyAnnotatrion.Delete;
import MyAnnotatrion.Insert;
import MyAnnotatrion.Update;
import entity.HandleSqlHelper;
import pool.MyPool;
import util.MySpring;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqlSession {
    private HandleResult result = MySpring.getBean("orm.HandleResult");


    private int update(String sql, Object obj) {
        return superMethod(sql, obj);
    }

    private int update(String sql) {
        return superMethod(sql, null);
    }

    private int delete(String sql, Object obj) {
        return superMethod(sql, obj);
    }

    private int delete(String sql) {
        return superMethod(sql, null);
    }

    private int insert(String sql, Object obj) {
        return superMethod(sql, obj);
    }

    private int insert(String sql) {
        return superMethod(sql, null);
    }

    private <T> T selectOne(String sql, Class cla) {
        return selectOne(sql, null, cla);
    }

    private <T> List<T> select(String sql, Class cla) {
        return select(sql, null, cla);
    }

    /**
     * 针对新增 修改 删除 等操作 因为他们本身就是sql不同
     * 预处理的参数不同  执行方法都是 executeUpdate()
     * 定义一个sql的规则  然后把参数传递过来 通过对sql的解析 在对参数进行赋值
     * 可以统一对这些操作进行处理 可以简化代码
     *
     * @param sql sql语句
     * @param obj sql语句中的参数
     * @return 返回对数据库操作的结果
     */
    private int superMethod(String sql, Object obj) {
        //解析参数
        HandleSqlHelper helper = result.handleSql(sql);
        //获取预处理参数
        Connection conn = null;
        //获取预处理状态参数
        PreparedStatement psta = null;
        try {
            conn = MyPool.getConnection();
            psta = conn.prepareStatement(helper.getSql());
            //对参数进行赋值
            result.handleParam(psta, helper.getDataList(), obj);
            //执行并返回结果
            return psta.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭
            MyPool.close(conn, psta, null);
        }
        return -1;
    }

    private <T> List<T> select(String sql, Object obj, Class cla) {
        List<T> list = new ArrayList<T>();

        //处理sql
        HandleSqlHelper helper = result.handleSql(sql);
        //获取连接
        Connection conn = null;
        //获取预处理参数
        PreparedStatement psta = null;
        // 获取结果集
        ResultSet rs = null;
        try {
            conn = MyPool.getConnection();
            psta = conn.prepareStatement(helper.getSql());
            //处理sql参数
            result.handleParam(psta, helper.getDataList(), obj);
            //进行查询
            rs = psta.executeQuery();
            //返回结果
            while (rs.next()) {
                T t = result.handleResult(rs, cla);
                list.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭
            MyPool.close(conn, psta, rs);
        }


        return list;
    }

    /**
     * 数据库的查询操作  查询操作sql 语句不同  返回值不同 执行的方法都是executeQuery()
     * 按照规定的sql 写sql  解析sql  对sql 进行处理  执行查询 返回
     *
     * @param sql sql语句
     * @param obj sql语句的对应参数的结果
     * @param cla 返回值类型  因为不值啊返回值是什么类型  告诉我 返回最终结果
     * @param <T> 泛型
     * @return 返回结果
     */
    private <T> T selectOne(String sql, Object obj, Class cla) {
        return select(sql, obj, cla).size() == 0 ? null : (T) select(sql, obj, cla).get(0);
    }

    public <T> T getMapping(Class cla) {
        return (T) Proxy.newProxyInstance(cla.getClassLoader(), new Class[]{cla}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                //获取方法上面的注解
                Annotation methodAnnotation = method.getAnnotations()[0];
                //获取注解的类型
                Class cla = methodAnnotation.annotationType();
                String sql = null;
                //获取参数
                Object arg = args == null ? null : (args.length == 0 ? null : args[0]);
                //获取sql
                try {
                    Method annotationMethod = cla.getDeclaredMethod("value");
                    sql = (String) annotationMethod.invoke(methodAnnotation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //判断注解的类型执行不同方法
                if (Insert.class == cla) {
                    return SqlSession.this.insert(sql, arg);
                } else if (Update.class == cla) {
                    return SqlSession.this.update(sql, arg);
                } else if (Delete.class == cla) {
                    return SqlSession.this.delete(sql, arg);
                } else {
                    //获取返回值的类型
                    Class returnType = method.getReturnType();
                    if (returnType == List.class) {
                        //获取泛型的类型
                        ParameterizedType parameterizedType = null;
                        Class clazz = null;
                        try {
                            parameterizedType = (ParameterizedType) method.getGenericReturnType();
                            clazz = (Class) parameterizedType.getActualTypeArguments()[0];
                        } catch (Exception e) {
                            clazz = Map.class;
                        }

                        return SqlSession.this.select(sql, arg, clazz);
                    } else {
                        return SqlSession.this.selectOne(sql, arg, returnType);
                    }
                }
            }
        });
    }


}

