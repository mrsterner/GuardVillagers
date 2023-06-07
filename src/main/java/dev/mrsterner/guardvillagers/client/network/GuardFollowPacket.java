package dev.mrsterner.guardvillagers.client.network;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class GuardFollowPacket implements FabricPacket {
    public static final PacketType<GuardFollowPacket> PACKET_TYPE = PacketType.create(new Identifier(GuardVillagers.MODID, "guard_follow"), GuardFollowPacket::read);
    private final int id;

    public GuardFollowPacket(int id) {
        this.id = id;
    }


    public static void handle(GuardFollowPacket guardFollowPacket, ServerPlayerEntity serverPlayerEntity, PacketSender packetSender) {
        int entityId = guardFollowPacket.id;

        Entity entity = serverPlayerEntity.getWorld().getEntityById(entityId);
        if(entity instanceof GuardEntity guardEntity){
            guardEntity.setFollowing(!guardEntity.isFollowing());
            guardEntity.setOwnerId(serverPlayerEntity.getUuid());
            guardEntity.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1,1);
        }
    }

    public static GuardFollowPacket read(PacketByteBuf buf){
        int id = buf.readInt();
        return new GuardFollowPacket(id);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(id);
    }

    @Override
    public PacketType<?> getType() {
        return PACKET_TYPE;
    }
}
