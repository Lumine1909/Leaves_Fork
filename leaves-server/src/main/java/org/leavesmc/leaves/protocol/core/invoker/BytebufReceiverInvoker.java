package org.leavesmc.leaves.protocol.core.invoker;

import net.minecraft.network.FriendlyByteBuf;
import org.leavesmc.leaves.protocol.core.IdentifierSelector;
import org.leavesmc.leaves.protocol.core.LeavesProtocol;
import org.leavesmc.leaves.protocol.core.ProtocolHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public interface BytebufReceiverInvoker {

    MethodHandle INVOKE0 = InvokerHolder.find(BytebufReceiverInvoker.class, "invoke0", MethodType.methodType(Object.class, Object.class, FriendlyByteBuf.class));

    static InvokerHolder<ProtocolHandler.BytebufReceiver, BytebufReceiverInvoker> createHolder(LeavesProtocol owner, Method invokerMethod, ProtocolHandler.BytebufReceiver handler) {
        return InvokerHolder.create(handler, BytebufReceiverInvoker.class, owner, invokerMethod, INVOKE0, handler.stage());
    }

    Object invoke0(Object object, FriendlyByteBuf buf) throws Throwable;

    default boolean invoke(IdentifierSelector selector, FriendlyByteBuf buf) {
        try {
            return invoke0(InvokerHolder.select(this, selector), buf) instanceof Boolean b && b;
        } catch (Throwable throwable) {
            InvokerHolder.throwOrLog(throwable);
        }
        return false;
    }
}