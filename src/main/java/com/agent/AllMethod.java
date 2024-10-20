package com.agent;


import net.bytebuddy.asm.Advice;

public class AllMethod {

    @Advice.OnMethodExit
    static void getAllMethods(@Advice.Origin String method) {
        Agent.removeMethod(method);
    }

}
