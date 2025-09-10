package uk.cloudmc.swrc.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.track.TrackBuilder;
import uk.cloudmc.swrc.track.Checkpoint;
import uk.cloudmc.swrc.track.Trap;

import java.util.ArrayList;
import java.util.OptionalDouble;

public class TrackBuilderRenderer implements WorldRenderEvents.Last {

    private static final RenderLayer renderLayer = RenderLayer.of(
            "debug_lines",
            1536,
            RenderUtils.DEBUG_LINES,
            RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1))).build(false)
    );

    public void renderCheckpoint(MatrixStack matrixStack, Camera camera, Vec3d left, Vec3d right, int color) {
        Tessellator tessellator = Tessellator.getInstance();

        float prevLineWidth = RenderSystem.getShaderLineWidth();

        RenderSystem.lineWidth(3f);

        Vector3f lrDelta = right.subtract(left).toVector3f();
        Vector3f checkpointDirection = new Vector3f(lrDelta.z, lrDelta.y, -lrDelta.x).normalize();

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        Vector3f fLeft = left.subtract(camera.getPos()).toVector3f();
        Vector3f fRight = right.subtract(camera.getPos()).toVector3f();

        // Render line from left to right
        BufferBuilder checkpointLine = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        checkpointLine.vertex(matrix, fLeft.x, fLeft.y, fLeft.z).color(color & 0xFFFFFFFF);
        checkpointLine.vertex(matrix, fRight.x, fRight.y, fRight.z).color(color & 0xFFFFFFFF);

        renderLayer.draw(checkpointLine.end());

        // Render line on left checkpoint showing detection height
        BufferBuilder leftHeightLine = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        leftHeightLine.vertex(matrix, fLeft.x, fLeft.y - 2, fLeft.z).color(color & 0xFFFFFF00);
        leftHeightLine.vertex(matrix, fLeft.x, fLeft.y + 2, fLeft.z).color(color & 0xFFFFFFFF);

        renderLayer.draw(leftHeightLine.end());

        // Render line on right checkpoint showing detection height
        BufferBuilder rightHeightLine = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        rightHeightLine.vertex(matrix, fRight.x, fRight.y - 2, fRight.z).color(color & 0xFFFFFF00);
        rightHeightLine.vertex(matrix, fRight.x, fRight.y + 2, fRight.z).color(color & 0xFFFFFFFF);

        renderLayer.draw(rightHeightLine.end());

        // Render line on left checkpoint showing direction
        BufferBuilder leftDirectionLine = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        leftDirectionLine.vertex(matrix, fLeft.x, fLeft.y, fLeft.z).color(color & 0xFFFFFFFF);
        leftDirectionLine.vertex(matrix, fLeft.x + checkpointDirection.x, fLeft.y + checkpointDirection.y, fLeft.z + checkpointDirection.z).color(color & 0xFFFFFFFF);

        renderLayer.draw(leftDirectionLine.end());

        // Render line on right checkpoint showing direction
        BufferBuilder rightDirectionLine = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        rightDirectionLine.vertex(matrix, fRight.x, fRight.y, fRight.z).color(color & 0xFFFFFFFF);
        rightDirectionLine.vertex(matrix, fRight.x + checkpointDirection.x, fRight.y + checkpointDirection.y, fRight.z + checkpointDirection.z).color(color & 0xFFFFFFFF);

        renderLayer.draw(rightDirectionLine.end());

        RenderSystem.lineWidth(prevLineWidth);
    }

    public void renderPole(MatrixStack matrixStack, Camera camera, Vec3d pos, int color) {
        Tessellator tessellator = Tessellator.getInstance();

        //RenderSystem.disableScissor();


        float prevLineWidth = RenderSystem.getShaderLineWidth();

        RenderSystem.lineWidth(3f);

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        Vector3f fPos = pos.subtract(camera.getPos()).toVector3f();

        // Render line from left to right
        BufferBuilder line = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        line.vertex(matrix, fPos.x, fPos.y - 2, fPos.z).color(color & 0xFFFFFF00);
        line.vertex(matrix, fPos.x, fPos.y + 2, fPos.z).color(color & 0xFFFFFFFF);

        renderLayer.draw(line.end());

        RenderSystem.lineWidth(prevLineWidth);
    }

    public void renderLine(MatrixStack matrixStack, Camera camera, Vec3d pos1, Vec3d pos2, int color) {
        Tessellator tessellator = Tessellator.getInstance();

        //RenderSystem.disableScissor();

        RenderSystem.lineWidth(3f);

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        Vector3f fPos1 = pos1.subtract(camera.getPos()).toVector3f();
        Vector3f fPos2 = pos2.subtract(camera.getPos()).toVector3f();

        // Render line from left to right
        BufferBuilder line = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        line.vertex(matrix, fPos1.x, fPos1.y, fPos1.z).color(color & 0xFFFFFF00);
        line.vertex(matrix, fPos2.x, fPos2.y, fPos2.z).color(color & 0xFFFFFFFF);

        renderLayer.draw(line.end());
    }

    @Override
    public void onLast(WorldRenderContext context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        MatrixStack matrixStack = context.matrixStack();
        Camera camera = context.camera();

        assert matrixStack != null;

        if (trackBuilder != null) {
            ArrayList<Checkpoint> checkpoints = trackBuilder.getCheckpoints();
            ArrayList<Trap> traps = trackBuilder.getTraps();

            for (Checkpoint checkpoint : checkpoints) {
                renderCheckpoint(matrixStack, camera, checkpoint.getLeft(), checkpoint.getRight(), 0xFFFFFFFF);
            }

            if (trackBuilder.checkpointBuilder.hasActiveCheckpoint()) {
                Checkpoint activeCheckpoint = trackBuilder.checkpointBuilder.getActiveCheckpoint();

                if (activeCheckpoint.isValid()) {
                    renderCheckpoint(matrixStack, camera, activeCheckpoint.getLeft(), activeCheckpoint.getRight(), 0xFF55FF55);
                } else {
                    if (activeCheckpoint.getLeft() != null) {
                        renderPole(matrixStack, camera, activeCheckpoint.getLeft(), 0xFF55FF55);
                    }
                    if (activeCheckpoint.getRight() != null) {
                        renderPole(matrixStack, camera, activeCheckpoint.getRight(), 0xFF55FF55);
                    }
                }
            }

            for (Trap trap : traps) {
                renderCheckpoint(matrixStack, camera, trap.enter.getLeft(), trap.enter.getRight(), 0xFFFF5555);
                renderCheckpoint(matrixStack, camera, trap.exit.getLeft(), trap.exit.getRight(), 0xFFFF5555);
                renderLine(matrixStack, camera, trap.enter.getCenter(), trap.exit.getCenter(), 0xFFFF5555);
            }

            if (trackBuilder.trapBuilder.hasActiveTrap()) {
                Trap activeTrap = trackBuilder.trapBuilder.getActiveTrap();

                if (activeTrap.enter != null) {
                    renderCheckpoint(matrixStack, camera, activeTrap.enter.getLeft(), activeTrap.enter.getRight(), 0xFFFF5555);
                }
                if (activeTrap.exit != null) {
                    renderCheckpoint(matrixStack, camera, activeTrap.exit.getLeft(), activeTrap.exit.getRight(), 0xFFFF5555);
                }

                if (activeTrap.isValid()) {
                    renderLine(matrixStack, camera, activeTrap.enter.getCenter(), activeTrap.exit.getCenter(), 0xFFFF5555);
                }
            }

            if (trackBuilder.hasPit()) {
                Checkpoint pit = trackBuilder.getPit();

                renderCheckpoint(matrixStack, camera, pit.getLeft(), pit.getRight(), 0xFF5555FF);
            }

            if (trackBuilder.hasPitEnter()) {
                Checkpoint pit_enter = trackBuilder.getPitEnter();

                renderCheckpoint(matrixStack, camera, pit_enter.getLeft(), pit_enter.getRight(), 0xFF55FFFF);
            }
        }
    }
}
