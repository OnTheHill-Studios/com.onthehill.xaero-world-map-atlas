package com.onthehill.xaeroworldmapbook.network;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAdvancementGate;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client payload carrying the admin-tab {@code MapAccessConfig}
 * state for the receiving player specifically. Sent on join and after any
 * successful admin config change.
 *
 * <p>Per the studio config standard's mandatory read-only-view setting, the
 * server decides per-recipient whether real values are included at all —
 * {@link #permitted()} being {@code false} means every other field is a
 * meaningless placeholder, not a real value hidden only by client-side
 * rendering. {@link com.onthehill.xaeroworldmapbook.config.MapAccessConfig}
 * is this mod's only remaining server-authoritative config object, so
 * {@link #allowNonOpReadOnlyView()} here carries that same one shared,
 * mod-wide setting {@link #permitted()} was itself computed from.
 *
 * @param operator Whether the receiving player currently has operator permission.
 * @param permitted Whether real values are present in this payload at all.
 * @param keybindOpenEnabled Real keybind-enabled setting, or {@code false} if not permitted.
 * @param keybindItemRequirement Real keybind item-location rule, or {@link KeybindItemRequirement#HOTBAR} if not permitted.
 * @param minimapAccessRequirement Real Minimap holding rule, or {@link MinimapAccessRequirement#MAIN_OR_OFFHAND} if not permitted.
 * @param chunksRequiredForWellTraveled Real chunk-travel milestone, or {@code 0} if not permitted.
 * @param minimapAdvancementGate Real advancement source for {@code ADVANCEMENT_ONLY} mode, or the default if not permitted.
 * @param creativeBypassEnabled Real Creative-mode bypass setting, or {@code false} if not permitted.
 * @param allowNonOpReadOnlyView Real read-only-view setting, or {@code false} if not permitted.
 */
public record MapAccessConfigSyncPayload(
    boolean operator,
    boolean permitted,
    boolean keybindOpenEnabled,
    KeybindItemRequirement keybindItemRequirement,
    MinimapAccessRequirement minimapAccessRequirement,
    int chunksRequiredForWellTraveled,
    MinimapAdvancementGate minimapAdvancementGate,
    boolean creativeBypassEnabled,
    boolean allowNonOpReadOnlyView
) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<MapAccessConfigSyncPayload> ID =
        new CustomPacketPayload.Type<>(XaeroWorldMapBook.id("map_access_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, MapAccessConfigSyncPayload> CODEC = StreamCodec.of(
        (buf, payload) ->
        {
            buf.writeBoolean(payload.operator());
            buf.writeBoolean(payload.permitted());
            buf.writeBoolean(payload.keybindOpenEnabled());
            buf.writeEnum(payload.keybindItemRequirement());
            buf.writeEnum(payload.minimapAccessRequirement());
            buf.writeVarInt(payload.chunksRequiredForWellTraveled());
            buf.writeEnum(payload.minimapAdvancementGate());
            buf.writeBoolean(payload.creativeBypassEnabled());
            buf.writeBoolean(payload.allowNonOpReadOnlyView());
        },
        buf -> new MapAccessConfigSyncPayload(
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readEnum(KeybindItemRequirement.class),
            buf.readEnum(MinimapAccessRequirement.class),
            buf.readVarInt(),
            buf.readEnum(MinimapAdvancementGate.class),
            buf.readBoolean(),
            buf.readBoolean()
        )
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }
}
