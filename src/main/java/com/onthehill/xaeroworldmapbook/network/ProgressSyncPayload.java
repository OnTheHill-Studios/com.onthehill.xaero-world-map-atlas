package com.onthehill.xaeroworldmapbook.network;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client payload carrying the current example progress value and
 * its rate. Sent on player join and on a periodic resync heartbeat rather
 * than every single tick — the client extrapolates the value smoothly
 * between syncs using {@code ProgressMath.extrapolate}, per the general mod
 * rule against unnecessary per-tick network chatter.
 *
 * @param progress Current progress value at the moment this payload was sent, in [0, 1).
 * @param ratePerTick Current server-configured per-tick rate, in (0, 1).
 */
public record ProgressSyncPayload(float progress, float ratePerTick) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ProgressSyncPayload> ID =
        new CustomPacketPayload.Type<>(XaeroWorldMapBook.id("progress_sync"));

    public static final StreamCodec<FriendlyByteBuf, ProgressSyncPayload> CODEC = StreamCodec.of(
        (buf, payload) ->
        {
            buf.writeFloat(payload.progress());
            buf.writeFloat(payload.ratePerTick());
        },
        buf -> new ProgressSyncPayload(buf.readFloat(), buf.readFloat())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }
}
