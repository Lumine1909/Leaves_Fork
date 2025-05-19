package org.leavesmc.leaves.protocol.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface LeavesCustomPayload<B extends FriendlyByteBuf> extends CustomPacketPayload {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ID {
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Codec {
    }

    void encode(B buf);

    interface Game extends LeavesCustomPayload<RegistryFriendlyByteBuf> {
        default void encode(RegistryFriendlyByteBuf buf) {
            var location = LeavesProtocolManager.IDS.get(this.getClass());
            var codec = LeavesProtocolManager.CODECS_GAME.get(this.getClass());
            if (location == null || codec == null) {
                throw new IllegalArgumentException("Payload " + this.getClass() + " is not configured correctly");
            }
            buf.writeResourceLocation(location);
            codec.encode(buf, this);
        }
    }

    interface Config extends LeavesCustomPayload<FriendlyByteBuf> {
        default void encode(FriendlyByteBuf buf) {
            var location = LeavesProtocolManager.IDS.get(this.getClass());
            var codec = LeavesProtocolManager.CODECS_CONFIG.get(this.getClass());
            if (location == null || codec == null) {
                throw new IllegalArgumentException("Payload " + this.getClass() + " is not configured correctly");
            }
            buf.writeResourceLocation(location);
            codec.encode(buf, this);
        }
    }

    @Override
    default @NotNull Type<? extends CustomPacketPayload> type() {
        throw new UnsupportedOperationException("Not supported");
    }
}