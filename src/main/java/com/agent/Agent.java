package com.agent;


import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class Agent {

    public static void premain(String arguments, Instrumentation instrumentation) {

        System.out.println("Agent for get all methods");

        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type((ElementMatchers.nameStartsWith("com.example")))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) ->
                        builder
                                .method(ElementMatchers.any())
                                .intercept(Advice.to(AllMethod.class)))
                .installOn(instrumentation);
    }
}

class AllMethod {

    @Advice.OnMethodExit
    static void getAllMethods(@Advice.Origin String method) throws Exception {

        System.out.println("Trace --- " + method);
    }

}
