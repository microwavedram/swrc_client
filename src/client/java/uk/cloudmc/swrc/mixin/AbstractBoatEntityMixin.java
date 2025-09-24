package uk.cloudmc.swrc.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.WebsocketManager;

import java.util.function.Supplier;

@Mixin(AbstractBoatEntity.class)
public class AbstractBoatEntityMixin {

    @Unique private int defaultInterpolation;

    @Shadow @Final private PositionInterpolator interpolator;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookConstructor(EntityType type, World world, Supplier itemSupplier, CallbackInfo ci) {
        defaultInterpolation = ((PositionInterpolatorAccessor) interpolator).getLerpDuration();
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void hookPositionInterpolator(CallbackInfo ci) {
        if (WebsocketManager.racerSocketAvalible() && SWRCConfig.getInstance().interpolation_compat) {
            interpolator.setLerpDuration(10);
            return;
        }

        interpolator.setLerpDuration(defaultInterpolation);
    }
}
