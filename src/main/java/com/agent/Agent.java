package com.agent;


import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.logging.*;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;


public class Agent {

    public static final Logger LOGGER = Logger.getAnonymousLogger();
    private static final String PATH = "com.example";

    private static class MyCustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getMessage() + "\n";
        }
    }

    static {
        try {
            FileHandler fileHandler = new FileHandler("/tmp/coverage.log");
            fileHandler.setFormatter(new MyCustomFormatter());

            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        System.out.println("Agent is running");

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(ElementMatchers.nameStartsWith(PATH))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) -> {
                    typeDescription.getDeclaredMethods()
                            .forEach(method -> {
                                if(method.isMethod()) {
                                    LOGGER.info(typeDescription.getCanonicalName() + " - " + method.getName());
                                }
                            });
                    LOGGER.info("--------------------------------");
                    return builder
                            .method(ElementMatchers.any())
                            .intercept(Advice.to(AllMethod.class));}
                )
                .installOn(instrumentation);

    }
}