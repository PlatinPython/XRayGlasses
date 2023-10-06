package platinpython.xrayglasses;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.HashSet;

@Mod(XRayGlasses.MODID)
public class XRayGlasses {
    public static final String MODID = "xrayglasses";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientEvents {
        private static final HashSet<BlockPos> RENDER_POS_SET = new HashSet<>();

        @SubscribeEvent
        public static void onClientTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.side.isServer()) {
                return;
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            RENDER_POS_SET.clear();
            Vec3 eyePos = player.getEyePosition();
            BlockPos eyeBlockPos = new BlockPos((int) eyePos.x, (int) eyePos.y, (int) eyePos.z);
            BlockPos.betweenClosedStream(eyeBlockPos.offset(-5, -5, -5), eyeBlockPos.offset(5, 5, 5))
                    .filter(blockPos -> event.level.getBlockState(blockPos).is(Tags.Blocks.ORES))
                    .map(BlockPos::immutable)
                    .forEach(RENDER_POS_SET::add);
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
                return;
            }

            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

            poseStack.pushPose();
            Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            poseStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

            Matrix4f matrix = poseStack.last().pose();

            VertexConsumer builder = buffer.getBuffer(BoxRenderType.TRANSLUCENT_NO_CULL);
            RENDER_POS_SET.forEach(blockPos -> {
                Vector3f pos = Vec3.atCenterOf(blockPos).toVector3f();
                renderBoxSides(builder, matrix, pos.x() - 0.5f, pos.y() - 0.5f, pos.z() - 0.5f, pos.x() + 0.5f,
                               pos.y() + 0.5f, pos.z() + 0.5f
                );
            });
            buffer.endBatch(BoxRenderType.TRANSLUCENT_NO_CULL);

            poseStack.popPose();
        }

        private static void renderBoxSides(
                VertexConsumer builder,
                Matrix4f matrix,
                float minX,
                float minY,
                float minZ,
                float maxX,
                float maxY,
                float maxZ
        ) {
            int red = 0;
            int green = 128;
            int blue = 128;
            int alpha = 128;

            // Top side
            builder.vertex(matrix, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, minY, minZ).color(red, green, blue, alpha).endVertex();

            // Bottom side
            builder.vertex(matrix, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            // North side
            builder.vertex(matrix, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

            // East side
            builder.vertex(matrix, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();

            // South side
            builder.vertex(matrix, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

            // West side
            builder.vertex(matrix, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        }

        private static class BoxRenderType extends RenderType {

            public BoxRenderType(
                    String name,
                    VertexFormat format,
                    VertexFormat.Mode mode,
                    int bufferSize,
                    boolean affectsCrumbling,
                    boolean sortOnUpload,
                    Runnable setupState,
                    Runnable clearState
            ) {
                super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
            }

            public static final RenderType TRANSLUCENT_NO_CULL = RenderType.create(
                    new ResourceLocation(MODID, "translucent_no_cull").toString(), DefaultVertexFormat.POSITION_COLOR,
                    VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                                                                                        .setOutputState(
                                                                                                RenderStateShard.TRANSLUCENT_TARGET)
                                                                                        .setShaderState(
                                                                                                RenderStateShard.POSITION_COLOR_SHADER)
                                                                                        .setLayeringState(
                                                                                                LayeringStateShard.VIEW_OFFSET_Z_LAYERING)
                                                                                        .setTransparencyState(
                                                                                                RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                                                                                        .setTextureState(
                                                                                                RenderStateShard.NO_TEXTURE)
                                                                                        .setDepthTestState(
                                                                                                RenderStateShard.NO_DEPTH_TEST)
                                                                                        .setCullState(
                                                                                                RenderStateShard.NO_CULL)
                                                                                        .setLightmapState(
                                                                                                RenderStateShard.NO_LIGHTMAP)
                                                                                        .setWriteMaskState(
                                                                                                RenderStateShard.COLOR_DEPTH_WRITE)
                                                                                        .createCompositeState(false)
            );
        }
    }
}