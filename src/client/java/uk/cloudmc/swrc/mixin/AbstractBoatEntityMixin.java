package uk.cloudmc.swrc.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.PositionInterpolator;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.cloudmc.swrc.SWRC;

import java.util.function.Supplier;

@Mixin(AbstractBoatEntity.class)
public abstract class AbstractBoatEntityMixin {

    @Shadow @Final private PositionInterpolator interpolator;

    @Unique private int swrc$defaultInterpolation;


    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookConstructor(EntityType type, World world, Supplier itemSupplier, CallbackInfo ci) {
        swrc$defaultInterpolation = ((PositionInterpolatorAccessor) interpolator).getLerpDuration();
    }

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void hookPositionInterpolator(CallbackInfo ci) {
        if (SWRC.getRace() != null) {
            interpolator.setLerpDuration(10);
            return;
        }

        interpolator.setLerpDuration(swrc$defaultInterpolation);
    }
}