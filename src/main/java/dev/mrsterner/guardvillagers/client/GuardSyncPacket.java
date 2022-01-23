package dev.mrsterner.guardvillagers.client;

import dev.mrsterner.guardvillagers.GuardVillagers;
import dev.mrsterner.guardvillagers.common.entity.GuardEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class GuardSyncPacket {
    public static final Identifier ID = new Identifier(GuardVillagers.MODID, "sync_guard");

    public static void send(PlayerEntity player, GuardEntity entity, int syncId) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(syncId);
        buf.writeInt(entity.getGuard().getId());
        ServerPlayNetworking.send((ServerPlayerEntity) player, ID, buf);
    }
}
