package org.leavesmc.leaves.protocol.core;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.tuple.Pair;
import org.leavesmc.leaves.LeavesLogger;
import org.leavesmc.leaves.config.InternalConfigProvider;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// TODO refactor
public class LeavesProtocolManager {

    private static final LeavesLogger LOGGER = LeavesLogger.LOGGER;

    private static final Map<LeavesProtocol, List<InvokerHolder<?>>> REGISTERED_PROTOCOLS = new HashMap<>();

    private static final Map<Class<? extends LeavesCustomPayload<? super RegistryFriendlyByteBuf>>, InvokerHolder<ProtocolHandler.PayloadReceiver>> PAYLOAD_RECEIVERS = new HashMap<>();
    public static final Map<Class<? extends LeavesCustomPayload<? super RegistryFriendlyByteBuf>>, ResourceLocation> IDS = new HashMap<>();
    public static final Map<Class<? extends LeavesCustomPayload<RegistryFriendlyByteBuf>>, StreamCodec<RegistryFriendlyByteBuf, LeavesCustomPayload<RegistryFriendlyByteBuf>>> CODECS_GAME = new HashMap<>();
    public static final Map<Class<? extends LeavesCustomPayload<FriendlyByteBuf>>, StreamCodec<FriendlyByteBuf, LeavesCustomPayload<FriendlyByteBuf>>> CODECS_CONFIG = new HashMap<>();
    public static final Map<ResourceLocation, StreamCodec<RegistryFriendlyByteBuf, LeavesCustomPayload<RegistryFriendlyByteBuf>>> ID2CODEC_GAME = new HashMap<>();
    public static final Map<ResourceLocation, StreamCodec<FriendlyByteBuf, LeavesCustomPayload<FriendlyByteBuf>>> ID2CODEC_CONFIG = new HashMap<>();

    private static final Map<String, InvokerHolder<ProtocolHandler.BytebufReceiver>> STRICT_BYTEBUF_RECEIVERS = new HashMap<>();
    private static final Map<String, InvokerHolder<ProtocolHandler.BytebufReceiver>> NAMESPACED_BYTEBUF_RECEIVERS = new HashMap<>();
    private static final List<InvokerHolder<ProtocolHandler.BytebufReceiver>> GENERIC_BYTEBUF_RECEIVERS = new ArrayList<>();

    private static final Map<String, InvokerHolder<ProtocolHandler.MinecraftRegister>> STRICT_MINECRAFT_REGISTER = new HashMap<>();
    private static final Map<String, InvokerHolder<ProtocolHandler.MinecraftRegister>> NAMESPACED_MINECRAFT_REGISTER = new HashMap<>();
    private static final List<InvokerHolder<ProtocolHandler.MinecraftRegister>> WILD_MINECRAFT_REGISTER = new ArrayList<>();

    private static final Map<InvokerHolder<ProtocolHandler.Ticker>, Integer> TICKERS_INTERVAL = new HashMap<>();

    private static final List<InvokerHolder<ProtocolHandler.PlayerJoin>> PLAYER_JOIN = new ArrayList<>();
    private static final List<InvokerHolder<ProtocolHandler.PlayerLeave>> PLAYER_LEAVE = new ArrayList<>();
    private static final List<InvokerHolder<ProtocolHandler.ReloadServer>> RELOAD_SERVER = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public static void init() {
        for (Class<?> clazz : getClasses("org.leavesmc.leaves.protocol")) {
            if (clazz.isAssignableFrom(LeavesCustomPayload.class)) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (!Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    try {
                        final LeavesCustomPayload.ID id = field.getAnnotation(LeavesCustomPayload.ID.class);
                        if (id != null && field.getType() == ResourceLocation.class) {
                            IDS.put((Class<? extends LeavesCustomPayload<? super RegistryFriendlyByteBuf>>) clazz, (ResourceLocation) field.get(null));
                        }
                        final LeavesCustomPayload.Codec codec = field.getAnnotation(LeavesCustomPayload.Codec.class);
                        if (codec != null && field.getType() == StreamCodec.class) {
                            if (clazz.isAssignableFrom(LeavesCustomPayload.Config.class)) {
                                CODECS_CONFIG.put((Class<? extends LeavesCustomPayload<FriendlyByteBuf>>) clazz, (StreamCodec<FriendlyByteBuf, LeavesCustomPayload<FriendlyByteBuf>>) field.get(null));
                            }
                            if (clazz.isAssignableFrom(LeavesCustomPayload.Game.class)) {
                                CODECS_GAME.put((Class<? extends LeavesCustomPayload<RegistryFriendlyByteBuf>>) clazz, (StreamCodec<RegistryFriendlyByteBuf, LeavesCustomPayload<RegistryFriendlyByteBuf>>) field.get(null));
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                continue;
            }

            final LeavesProtocol.Register register = clazz.getAnnotation(LeavesProtocol.Register.class);
            if (register == null) {
                continue;
            }
            LeavesProtocol protocol;
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                protocol = (LeavesProtocol) constructor.newInstance();
            } catch (Throwable throwable) {
                LOGGER.severe("Failed to load class " + clazz.getName() + ". " + throwable);
                return;
            }

            for (final Method method : clazz.getDeclaredMethods()) {
                if (method.isBridge() || method.isSynthetic()) {
                    continue;
                }
                method.setAccessible(true);

                final ProtocolHandler.Init init = method.getAnnotation(ProtocolHandler.Init.class);
                if (init != null) {
                    InvokerHolder<ProtocolHandler.Init> holder = new InvokerHolder<>(protocol, method, init);
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                    try {
                        holder.invokeEmpty();
                    } catch (RuntimeException exception) {
                        LOGGER.severe("Failed to invoke init method " + method.getName() + " in " + clazz.getName() + ", " + exception.getCause() + ": " + exception.getMessage());
                    }
                    continue;
                }

                final ProtocolHandler.PayloadReceiver payloadReceiver = method.getAnnotation(ProtocolHandler.PayloadReceiver.class);
                if (payloadReceiver != null) {
                    InvokerHolder<ProtocolHandler.PayloadReceiver> holder = new InvokerHolder<>(protocol, method, payloadReceiver);
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                    PAYLOAD_RECEIVERS.put(payloadReceiver.payload(), holder);
                    continue;
                }

                final ProtocolHandler.BytebufReceiver bytebufReceiver = method.getAnnotation(ProtocolHandler.BytebufReceiver.class);
                if (bytebufReceiver != null) {
                    String key = bytebufReceiver.key();
                    InvokerHolder<ProtocolHandler.BytebufReceiver> holder = new InvokerHolder<>(protocol, method, bytebufReceiver);
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                    if (bytebufReceiver.onlyNamespace()) {
                        NAMESPACED_BYTEBUF_RECEIVERS.put(key.isEmpty() ? register.namespace() : key, holder);
                    } else {
                        if (key.isEmpty()) {
                            GENERIC_BYTEBUF_RECEIVERS.add(holder);
                        } else {
                            if (key.contains(":")) {
                                STRICT_BYTEBUF_RECEIVERS.put(key, holder);
                            } else {
                                STRICT_BYTEBUF_RECEIVERS.put(register.namespace() + key, holder);
                            }
                        }
                    }
                    continue;
                }

                final ProtocolHandler.Ticker ticker = method.getAnnotation(ProtocolHandler.Ticker.class);
                if (ticker != null) {
                    var holder = new InvokerHolder<>(protocol, method, ticker);
                    if (ticker.interval() != -1) {
                        TICKERS_INTERVAL.put(holder, ticker.interval());
                    } else {
                        try {
                            TICKERS_INTERVAL.put(holder, InternalConfigProvider.INSTANCE.getConfig(ticker.configName()).getInt());
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid ticker: " + ticker);
                        }
                    }
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                    continue;
                }

                final ProtocolHandler.PlayerJoin playerJoin = method.getAnnotation(ProtocolHandler.PlayerJoin.class);
                if (playerJoin != null) {
                    var holder = new InvokerHolder<>(protocol, method, playerJoin);
                    PLAYER_JOIN.add(holder);
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                    continue;
                }

                final ProtocolHandler.PlayerLeave playerLeave = method.getAnnotation(ProtocolHandler.PlayerLeave.class);
                if (playerLeave != null) {
                    var holder = new InvokerHolder<>(protocol, method, playerLeave);
                    PLAYER_LEAVE.add(holder);
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                    continue;
                }

                final ProtocolHandler.ReloadServer reloadServer = method.getAnnotation(ProtocolHandler.ReloadServer.class);
                if (reloadServer != null) {
                    var holder = new InvokerHolder<>(protocol, method, reloadServer);
                    RELOAD_SERVER.add(holder);
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                    continue;
                }

                final ProtocolHandler.MinecraftRegister minecraftRegister = method.getAnnotation(ProtocolHandler.MinecraftRegister.class);
                if (minecraftRegister != null) {
                    String key = minecraftRegister.key();
                    InvokerHolder<ProtocolHandler.MinecraftRegister> holder = new InvokerHolder<>(protocol, method, minecraftRegister);
                    if (minecraftRegister.onlyNamespace()) {
                        NAMESPACED_MINECRAFT_REGISTER.put(key.isEmpty() ? register.namespace() : key, holder);
                    } else {
                        if (key.isEmpty()) {
                            WILD_MINECRAFT_REGISTER.add(holder);
                        } else {
                            if (key.contains(":")) {
                                STRICT_MINECRAFT_REGISTER.put(key, holder);
                            } else {
                                STRICT_MINECRAFT_REGISTER.put(register.namespace() + key, holder);
                            }
                        }
                    }
                    REGISTERED_PROTOCOLS.computeIfAbsent(protocol, k -> new ArrayList<>()).add(holder);
                }
            }
        }
        for (var idInfo : IDS.entrySet()) {
            var codecConfig = CODECS_CONFIG.get(idInfo.getKey());
            if (codecConfig != null) {
                ID2CODEC_CONFIG.put(idInfo.getValue(), codecConfig);
            }
            var codecGame = CODECS_GAME.get(idInfo.getKey());
            if (codecConfig != null) {
                ID2CODEC_GAME.put(idInfo.getValue(), codecGame);
            }
        }
    }

    public static LeavesCustomPayload<? extends FriendlyByteBuf> decode(ResourceLocation location, FriendlyByteBuf buf) {
        if (buf instanceof RegistryFriendlyByteBuf registry) {
            var codec = ID2CODEC_GAME.get(location);
            if (codec == null) {
                return null;
            }
            return codec.decode(registry);
        } else {
            var codec = ID2CODEC_CONFIG.get(location);
            if (codec == null) {
                return null;
            }
            return codec.decode(buf);
        }
    }

    public static <T extends FriendlyByteBuf> void encode(T buf, LeavesCustomPayload<T> payload) {
        payload.encode(buf);
    }

    public static void handlePayload(ServerPlayer player, LeavesCustomPayload<?> payload) {
        InvokerHolder<ProtocolHandler.PayloadReceiver> holder;
        if ((holder = PAYLOAD_RECEIVERS.get(payload.getClass())) != null) {
            holder.invokePayload(player, payload);
        }
    }

    public static void handleBytebuf(ServerPlayer player, ResourceLocation location, ByteBuf buf) {
        for (var holder : GENERIC_BYTEBUF_RECEIVERS) {
            holder.invokeBuf(player, buf);
        }
        InvokerHolder<ProtocolHandler.BytebufReceiver> holder;
        if ((holder = NAMESPACED_BYTEBUF_RECEIVERS.get(location.getNamespace())) != null) {
            holder.invokeBuf(player, buf);
        }
        if ((holder = STRICT_BYTEBUF_RECEIVERS.get(location.toString())) != null) {
            holder.invokeBuf(player, buf);
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleTick(long tickCount) {
        try {
            for (var protocol : LeavesProtocol.reloadPending) {
                for (var holder : REGISTERED_PROTOCOLS.get(protocol)) {
                    if (((Annotation) holder.handler()).annotationType() == ProtocolHandler.Ticker.class) {
                        ProtocolHandler.Ticker ticker = (ProtocolHandler.Ticker) holder.handler();
                        TICKERS_INTERVAL.put(
                            (InvokerHolder<ProtocolHandler.Ticker>) holder,
                            InternalConfigProvider.INSTANCE.getConfig(ticker.configName()).getInt()
                        );
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to handle Ticker event", e);
        }
        for (var tickerInfo : TICKERS_INTERVAL.entrySet()) {
            if (tickCount % tickerInfo.getValue() == 0) {
                tickerInfo.getKey().invokeEmpty();
            }
        }
    }

    public static void handlePlayerJoin(ServerPlayer player) {
        for (var join : PLAYER_JOIN) {
            join.invokePlayer(player);
        }
        ProtocolUtils.sendPayloadPacket(player, new FabricRegisterPayload(collectKnownId()));
    }

    public static void handlePlayerLeave(ServerPlayer player) {
        for (var leave : PLAYER_LEAVE) {
            leave.invokePlayer(player);
        }
    }

    public static void handleServerReload() {
        for (var reload : RELOAD_SERVER) {
            reload.invokeEmpty();
        }
    }

    public static void handleMinecraftRegister(String channelId, ServerPlayer player) {
        ResourceLocation location = ResourceLocation.tryParse(channelId);
        if (location == null) {
            return;
        }

        for (var wildHolder : WILD_MINECRAFT_REGISTER) {
            wildHolder.invokeKey(player, location);
        }

        InvokerHolder<ProtocolHandler.MinecraftRegister> holder;
        if ((holder = STRICT_MINECRAFT_REGISTER.get(location.toString())) != null) {
            holder.invokeKey(player, location);
        }
        if ((holder = NAMESPACED_MINECRAFT_REGISTER.get(location.getNamespace())) != null) {
            holder.invokeKey(player, location);
        }
    }

    private static Set<String> collectKnownId() {
        Set<String> set = new HashSet<>();
        PAYLOAD_RECEIVERS.forEach((clazz, holder) -> {
            if (holder.owner().isActive()) {
                set.add(IDS.get(clazz).toString());
            }
        });
        STRICT_BYTEBUF_RECEIVERS.forEach((key, holder) -> {
            if (holder.owner().isActive()) {
                set.add(key);
            }
        });
        return set;
    }

    public static Set<Class<?>> getClasses(String pack) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        String packageDirName = pack.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
                    findClassesInPackageByFile(pack, filePath, classes);
                } else if ("jar".equals(protocol)) {
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        findClassesInPackageByJar(pack, entries, packageDirName, classes);
                    } catch (IOException exception) {
                        LOGGER.warning("Failed to load jar file, " + exception.getCause() + ": " + exception.getMessage());
                    }
                }
            }
        } catch (IOException exception) {
            LOGGER.warning("Failed to load classes, " + exception.getCause() + ": " + exception.getMessage());
        }
        return classes;
    }

    private static void findClassesInPackageByFile(String packageName, String packagePath, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles((file) -> file.isDirectory() || file.getName().endsWith(".class"));
        if (dirfiles != null) {
            for (File file : dirfiles) {
                if (file.isDirectory()) {
                    findClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), classes);
                } else {
                    String className = file.getName().substring(0, file.getName().length() - 6);
                    try {
                        classes.add(Class.forName(packageName + '.' + className));
                    } catch (ClassNotFoundException exception) {
                        LOGGER.warning("Failed to load class " + className + ", " + exception.getCause() + ": " + exception.getMessage());
                    }
                }
            }
        }
    }

    private static void findClassesInPackageByJar(String packageName, Enumeration<JarEntry> entries, String packageDirName, Set<Class<?>> classes) {
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }
            if (name.startsWith(packageDirName)) {
                int idx = name.lastIndexOf('/');
                if (idx != -1) {
                    packageName = name.substring(0, idx).replace('/', '.');
                }
                if (name.endsWith(".class") && !entry.isDirectory()) {
                    String className = name.substring(packageName.length() + 1, name.length() - 6);
                    try {
                        classes.add(Class.forName(packageName + '.' + className));
                    } catch (ClassNotFoundException exception) {
                        LOGGER.warning("Failed to load class " + className + ", " + exception.getCause() + ": " + exception.getMessage());
                    }
                }
            }
        }
    }

    public record FabricRegisterPayload(Set<String> channels) implements LeavesCustomPayload.Game {

        @ID
        public static final ResourceLocation ID = ResourceLocation.tryParse("minecraft:register");

        @Codec
        public static StreamCodec<FriendlyByteBuf, FabricRegisterPayload> CODEC = StreamCodec.of(
            FabricRegisterPayload::write,
            v -> {
                throw new UnsupportedOperationException();
            }
        );

        public static void write(FriendlyByteBuf buf, FabricRegisterPayload payload) {
            boolean first = true;

            ResourceLocation channel;
            for (Iterator<String> var3 = payload.channels.iterator(); var3.hasNext(); buf.writeBytes(channel.toString().getBytes(StandardCharsets.US_ASCII))) {
                channel = ResourceLocation.parse(var3.next());
                if (first) {
                    first = false;
                } else {
                    buf.writeByte(0);
                }
            }
        }
    }
}