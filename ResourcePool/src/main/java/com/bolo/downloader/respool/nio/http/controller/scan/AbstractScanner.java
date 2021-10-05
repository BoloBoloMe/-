package com.bolo.downloader.respool.nio.http.controller.scan;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.http.controller.annotate.Controller;
import com.bolo.downloader.respool.nio.http.controller.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.controller.annotate.RequestMethod;
import com.bolo.downloader.respool.nio.http.controller.annotate.Scope;
import com.bolo.downloader.respool.nio.http.controller.invoke.impl.GeneralMethodInvoker;
import com.bolo.downloader.respool.nio.http.controller.invoke.impl.GeneralResultInterpreter;
import com.bolo.downloader.respool.nio.http.controller.invoke.MethodInvoker;
import com.bolo.downloader.respool.nio.http.controller.scan.impl.*;
import io.netty.handler.codec.http.HttpMethod;

import javax.naming.OperationNotSupportedException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract public class AbstractScanner implements MethodMapperScanner {
    private final MyLogger log;

    {
        log = LoggerFactory.getLogger(getClass());
    }

    /**
     * 加载 Class 对象集合的抽象方法
     *
     * @return 将会创建 Mapper 对象的 Class 对象
     */
    public abstract Set<Class<?>> getClasses();

    @Override
    public void scan() {
        synchronized (MethodMapperScanner.class) {
            Set<Class<?>> classSet = getClasses();
            if (Objects.isNull(classSet) || classSet.isEmpty()) {
                return;
            }
            final String rootPath = ScanContextHolder.getValue(ScanContextHolder.KEY_ROOT_PATH, String.class).orElse("");
            final MethodInvoker methodInvoker = ScanContextHolder.getValue(ScanContextHolder.KEY_METHOD_INVOKER, MethodInvoker.class).orElse(new GeneralMethodInvoker(new GeneralResultInterpreter()));
            final MethodMapperContainer methodMapperContainer = new MethodMapperContainer();
            try {
                methodInvoker.setMethodMapperContainer(methodMapperContainer);
                ScanContextHolder.set(ScanContextHolder.KEY_METHOD_INVOKER, methodInvoker);
            } catch (OperationNotSupportedException e) {
                log.error("Setting up the container for the invoker failed.", e);
                throw new RuntimeException(e);
            }
            for (Class<?> targetClass : classSet) {
                try {
                    Optional<Controller> controllerOpt = getAnnotateFromClass(targetClass, Controller.class);
                    if (!controllerOpt.isPresent()) {
                        continue;
                    }
                    List<String> basePaths = new ArrayList<>();
                    getAnnotateFromClass(targetClass, RequestMapping.class).ifPresent(mapping ->
                            Stream.concat(Stream.of(mapping.value()), Stream.of(mapping.path()))
                                    .distinct().filter(path -> Objects.nonNull(path) && !path.isEmpty()).forEach(basePaths::add));
                    String scope = getAnnotateFromClass(targetClass, Scope.class).map(Scope::value).orElse(Scope.SCOPE_SINGLETON);
                    Method[] methods = targetClass.getDeclaredMethods();
                    for (Method method : methods) {
                        getAnnotateFromMethod(method, RequestMapping.class).ifPresent(mapping -> {
                            List<String> paths = Stream.concat(Stream.of(mapping.value()), Stream.of(mapping.path()))
                                    .distinct().filter(Objects::nonNull).collect(Collectors.toList());
                            List<HttpMethod> httpMethods = Stream.of(mapping.method()).map(RequestMethod::getHttpMethod).collect(Collectors.toList());
                            List<String> mixPaths = mixPath(rootPath, basePaths, paths);
                            MethodMapper methodMapper = new MethodMapper(mixPaths, httpMethods, method, buildTargetInstanceFactory(scope, targetClass), targetClass);
                            mixPaths.forEach(path -> methodMapperContainer.put(path, methodMapper));
                        });
                    }
                } catch (Exception e) {
                    log.error("scan class by classpath is failed. error:", e);

                    throw new RuntimeException(e);
                }
            }
        }
    }

    private TargetInstanceFactory buildTargetInstanceFactory(String scope, Class<?> targetClass) {
        if (Scope.SCOPE_PROTOTYPE.equals(scope)) {
            return new PrototypeInstanceFactory(targetClass);
        } else if (Scope.SCOPE_SINGLETON.endsWith(scope)) {
            return new SingletonInstanceFactory(targetClass);
        }
        return new SingletonInstanceFactory(targetClass);
    }

    private <A extends Annotation> Optional<A> getAnnotateFromClass(Class<?> targetClazz, Class<A> annotateClass) {
        return Optional.ofNullable(targetClazz).map(c -> c.getAnnotation(annotateClass));
    }

    private <A extends Annotation> Optional<A> getAnnotateFromMethod(Method method, Class<A> annotateClass) {
        return Optional.ofNullable(method).map(c -> c.getAnnotation(annotateClass));
    }

    private List<String> mixPath(String rootPath, List<String> basePaths, List<String> paths) {
        basePaths = basePaths.isEmpty() ? Collections.singletonList("") : basePaths.stream().map(this::formatPath).collect(Collectors.toList());
        paths = paths.stream().map(this::formatPath).collect(Collectors.toList());
        rootPath = formatPath(rootPath);
        List<String> newPaths = new ArrayList<>(paths.size() * basePaths.size() * 2);
        for (String basePath : basePaths) {
            for (String path : paths) {
                String newPath;
                if (path.length() > 0 && path.charAt(0) != '/') {
                    newPath = rootPath + basePath + path;
                } else {
                    newPath = rootPath + basePath + path;
                }
                newPaths.add(newPath);
                newPaths.add(newPath.endsWith("/") ? newPath.substring(0, newPath.length() - 1) : newPath + "/");
            }
        }
        return newPaths;
    }

    private String formatPath(String path) {
        if (Objects.isNull(path) || "".equals(path)) {
            return "";
        }
        StringBuilder pathBuilder = new StringBuilder(path);
        if (pathBuilder.charAt(0) != '/') {
            pathBuilder.insert(0, '/');
        }
        int lastIndex = pathBuilder.length() - 1;
        if (pathBuilder.charAt(lastIndex) == '/') {
            pathBuilder.deleteCharAt(lastIndex);
        }
        return pathBuilder.toString();
    }
}
