package com.onthehill.xaeroworldmapbook.network;

import com.onthehill.xaeroworldmapbook.XaeroWorldMapBook;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server-to-client payload carrying the receiving player's own
 * {@code craft_atlas}/{@code well_traveled}/vanilla-{@code adventuring_time}
 * advancement status.
 *
 * <p>This mod's gate checks need this boolean state on the client (for
 * {@code client.config.MapAccessClientState}'s per-tick gate evaluation),
 * but this Minecraft version's client-side advancement tracking
 * ({@code net.minecraft.client.multiplayer.ClientAdvancements}) exposes no
 * public accessor for a specific advancement's progress outside its own
 * listener-driven advancement-screen UI — confirmed via {@code javap -p}
 * against the real client jar, per the spec's own "confirm the exact
 * client-side accessor before relying on it" caution. Networking this
 * explicitly, rather than guessing at a client-side read, is the resolution.
 *
 * @param craftAtlasEarned Whether the receiving player currently holds {@code craft_atlas}.
 * @param wellTraveledEarned Whether the receiving player currently holds this mod's own {@code well_traveled}.
 * @param adventuringTimeEarned Whether the receiving player currently holds vanilla's own {@code minecraft:adventure/adventuring_time}
 *     — the alternate {@code MinimapAdvancementGate} option.
 */
public record MapAccessAdvancementStatusPayload(boolean craftAtlasEarned, boolean wellTraveledEarned, boolean adventuringTimeEarned)
    implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<MapAccessAdvancementStatusPayload> ID =
        new CustomPacketPayload.Type<>(XaeroWorldMapBook.id("map_access_advancement_status"));

    public static final StreamCodec<FriendlyByteBuf, MapAccessAdvancementStatusPayload> CODEC = StreamCodec.of(
        (buf, payload) ->
        {
            buf.writeBoolean(payload.craftAtlasEarned());
            buf.writeBoolean(payload.wellTraveledEarned());
            buf.writeBoolean(payload.adventuringTimeEarned());
        },
        buf -> new MapAccessAdvancementStatusPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }
}
