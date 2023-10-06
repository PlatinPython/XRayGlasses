package platinpython.xrayglasses;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;

public class GhostBlockRenderer extends RenderStateShard {
    private static final RandomSource RANDOM = RandomSource.create();
    @SuppressWarnings("deprecation")
    private static final RenderType BLOCK_TRANSLUCENT = RenderType.create(
            new ResourceLocation(XRayGlasses.MOD_ID, "block_translucent").toString(), DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                                                                                .setShaderState(
                                                                                        RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                                                                                .setTextureState(
                                                                                        new RenderStateShard.TextureStateShard(
                                                                                                TextureAtlas.LOCATION_BLOCKS,
                                                                                                false, false
                                                                                        ))
                                                                                .setTransparencyState(
                                                                                        TRANSLUCENT_TRANSPARENCY)
                                                                                .setLightmapState(LIGHTMAP)
                                                                                .setOverlayState(OVERLAY)
                                                                                .setDepthTestState(NO_DEPTH_TEST)
                                                                                .setOutputState(TRANSLUCENT_TARGET)
                                                                                .createCompositeState(true)
    );

    public GhostBlockRenderer(String name, Runnable setupState, Runnable clearState) {
        super(name, setupState, clearState);
    }

    public static void doRenderGhostBlock(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffers,
            BlockAndTintGetter level,
            BlockPos renderPos,
            BlockState renderState
    ) {
        Vec3 offset = Vec3.atLowerCornerOf(renderPos)
                          .subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        VertexConsumer builder = new GhostVertexConsumer(buffers.getBuffer(BLOCK_TRANSLUCENT), 0xAA);

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(renderState);
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        for (RenderType type : model.getRenderTypes(renderState, RANDOM, ModelData.EMPTY)) {
            doRenderGhostBlockInLayer(poseStack, builder, level, renderPos, renderState, type);
        }
        poseStack.popPose();

        RenderSystem.enableCull();
        buffers.endBatch(BLOCK_TRANSLUCENT);
    }

    private static void doRenderGhostBlockInLayer(
            PoseStack poseStack,
            VertexConsumer builder,
            BlockAndTintGetter level,
            BlockPos renderPos,
            BlockState renderState,
            RenderType layer
    ) {
        Minecraft.getInstance()
                 .getBlockRenderer()
                 .renderBatched(renderState, renderPos, level, poseStack, builder, true, RANDOM, ModelData.EMPTY,
                                layer
                 );
    }

    private static final class GhostVertexConsumer extends VertexConsumerWrapper {
        private final int alpha;

        public GhostVertexConsumer(VertexConsumer wrapped, int alpha) {
            super(wrapped);
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return parent.color(red, green, blue, (alpha * this.alpha) / 0xFF);
        }
    }
}
