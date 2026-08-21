package com.onthehill.templatemod.network;

import com.onthehill.templatemod.TemplateMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client marker payload instructing the receiving client to open
 * the config screen positioned on the admin tab. Sent in response to the
 * op-gated {@code /template-mod-admin gui} command, since that command's
 * executor runs with a {@code ServerCommandSource} that has no client
 * rendering context and cannot open a {@code Screen} directly.
 */
public record OpenAdminScreenPayload() implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<OpenAdminScreenPayload> ID =
        new CustomPacketPayload.Type<>(TemplateMod.id("open_admin_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenAdminScreenPayload> CODEC =
        StreamCodec.unit(new OpenAdminScreenPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }
}
