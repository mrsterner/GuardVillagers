package dev.mrsterner.guardvillagers.client.network;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class GuardPatrolPacket implements FabricPacket {
    public static final PacketType<GuardPatrolPacket> PACKET_TYPE = PacketType.create(new Identifier(GuardVillagers.MODID, "guard_patrol"), GuardPatrolPacket::read);
    private final int id;
    private final boolean pressed;

    public GuardPatrolPacket(int id, boolean pressed) {
        this.id = id;
        this.pressed = pressed;
    }

    public static void handle(GuardPatrolPacket packet, ServerPlayerEntity serverPlayerEntity, PacketSender buf) {
        int entityId = packet.id;
        boolean pressed = packet.pressed;

        Entity entity = serverPlayerEntity.getWorld().getEntityById(entityId);
        if(entity instanceof GuardEntity guardEntity) {
            BlockPos pos = guardEntity.getBlockPos();
            if (guardEntity.getBlockPos() != null) {
                guardEntity.setPatrolPos(pos);
            }
            guardEntity.setPatrolling(pressed);
        }
    }

    private static GuardPatrolPacket read(PacketByteBuf buf) {
        int id = buf.readInt();
        boolean pressed = buf.readBoolean();
        return new GuardPatrolPacket(id, pressed);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(id);
        buf.writeBoolean(pressed);
    }

    @Override
    public PacketType<?> getType() {
        return PACKET_TYPE;
    }
}
