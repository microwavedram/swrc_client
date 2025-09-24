package uk.cloudmc.swrc.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.net.packets.S2CUpdatePacket;
import uk.cloudmc.swrc.util.ColorUtil;
import uk.cloudmc.swrc.util.DeltaFormat;

import static net.minecraft.util.math.MathHelper.clamp;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRenderMixin<T extends LivingEntity, S extends LivingEntityRenderState> {

    @Shadow
    protected abstract boolean hasLabel(T entity, double distanceSquared);

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL")
    )
    @SuppressWarnings("ConstantValue")
    private void renderLapTimeLabel(S renderState, MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        if (!(renderState instanceof PlayerEntityRenderState playerState)) return;
        if (!(((Object) this) instanceof PlayerEntityRenderer renderer)) return;
        if (!SWRCConfig.getInstance().render_lap_times_above_heads) return;
        if (SWRC.minecraftClient.world == null || SWRC.getRace() == null) return;

        Race race = SWRC.getRace();

        AbstractClientPlayerEntity player = SWRC.minecraftClient.world.getPlayers().stream()
                .filter(p -> p.getName().getLiteralString().equals(playerState.name))
                .findFirst()
                .orElse(null);

        if (player == null) return;

        S2CUpdatePacket.RaceLeaderboardPosition leaderboardPos = race.raceLeaderboardPositions.stream()
                .filter(pos -> pos.player_name.equals(player.getName().getString()))
                .findFirst()
                .orElse(null);

        if (leaderboardPos == null) return;

        matrices.push();
        matrices.translate(0, playerState.height + 0.5f, 0);

        boolean showLabel = hasLabel((T) player, playerState.squaredDistanceToCamera);
        boolean isClose = playerState.squaredDistanceToCamera < 100.0;
        boolean hasBelowNameObjective = player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null;

        if (showLabel && playerState.squaredDistanceToCamera <= 4096.0) {
            matrices.translate(0.0, 9.0f * 1.15f * 0.025f, 0.0);
            if (isClose && hasBelowNameObjective) {
                matrices.translate(0.0, 9.0f * 1.15f * 0.025f, 0.0);
            }
        }

        matrices.multiply(((EntityRendererAccessor) renderer).getDispatcher().getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.PI));
        matrices.scale(-1.0f, 1.0f, 1.0f);
        matrices.scale(0.025f, 0.025f, 0.025f);

        String playerName = player.getName().getString();
        long lapTime = System.currentTimeMillis() - race.getLapBeginTime(playerName);

        Text text = Text.empty()
                .append(Text.literal("P" + (race.raceLeaderboardPositions.indexOf(leaderboardPos) + 1) + " ")
                        .styled(style -> style.withFormatting(Formatting.AQUA, Formatting.BOLD)))
                .append(Text.literal(DeltaFormat.formatMillis(lapTime)));

        if (race.laps.getOrDefault(leaderboardPos.player_name, 0) > race.getTotalLaps()) {
            text = Text.empty()
                    .append(Text.literal("P" + (race.raceLeaderboardPositions.indexOf(leaderboardPos) + 1) + " ")
                            .styled(style -> style.withFormatting(Formatting.AQUA, Formatting.BOLD)))
                    .append(Text.literal("FINISHED").withColor(0xAAAAAA));
        }

        if (race.getRaceState() == Race.RaceState.QUALI) {
            text = Text.empty()
                    .append(Text.literal("P" + (race.raceLeaderboardPositions.indexOf(leaderboardPos) + 1) + " ")
                            .styled(style -> style.withFormatting(Formatting.AQUA, Formatting.BOLD)))
                    .append(Text.literal(DeltaFormat.formatDelta(leaderboardPos.lap_delta)).withColor(
                            ColorUtil.lerpHue(
                                    0x6af57d, // green
                                    0xf56a6a, // red
                                    clamp((float) Math.tanh(leaderboardPos.lap_delta / 2000f) / 2f + .5f, 0, 1)                            )
                    ));
        }

        TextRenderer textRenderer = SWRC.minecraftClient.textRenderer;

        textRenderer.draw(
                text,
                -textRenderer.getWidth(text.getString()) / 2.0f,
                0.0f,
                0xffffffff,
                true,
                matrices.peek().getPositionMatrix(),
                vertices,
                TextRenderer.TextLayerType.NORMAL,
                0x77777777,
                0xffffff
        );

        matrices.pop();
    }
}