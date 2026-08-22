package com.onthehill.xaeroworldmapbook.network;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;
import com.onthehill.xaeroworldmapbook.config.KeybindItemRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAccessRequirement;
import com.onthehill.xaeroworldmapbook.config.MinimapAdvancementGate;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-to-server payload sent by the consolidated admin GUI tab's Save
 * button for every {@code MapAccessConfig} field, including the mandatory
 * "allow non-op read-only view" setting — {@link com.onthehill.xaeroworldmapbook.config.MapAccessConfig}
 * is this mod's only remaining server-authoritative config object, so this
 * is that setting's one update payload. The server receiver re-validates
 * both the sender's operator permission and the proposed values exactly as
 * if this had arrived from an untrusted source, because it did.
 *
 * @param keybindOpenEnabled Proposed new keybind-enabled setting.
 * @param keybindItemRequirement Proposed new keybind item-location rule.
 * @param minimapAccessRequirement Proposed new Minimap holding rule.
 * @param chunksRequiredForWellTraveled Proposed new chunk-travel milestone.
 * @param minimapAdvancementGate Proposed new advancement source for {@code ADVANCEMENT_ONLY} mode.
 * @param creativeBypassEnabled Proposed new Creative-mode bypass setting.
 * @param allowNonOpReadOnlyView Proposed new read-only-view setting.
 */
public record MapAccessConfigUpdatePayload(
    boolean keybindOpenEnabled,
    KeybindItemRequirement keybindItemRequirement,
    MinimapAccessRequirement minimapAccessRequirement,
    int chunksRequiredForWellTraveled,
    MinimapAdvancementGate minimapAdvancementGate,
    boolean creativeBypassEnabled,
    boolean allowNonOpReadOnlyView
) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<MapAccessConfigUpdatePayload> ID =
        new CustomPacketPayload.Type<>(XaeroWorldMapBook.id("map_access_config_update"));

    public static final StreamCodec<FriendlyByteBuf, MapAccessConfigUpdatePayload> CODEC = StreamCodec.of(
        (buf, payload) ->
        {
            buf.writeBoolean(payload.keybindOpenEnabled());
            buf.writeEnum(payload.keybindItemRequirement());
            buf.writeEnum(payload.minimapAccessRequirement());
            buf.writeVarInt(payload.chunksRequiredForWellTraveled());
            buf.writeEnum(payload.minimapAdvancementGate());
            buf.writeBoolean(payload.creativeBypassEnabled());
            buf.writeBoolean(payload.allowNonOpReadOnlyView());
        },
        buf -> new MapAccessConfigUpdatePayload(
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
