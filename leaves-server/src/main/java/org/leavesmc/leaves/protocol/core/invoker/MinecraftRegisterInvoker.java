package org.leavesmc.leaves.protocol.core.invoker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.leavesmc.leaves.protocol.core.IdentifierSelector;
import org.leavesmc.leaves.protocol.core.LeavesProtocol;
import org.leavesmc.leaves.protocol.core.ProtocolHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public interface MinecraftRegisterInvoker {

    MethodHandle INVOKE0 = InvokerHolder.find(MinecraftRegisterInvoker.class, "invoke0", MethodType.methodType(void.class, Object.class, FriendlyByteBuf.class));

    static InvokerHolder<ProtocolHandler.MinecraftRegister, MinecraftRegisterInvoker> createHolder(LeavesProtocol owner, Method invokerMethod, ProtocolHandler.MinecraftRegister handler) {
        return InvokerHolder.create(handler, MinecraftRegisterInvoker.class, owner, invokerMethod, INVOKE0, handler.stage());
    }

    void invoke0(Object object, Identifier id) throws Throwable;

    default void invoke(IdentifierSelector selector, Identifier id) {
        try {
            invoke0(InvokerHolder.select(this, selector), id);
        } catch (Throwable throwable) {
            InvokerHolder.throwOrLog(throwable);
        }
    }
}