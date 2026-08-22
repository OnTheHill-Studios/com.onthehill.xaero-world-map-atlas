package com.onthehill.xaeroworldmapbook.network;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-to-server payload sent by the admin GUI tab's Save button. The
 * server receiver re-validates both the sender's operator permission and the
 * proposed values exactly as if this had arrived from an untrusted source,
 * because it did — the client-side disabled/greyed-out controls a non-op
 * player sees are a UI convenience, not a security boundary.
 *
 * @param progressRate Proposed new progress rate.
 * @param allowNonOpReadOnlyView Proposed new read-only-view setting.
 */
public record AdminConfigUpdatePayload(
    float progressRate,
    boolean allowNonOpReadOnlyView
) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AdminConfigUpdatePayload> ID =
        new CustomPacketPayload.Type<>(XaeroWorldMapBook.id("admin_config_update"));

    public static final StreamCodec<FriendlyByteBuf, AdminConfigUpdatePayload> CODEC = StreamCodec.of(
        (buf, payload) ->
        {
            buf.writeFloat(payload.progressRate());
            buf.writeBoolean(payload.allowNonOpReadOnlyView());
        },
        buf -> new AdminConfigUpdatePayload(buf.readFloat(), buf.readBoolean())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }
}
