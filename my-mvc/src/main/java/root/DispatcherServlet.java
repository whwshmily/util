package root;

import annotation.RequestMapping;
import annotation.RequestParam;
import com.google.gson.Gson;
import exception.NoSuchRedirect;
import exception.ParamTypeException;
import exception.RequestMappingException;
import result.HandleResult;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

@WebServlet("/")
public class DispatcherServlet extends HttpServlet {
    //配置文件的名字
    private static final String DEFAULT_FILE = "application.properties";
    //存储请求的资源名字对应的类名
    private static Map<String, String> requestNameMap = new HashMap<>();
    //注解扫描的 方式获取的对应的关系 存贮请求和类对象
    private static Map<String, Object> classMap = new HashMap<>();
    //用于存贮请求资源名字和对应的方法
    private static Map<String, Method> methodMap = new HashMap<>();
    //通过注解扫描 存放所有的controller类名字
    private static List<String> controllerClassName = new ArrayList<>();

    /**
     * 读取配置文件 获取配置文件的对应关系
     * 通过对应关系 根据不同的请求找到不同的类
     */
    private static void readConfigFile() {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_FILE));
            Enumeration<?> enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String contentName = (String) enumeration.nextElement();
                String className = properties.getProperty(contentName);
                requestNameMap.put(contentName, className);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过配置文件获得要扫描包的信息  对要扫描的包进行扫描
     */
    private static void scanPackage() {
        //获取要扫描包的名字
        String packageName = requestNameMap.get("scan");
        if (packageName != null) {
            //判断要扫描几个包  按 . 进行拆分
            String[] packageNames = packageName.split("\\.");
            for (int i = 0; i < packageNames.length; i++) {
                //每一个包名字
                String name = packageNames[i];
                //获取包连接
                URL url = Thread.currentThread().getContextClassLoader().getResource(name);
                //获取包路径
                String path = url.getPath();
                //获取包对应的文件
                File file = new File(path);
                scanFile(name, file);
            }
        }
    }

    /**
     * 进行扫描
     *
     * @param packageName 包名
     * @param file        包所在的文件
     */
    private static void scanFile(String packageName, File file) {
        //获取包下面的所有文件  判断是否是.class文件 是 就对其扫描获取信息  是否是文件夹 是 打开文件夹扫描内部的文件
        File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.getName().endsWith(".class") || pathname.isDirectory()) {
                    return true;
                }
                return false;
            }
        });
        //真正的扫描并获取相关的信息
        if (files != null && files.length != 0) {
            for (int j = 0; j < files.length; j++) {
                File classFile = files[j];
                //判断是否是文件夹
                if (classFile.isDirectory()) {
                    scanFile(packageName + "." + classFile.getName(), classFile);
                } else {
                    //文件就获取类信息 类全路径
                    String fileName = classFile.getName();
                    String realClassName = packageName + "." + fileName.substring(0, fileName.lastIndexOf(".class"));
                    controllerClassName.add(realClassName);
                }
            }
        }
    }

    /**
     * 获取servlet类对其进行加载  并存贮相关的对应关系
     */
    private static void getRequestMapping() {
        if (controllerClassName.size() == 0) {
            return;
        }
        //遍历扫描的servlet类  获取请求对应关系
        for (String className : controllerClassName) {
            try {
                //通过反射加载类
                Class cla = Class.forName(className);
                //获取无参构造器
                Object object = cla.newInstance();
                //获取类上面的请求关系注解
                RequestMapping classRequestMapping = (RequestMapping) cla.getAnnotation(RequestMapping.class);
                //获取类中所有的方法
                Method[] methods = cla.getDeclaredMethods();
                if (methods != null && methods.length != 0) {
                    for (Method method : methods) {
                        //获取方法上面的注解
                        RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                        //判断注解是否为空  为空表示没有对应关系 则抛出异常
                        if (methodRequestMapping == null) {
                            throw new RequestMappingException("requestMapping is not found");
                        }
                        //获取请求方法的路径
                        String methodMapping = methodRequestMapping.value();
                        if (classRequestMapping != null) {
                            //获取全路径  类上的注解不为空 应该和方法上的路径 才是真正的请求路径
                            methodMapping = classRequestMapping.value() + methodMapping;
                        }
                        //存储请求对应的类和方法
                        classMap.put(methodMapping, object);
                        methodMap.put(methodMapping, method);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 在类初始化的时候就对配置文件进行初始化  对servlet初始化  请求对应类和方法进行初始化
     */
    public void init() {
        readConfigFile();
        scanPackage();
        getRequestMapping();
    }

    /**
     * 对请求的方法进行处理  获得处理的后方法所需要的参数  对参数进行一些依赖注入
     * 方法中的参数只允许出现
     * 基本类型必须和注解RequestParam一起使用 声明参数名字 参数名字必须和request中参数名字一致
     * map  和实体类  实体类中属性名字必须和request中参数名字一致  必须有无参构造器   set方法
     *
     * @param method   将执行的方法
     * @param request
     * @param response
     * @return 方法所需要的参数
     */
    private Object[] handleMethodParams(Method method, HttpServletRequest request, HttpServletResponse response) {
        //获取方法中所有的参数
        Parameter[] parameters = method.getParameters();
        //返回方法所需要的参数数组
        Object[] objects = null;
        if (parameters != null && parameters.length != 0) {
            //初始化参数数组
            objects = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                //获取参数的类型
                Class paramType = (Class) parameters[i].getParameterizedType();
                //根据类型进行判断 赋值
                if (paramType == HttpServletRequest.class) {
                    objects[i] = request;
                } else if (paramType == HttpServletResponse.class) {
                    objects[i] = response;
                } else if (paramType == HttpSession.class) {
                    objects[i] = request.getSession();
                } else {
                    //参数上面有注解的  基本类型的参数
                    RequestParam requestParam = (RequestParam) parameters[i].getAnnotation(RequestParam.class);
                    if (requestParam != null) {
                        //基本类型的赋值 先获取所需要的值
                        String key = requestParam.value();
                        String value = request.getParameter(key);
                        objects[i] = handleParamType(paramType, value);
                    } else {
                        //是map  或 实体类进行赋值
                        Object o = null;
                        //map 类型 和他的子类
                        try {
                            if (paramType == Map.class) {
                                o = new HashMap<>();
                                handleDependParam((Map) o, request);
                            } else {
                                o = paramType.newInstance();
                                if (o instanceof Map) {
                                    handleDependParam((Map) o, request);
                                } else {
                                    //实体类 类型
                                    //获取请求的所有参数  对实体类进行响应的注入
                                    Enumeration<String> parameterNames = request.getParameterNames();
                                    while (parameterNames.hasMoreElements()) {
                                        String key = parameterNames.nextElement();
                                        try {
                                            //查看是否有这个属性 没有就跳过 有就赋值 用set方法赋值
                                            Field field = paramType.getDeclaredField(key);
                                            //获取方法名字
                                            String methodName = "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
                                            //获取对应的方法
                                            Method paramMethod = paramType.getDeclaredMethod(methodName, field.getType());
                                            //执行方法
                                            paramMethod.invoke(o, handleParamType(field.getType(), request.getParameter(key)));
                                        } catch (NoSuchFieldException e) {
                                            continue;
                                        } catch (NoSuchMethodException e) {
                                            e.printStackTrace();
                                        } catch (InvocationTargetException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        objects[i] = o;
                    }
                }
            }
        }
        return objects;
    }

    /**
     * 对基本类型的参数进行处理  转化其所需要的值
     *
     * @param paramType 基本数据的类型
     * @param value     值
     * @return 处理后的结果
     */
    private Object handleParamType(Class paramType, String value) {
        if (value == null) {
            return null;
        }
        if (paramType == int.class || paramType == Integer.class) {
            return Integer.parseInt(value);
        } else if (paramType == float.class || paramType == Float.class) {
            return Float.parseFloat(value);
        } else if (paramType == short.class || paramType == Short.class) {
            return Short.parseShort(value);
        } else if (paramType == double.class || paramType == Double.class) {
            return Double.parseDouble(value);
        } else if (paramType == boolean.class || paramType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (paramType == String.class || paramType == Character.class || char.class == paramType) {
            return value;
        } else {
            throw new ParamTypeException("paramType error  " + paramType);
        }
    }

    /**
     * 对参数是map的集合进行赋值
     *
     * @param map     参数map
     * @param request
     */
    private void handleDependParam(Map map, HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String key = parameterNames.nextElement();
            String value = request.getParameter(key);
            map.put(key, value);
        }
    }

    /**
     * 对方法返回的结果进行处理  规定返回结果是HandleResult 类型  属性 String content  Object  object
     * 里面有属性content 的信息是 转发 直接写转发地址  所需要携带的参数   object 必须是map集合
     * 重定向 content 格式 redirect: 地址   会进行重定向
     * json  content  内容 json   会把object的信息转化成json格式 响应回去
     *
     * @param result   方法执行后放回的结果
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    private void handleResultAndResponse(HandleResult result, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (result == null) {
            return;
        }
        String content = result.getContent();
        int colon = content.indexOf(":");
        //是否进行重定向
        if (colon == -1) {
            //json响应
            if ("json".equals(content)) {
                response.getWriter().append(new Gson().toJson(result.getObject()));
            } else {
                //转发响应  处理所需要携带的参数
                Map<String, String> map = (Map) result.getObject();
                Set set = map.keySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    String key = (String) iterator.next();
                    String value = map.get(key);
                    request.setAttribute(key, value);
                }
                //转发
                request.getRequestDispatcher(content).forward(request, response);
            }
        } else {
            //重定向
            String type = content.substring(0, colon);
            //检查格式是否正确
            if ("redirect".equals(type)) {
                response.sendRedirect(content.substring(colon + 1));
            } else {
                throw new NoSuchRedirect("not find redirect " + type);
            }
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //获取请求名字
        String uri = request.getRequestURI();
        //根据配置文件或注解扫描 找到对应请求的类和方法
        Object object = classMap.get(uri);
        Method method = methodMap.get(uri);
        //对方法进行处理  处理并获取方法的参数  对参数进行自动注入
        Object[] objects = handleMethodParams(method, request, response);
        //执行方法 并接受方法的返回值
        HandleResult result = null;
        try {
            result = (HandleResult) method.invoke(object, objects);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        //根据返回值 做出不同的判断 是转发 重定向 json
        handleResultAndResponse(result, request, response);
    }


}
