package org.leavesmc.leaves.protocol.core.invoker;

import org.leavesmc.leaves.protocol.core.LeavesProtocol;
import org.leavesmc.leaves.protocol.core.ProtocolHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public interface InitInvoker {

    MethodHandle INVOKE0 = InvokerHolder.find(InitInvoker.class, "invoke0", MethodType.methodType(void.class));

    static InvokerHolder<ProtocolHandler.Init, InitInvoker> createHolder(LeavesProtocol owner, Method invokerMethod, ProtocolHandler.Init handler) {
        return InvokerHolder.create(handler, InitInvoker.class, owner, invokerMethod, INVOKE0, null);
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
