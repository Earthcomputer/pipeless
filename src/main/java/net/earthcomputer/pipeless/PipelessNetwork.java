package net.earthcomputer.pipeless;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class PipelessNetwork {
    private PipelessNetwork() {}

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Pipeless.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    static {
        int id = 0;

        INSTANCE.registerMessage(
            id++,
            ClientboundSetItemBobOffsetPacket.class,
            ClientboundSetItemBobOffsetPacket::write,
            ClientboundSetItemBobOffsetPacket::new,
            (packet, ctx) -> {
                ctx.get().enqueueWork(() -> handleClientBobOffset(packet));
                ctx.get().setPacketHandled(true);
            },
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void register() {
        // load class
    }

    public static void updateClientBobOffset(ServerPlayer player, ItemEntity item) {
        INSTANCE.sendTo(
            new ClientboundSetItemBobOffsetPacket(item.getId(), item.getAge(), item.bobOffs),
            player.connection.connection,
            NetworkDirection.PLAY_TO_CLIENT
        );
    }

    private static void handleClientBobOffset(ClientboundSetItemBobOffsetPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Entity entity = level.getEntity(packet.itemId());
            if (entity instanceof ItemEntity item) {
                item.age = packet.age();
                item.bobOffs = packet.bobOffset();
            }
        }
    }

    private record ClientboundSetItemBobOffsetPacket(int itemId, int age, float bobOffset) {
        ClientboundSetItemBobOffsetPacket(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readVarInt(), buf.readFloat());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(this.itemId);
            buf.writeVarInt(this.age);
            buf.writeFloat(this.bobOffset);
        }
    }
}
