package org.leavesmc.leaves.protocol.core.invoker;

import org.leavesmc.leaves.LeavesConfig;
import org.leavesmc.leaves.LeavesLogger;
import org.leavesmc.leaves.protocol.core.IdentifierSelector;
import org.leavesmc.leaves.protocol.core.LeavesProtocol;
import org.leavesmc.leaves.protocol.core.ProtocolHandler;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public record InvokerHolder<Handler, Invoker>(
    LeavesProtocol owner,
    Handler handler,
    Invoker invoker,
    ProtocolHandler.Stage stage
) {

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    private static final Map<Object, InvokerHolder<?, ?>> holderMap = new IdentityHashMap<>();

    public InvokerHolder(LeavesProtocol owner, Handler handler, Invoker invoker, ProtocolHandler.Stage stage) {
        this.owner = owner;
        this.handler = handler;
        this.invoker = invoker;
        this.stage = stage;
        holderMap.put(invoker, this);
    }

    @SuppressWarnings("unchecked")
    public static <Handler, Invoker> InvokerHolder<Handler, Invoker> create(
        Handler handler,
        Class<Invoker> invokerType,
        LeavesProtocol owner,
        Method targetMethod,
        MethodHandle invokerHandle,
        ProtocolHandler.Stage stage
    ) {
        boolean isStatic = Modifier.isStatic(targetMethod.getModifiers());
        try {
            CallSite lambdaSite = LambdaMetafactory.metafactory(
                lookup,
                "invoke0",
                MethodType.methodType(invokerType, isStatic ? new Class[0] : new Class[]{LeavesProtocol.class}),
                invokerHandle.type(),
                invokerHandle,
                MethodType.methodType(targetMethod.getReturnType(), targetMethod.getParameterTypes())
            );

            Invoker invoker = (Invoker) lambdaSite.getTarget().invokeExact(isStatic ? new Object[0] : new Object[]{owner});
            return new InvokerHolder<>(owner, handler, invoker, stage);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static MethodHandle find(Class<?> clazz, String name, MethodType methodType) {
        try {
            return lookup.findVirtual(clazz, name, methodType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwOrLog(Throwable t) throws T {
        if (LeavesConfig.protocol.strictMode) {
            throw (T) t;
        }
        LeavesLogger.LOGGER.error("Exception on invoking protocol ", t);
    }

    public static Object select(Object invoker, IdentifierSelector selector) {
        return selector.select(holderMap.get(invoker).stage);
    }

    public LeavesProtocol owner() {
        return owner;
    }

    public void run(Consumer<Invoker> invoke) {
        run(false, invoke);
    }

    public boolean result(Function<Invoker, Object> invoke) {
        return result(false, invoke) instanceof Boolean b && b;
    }

    public void run(boolean force, Consumer<Invoker> invoke) {
        result(force, invoker -> {
            invoke.accept(invoker);
            return null;
        });
    }

    public Object result(boolean force, Function<Invoker, Object> invoke) {
        if (!force && !owner.isActive()) {
            return null;
        }
        try {
            return invoke.apply(invoker);
        } catch (Exception e) {
            throwOrLog(e);
        }
        return null;
    }
}
