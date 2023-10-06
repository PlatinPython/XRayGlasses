package platinpython.xrayglasses;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = XRayGlasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class XRayRenderer {
    private static final HashSet<BlockPos> RENDER_POS_SET = new HashSet<>();
    private static FakeLevel FAKE_LEVEL = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isServer()) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            return;
        }

        RENDER_POS_SET.clear();
        Vec3 eyePos = player.getEyePosition();
        BlockPos eyeBlockPos = new BlockPos((int) eyePos.x, (int) eyePos.y, (int) eyePos.z);
        final int size = 5;
        BlockPos.betweenClosedStream(eyeBlockPos.offset(-size, -size, -size), eyeBlockPos.offset(size, size, size))
                .filter(blockPos -> level.getBlockState(blockPos).is(Tags.Blocks.ORES))
                .map(BlockPos::immutable)
                .forEach(RENDER_POS_SET::add);
        FAKE_LEVEL = new FakeLevel(level, RENDER_POS_SET);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        if (FAKE_LEVEL == null) {
            return;
        }
        RENDER_POS_SET.forEach(pos -> GhostBlockRenderer.doRenderGhostBlock(poseStack, buffer, FAKE_LEVEL, pos,
                                                                            FAKE_LEVEL.getBlockState(pos)
        ));
    }
}
