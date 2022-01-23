package dev.mrsterner.guardvillagers.client.networking;

import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class GuardFollowPacket {
    private final int entityId;

    public GuardFollowPacket(int entityId) {
        this.entityId = entityId;
    }

    public static GuardFollowPacket decode(PacketByteBuf buf) {
        return new GuardFollowPacket(buf.readInt());
    }

    public static void encode(GuardFollowPacket msg, PacketByteBuf buf) {
        buf.writeInt(msg.entityId);
    }

    public int getEntityId() {
        return this.entityId;
    }
    /*

    public static void handle(GuardFollowPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (msg != null) {
                ((NetworkEvent.Context) context.get()).enqueueWork(new Runnable() {
                    @Override
                    public void run() {
                        ServerPlayerEntity player = ((NetworkEvent.Context) context.get()).getSender();
                        if (player != null && player.level instanceof ServerLevel) {
                            Entity entity = player.world.getEntity(msg.getEntityId());
                            if (entity instanceof GuardEntity guard) {
                                guard.setFollowing(!guard.isFollowing());
                                guard.setOwnerId(player.getUUID());
                                guard.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
                            }
                        }
                    }
                });
            }
        });
        context.get().setPacketHandled(true);

    }


     */
}
