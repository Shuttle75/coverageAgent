package com.agent;


import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
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

    private static void initLog(String filePath){
        try {
            FileHandler fileHandler = new FileHandler(filePath);
            fileHandler.setFormatter(new MyCustomFormatter());

            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        System.out.println("Agent is running");

        System.out.println(arguments);

        String[] argsArray = arguments.split(";");
        initLog(argsArray[0]);

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(ElementMatchers.nameStartsWith(argsArray[1]))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> {
                    typeDescription.getDeclaredMethods()
                            .forEach(method -> {
                                if(method.isMethod()) {
                                    methodSet.put(method.toString(), "");
                                }
                            });
                    return builder
                            .method(ElementMatchers.any())
                            .intercept(Advice.to(AllMethod.class));}
                )
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

