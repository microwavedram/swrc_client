package uk.cloudmc.swrc.track;

import com.google.gson.annotations.Expose;
import net.minecraft.util.math.Vec3d;
import uk.cloudmc.swrc.util.NTPTimeSync;
import uk.cloudmc.swrc.util.Snapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class Checkpoint {

    @Expose
    private Vec3d left;
    @Expose
    private Vec3d right;

    private double lineLength;
    private Vec3d center;

    HashMap<String, Boolean> checkpoint_sides = new HashMap<>();
    HashMap<String, Long> cooldowns = new HashMap<>();

    private record SideResult(boolean line, boolean between) {
        @Override
            public String toString() {
                return "SideResult{" +
                        "line=" + line +
                        ", between=" + between +
                        '}';
            }
        }

    public Checkpoint() {}

    public void setCooldownExpire(String name, long timestamp) {
        cooldowns.put(name, timestamp);
    }

    public boolean isOnCooldown(String name) {
        return !(cooldowns.getOrDefault(name, 0L) < NTPTimeSync.getTrueTime());
    }

    public ArrayList<Snapshot> getLineCrosses(ArrayList<Snapshot> positionSnapshots) {
        ArrayList<Snapshot> line_crosses = new ArrayList<>();

        for (Snapshot positionSnapshot : positionSnapshots) {

            if (isOnCooldown(positionSnapshot.getPlayer())) continue;

            SideResult side = getSide(positionSnapshot.getPosition());
            if (!checkpoint_sides.containsKey(positionSnapshot.getPlayer())) {
                checkpoint_sides.put(positionSnapshot.getPlayer(), side.line);
            }

            boolean last_side = checkpoint_sides.get(positionSnapshot.getPlayer());

            if (side.line != last_side) {
                if (side.line && side.between) {
                    line_crosses.add(positionSnapshot);

                    setCooldownExpire(positionSnapshot.getPlayer(), NTPTimeSync.getTrueTime() + 10000);

                    checkpoint_sides.put(positionSnapshot.getPlayer(), true);
                    continue;
                }

                checkpoint_sides.put(positionSnapshot.getPlayer(), false);
            }
        }

        return line_crosses;
    }

    private SideResult getSide(Vec3d position) {
        if (left == null || right == null) {
            throw new RuntimeException("Left or Right not set on checkpoint");
        }

        double between_factor = Math.sqrt(
                Math.pow(left.getX() - position.getX(), 2)
                + Math.pow(left.getZ() - position.getZ(), 2)
        ) - Math.sqrt(
                Math.pow(right.getX() - position.getX(), 2)
                + Math.pow(right.getZ() - position.getZ(), 2)
        );

        double lowest = Math.min(left.y, right.y);
        double highest = Math.max(left.y, right.y);
        boolean within_height = position.y >= lowest - 2 && position.y <= highest + 2;

        boolean between = Math.abs(between_factor) < lineLength - 1 && position.isInRange(this.center, lineLength / 1.5) && within_height;
        boolean line;

        if (left.getX() > right.getX()) {
            line = (position.getZ()-left.getZ()) > safeDivide(left.getZ()-right.getZ(), left.getX()-right.getX()) * (position.getX()-left.getX());
        } else {
            line = (position.getZ()-left.getZ()) < safeDivide(left.getZ()-right.getZ(), left.getX()-right.getX()) * (position.getX()-left.getX());
        }

        return new SideResult(line, between);
    }

    public boolean isValid() {
        return left != null && right != null;
    }

    public Vec3d getCenter() {
        return left.add(right).multiply(0.5);
    }

    public Vec3d getLeft() {
        return left;
    }

    public void setLeft(Vec3d left) {
        this.left = left;
        recalculate();
    }

    public Vec3d getRight() {
        return right;
    }


    public void setRight(Vec3d right) {
        this.right = right;
        recalculate();
    }

    public void recalculate() {
        if (left == null || right == null) return;

        lineLength = Math.sqrt(
                Math.pow(left.getX() - right.getX(), 2)
                + Math.pow(left.getZ() - right.getZ(), 2)
        );
        center = getCenter();
    }

    private double safeDivide(double x, double y) {
        if (x == 0 || y == 0) {
            return 0;
        }

        return x / y;
    }
}
