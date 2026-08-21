package com.onthehill.templatemod.network;

import com.onthehill.templatemod.TemplateMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client payload carrying the admin-tab config state for the
 * receiving player specifically. Sent on join and after any successful
 * admin config change.
 *
 * <p>Per the studio config standard's mandatory read-only-view setting, the
 * server decides per-recipient whether real values are included at all —
 * {@link #permitted()} being {@code false} means {@link #progressRate()} and
 * {@link #allowNonOpReadOnlyView()} are meaningless placeholder zeros, not
 * real values hidden only by client-side rendering. The server must never
 * send real values to a non-op client when the read-only setting is disabled.
 *
 * @param operator Whether the receiving player currently has operator permission.
 * @param permitted Whether real values are present in this payload at all.
 * @param progressRate Real server-configured progress rate, or {@code 0} if not permitted.
 * @param allowNonOpReadOnlyView Real read-only-view setting, or {@code false} if not permitted.
 */
public record AdminConfigSyncPayload(
    boolean operator,
    boolean permitted,
    float progressRate,
    boolean allowNonOpReadOnlyView
) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AdminConfigSyncPayload> ID =
        new CustomPacketPayload.Type<>(TemplateMod.id("admin_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, AdminConfigSyncPayload> CODEC = StreamCodec.of(
        (buf, payload) ->
        {
            buf.writeBoolean(payload.operator());
            buf.writeBoolean(payload.permitted());
            buf.writeFloat(payload.progressRate());
            buf.writeBoolean(payload.allowNonOpReadOnlyView());
        },
        buf -> new AdminConfigSyncPayload(
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readFloat(),
            buf.readBoolean()
        )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }
}
