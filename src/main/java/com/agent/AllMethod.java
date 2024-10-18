package com.agent;

import net.bytebuddy.asm.Advice;

import static com.agent.Agent.LOGGER;

public class AllMethod {

    @Advice.OnMethodExit
    static void getAllMethods(@Advice.Origin String method) {
        LOGGER.info("Trace call --- " + method);
    }

}
