package com.agent;


import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatchers;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.groupingBy;

public class Agent {

    public static final Logger LOGGER = Logger.getAnonymousLogger();
    private static final Map<String, String> CALL_METHOD_MAP = new ConcurrentHashMap<>();
    private static final Map<String, MethodDescription.InDefinedShape> LOADER_METHOD_MAP = new ConcurrentHashMap<>();

    public static void removeMethod(String method) {
        CALL_METHOD_MAP.put(method, "");
    }

    private static class MyCustomFormatter extends Formatter {
        @Override
        public String format(LogRecord logRecord) {
            return logRecord.getMessage() + "\n";
        }
    }

    private static void initLogger(String filePath) throws IOException {
        FileHandler fileHandler = new FileHandler(filePath);
        fileHandler.setFormatter(new MyCustomFormatter());

        LOGGER.addHandler(fileHandler);
        LOGGER.setUseParentHandlers(false);
    }

    public static void premain(String arguments, Instrumentation instrumentation) throws IOException {
        System.out.println("Agent is running");

        String[] argsArray = arguments.split(",");
        initLogger(argsArray[0]);

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(ElementMatchers.nameStartsWith(argsArray[1]))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> {
                    typeDescription
                        .getDeclaredMethods()
                            .stream()
                            .filter(MethodDescription::isMethod)
                            .forEach(method -> LOADER_METHOD_MAP.put(method.toString(), method));
                    return builder
                            .method(ElementMatchers.any())
                            .intercept(Advice.to(AllMethod.class));
                })
                .installOn(instrumentation);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Running Shutdown Hook. Unload report");
            LOGGER.info(packagesLoad().toString());
        }));
    }

    private static JsonArray packagesLoad() {
        JsonArray jsonArray = new JsonArray();
        LOADER_METHOD_MAP.values().stream()
                .sorted(Comparator.comparing(Object::toString))
                .filter(shape -> shape.getDeclaringType().getPackage() != null)
                .collect(groupingBy(shape ->
                        shape.getDeclaringType().getPackage().getName(),
                        averagingDouble(shape -> CALL_METHOD_MAP.containsKey(shape.toString()) ? 1 : 0)))
                .forEach((packageDescription, coverage) -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("name", packageDescription);
                    jsonObject.add("classes", declaringTypesLoad(packageDescription));
                    jsonObject.addProperty("coverage", coverage);
                    jsonArray.add(jsonObject);
                });
        return jsonArray;
    }

    private static JsonArray declaringTypesLoad(String packageDescription) {
        JsonArray jsonArray = new JsonArray();
        LOADER_METHOD_MAP.values().stream()
                .filter(shape -> shape.getDeclaringType().getPackage() != null)
                .filter(shape -> shape.getDeclaringType().getPackage().getName().equals(packageDescription))
                .collect(groupingBy(shape ->
                                shape.getDeclaringType().getName(),
                        averagingDouble(shape -> CALL_METHOD_MAP.containsKey(shape.toString()) ? 1 : 0)))
                .forEach((type, coverage) -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("name", type);
                    jsonObject.add("methods", definedShapesLoad(type));
                    jsonObject.addProperty("coverage", coverage);
                    jsonArray.add(jsonObject);
                });
        return jsonArray;
    }

    private static JsonArray definedShapesLoad(String declaringType) {
        JsonArray jsonArray = new JsonArray();
        LOADER_METHOD_MAP.values().stream()
                .filter(shape -> shape.getDeclaringType().getName().equals(declaringType))
                .forEach(shape -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("name", shape.toString());
                    jsonObject.addProperty("coverage", CALL_METHOD_MAP.containsKey(shape.toString()));
                    jsonArray.add(jsonObject);
                });
        return jsonArray;
    }
}

