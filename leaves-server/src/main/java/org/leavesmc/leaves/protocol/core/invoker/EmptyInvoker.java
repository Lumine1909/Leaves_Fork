package org.leavesmc.leaves.protocol.core.invoker;

import org.leavesmc.leaves.protocol.core.LeavesProtocol;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public interface EmptyInvoker {

    MethodHandle INVOKE0 = InvokerHolder.find(EmptyInvoker.class, "invoke0", MethodType.methodType(void.class));

    static <Handler> InvokerHolder<Handler, EmptyInvoker> createHolder(LeavesProtocol owner, Method invokerMethod, Handler handler) {
        return InvokerHolder.create(handler, EmptyInvoker.class, owner, invokerMethod, INVOKE0, null);
    }

    void invoke0() throws Throwable;

    default void invoke() {
        try {
            invoke0();
        } catch (Throwable throwable) {
            InvokerHolder.throwOrLog(throwable);
        }
    }
}
