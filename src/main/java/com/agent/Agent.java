package com.agent;


import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatchers;

public class Agent {

    public static final Logger LOGGER = Logger.getAnonymousLogger();
    private static final Map<String, String> methodSet = new ConcurrentHashMap<>();

    public static void removeMethod(String method) {
        methodSet.remove(method);
    }

    private static class MyCustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getMessage() + "\n";
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

        String[] argsArray = arguments.split(";");
        initLogger(argsArray[0]);

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(ElementMatchers.nameStartsWith(argsArray[1]))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> {
                    typeDescription
                        .getDeclaredMethods()
                        .filter(MethodDescription::isMethod)
                        .forEach(method -> methodSet.put(method.toString(), ""));
                    return builder
                            .method(ElementMatchers.any())
                            .intercept(Advice.to(AllMethod.class));
                })
                .installOn(instrumentation);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Running Shutdown Hook. Unload report");
                methodSet.keySet()
                        .stream()
                        .sorted()
                        .forEach(LOGGER::info);
        }));
    }
}

