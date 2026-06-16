package org.leavesmc.leaves.protocol.core.invoker;

import org.leavesmc.leaves.protocol.core.IdentifierSelector;
import org.leavesmc.leaves.protocol.core.LeavesCustomPayload;
import org.leavesmc.leaves.protocol.core.LeavesProtocol;
import org.leavesmc.leaves.protocol.core.ProtocolHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public interface PayloadReceiverInvoker {

    MethodHandle INVOKE0 = InvokerHolder.find(PayloadReceiverInvoker.class, "invoke0", MethodType.methodType(void.class, Object.class, LeavesCustomPayload.class));

    static InvokerHolder<ProtocolHandler.PayloadReceiver, PayloadReceiverInvoker> createHolder(LeavesProtocol owner, Method invokerMethod, ProtocolHandler.PayloadReceiver handler) {
        return InvokerHolder.create(handler, PayloadReceiverInvoker.class, owner, invokerMethod, INVOKE0, handler.stage());
    }

    void invoke0(Object object, LeavesCustomPayload payload) throws Throwable;

    default void invoke(IdentifierSelector selector, LeavesCustomPayload payload) {
        try {
            invoke0(InvokerHolder.select(this, selector), payload);
        } catch (Throwable throwable) {
            InvokerHolder.throwOrLog(throwable);
        }
    }
}
