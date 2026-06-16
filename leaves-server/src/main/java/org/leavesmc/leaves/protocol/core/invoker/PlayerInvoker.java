package org.leavesmc.leaves.protocol.core.invoker;

import net.minecraft.server.level.ServerPlayer;
import org.leavesmc.leaves.protocol.core.LeavesProtocol;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public interface PlayerInvoker {

    MethodHandle INVOKE0 = InvokerHolder.find(PlayerInvoker.class, "invoke0", MethodType.methodType(void.class, ServerPlayer.class));

    static <Handler> InvokerHolder<Handler, PlayerInvoker> createHolder(LeavesProtocol owner, Method invokerMethod, Handler handler) {
        return InvokerHolder.create(handler, PlayerInvoker.class, owner, invokerMethod, INVOKE0, null);
    }

    void invoke0(ServerPlayer player) throws Throwable;

    default void invoke(ServerPlayer player) {
        try {
            invoke0(player);
        } catch (Throwable throwable) {
            InvokerHolder.throwOrLog(throwable);
        }
    }
}
