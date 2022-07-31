package net.earthcomputer.pipeless;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class WalkingItemEntityRenderer extends ItemEntityRenderer {
    private static final float ITEM_Y_OFFSET = 0.025f; // trust me, it makes all the difference
    private static final float FACE_Y_OFFSET = 0.3f;
    private static final float FACE_Z_OFFSET = 0.1f;
    private static final float FACE_RADIUS = 0.25f;
    private static final float LEGS_X_OFFSET = FACE_RADIUS * 0.3f;
    private static final float LEGS_Y_OFFSET = 0.15f;
    private static final float LEGS_X_RADIUS = 0.05f;
    private static final float LEGS_HEIGHT = 0.25f;

    private static final int FACE_ANIM_FRAMES = 14;
    private static final int NUM_TILES_X = 4;
    private static final int NUM_TILES_Y = 2;
    private static final int LEGS_TILE_X = 3;
    private static final int LEGS_TILE_Y = 1;
    private static final float LEFT_LEG_TILE_X_FRACTION = 0;
    private static final float RIGHT_LEG_TILE_X_FRACTION = 0.75f;
    private static final float LEGS_TILE_WIDTH_FRACTION = 0.25f;

    private static final ResourceLocation HAPPY_ITEM_TEXTURE = new ResourceLocation(Pipeless.MODID, "textures/entity/happy_item.png");

    private final ItemRenderer itemRenderer;

    public WalkingItemEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
        this.itemRenderer = pContext.getItemRenderer();
    }

    @Override
    public void render(ItemEntity pEntity_, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        WalkingItemEntity pEntity = (WalkingItemEntity) pEntity_;
        if (!pEntity.isFadingOut()) {
            renderHappyItem(pEntity, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
        }

        pMatrixStack.pushPose();
        setItemBobbingTranslation(pEntity, pPartialTicks, pMatrixStack);
        super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
        pMatrixStack.popPose();
    }

    private void setItemBobbingTranslation(WalkingItemEntity pEntity, float pPartialTicks, PoseStack pMatrixStack) {
        // Forge's shouldBob method doesn't work, translate in the opposite direction to cancel the bobbing out
        // PR to fix (use Forge API once it's fixed): https://github.com/MinecraftForge/MinecraftForge/pull/8919
        // we also add our custom bob offset here
        float delta = pEntity.getFadeBlend(pPartialTicks);
        float bobOffsetToCancel = Mth.sin(((float) pEntity.getAge() + pPartialTicks) / 10.0F + pEntity.bobOffs) * 0.1F + 0.1F;
        float vanillaBobOffset = Mth.sin(((float) pEntity.getAge() + pPartialTicks) / 10.0F + pEntity.getTargetBobOffset()) * 0.1F + 0.1F;
        float actualOffset = ITEM_Y_OFFSET * delta + vanillaBobOffset * (1 - delta);
        pMatrixStack.translate(0.0D, actualOffset - bobOffsetToCancel, 0.0D);
    }

    private void renderHappyItem(WalkingItemEntity pEntity, float partialTicks, PoseStack pMatrix, MultiBufferSource pBuffer, int light) {
        ItemStack stack = pEntity.getItem();
        BakedModel model = this.itemRenderer.getModel(stack, pEntity.level, null, pEntity.getId());
        ItemTransform transform = model.getTransforms().getTransform(ItemTransforms.TransformType.GROUND);

        pMatrix.pushPose();
        float spin = pEntity.getSpin(partialTicks);
        pMatrix.mulPose(Vector3f.YP.rotation(spin));

        pMatrix.pushPose();
        pMatrix.translate(0, FACE_Y_OFFSET, 0);

        VertexConsumer buffer = pBuffer.getBuffer(RenderType.entityCutout(HAPPY_ITEM_TEXTURE));
        final float offsetZ = model.isGui3d() ? 0.5f * transform.scale.z() + FACE_Z_OFFSET : FACE_Z_OFFSET;

        // face animation
        int frame = Math.floorMod(pEntity.getAge(), FACE_ANIM_FRAMES);
        int halfFrame = frame % (FACE_ANIM_FRAMES / 2);
        float minU = (1.0f / NUM_TILES_X) * (halfFrame % NUM_TILES_X);
        float minV = (1.0f / NUM_TILES_Y) * (float) (halfFrame / NUM_TILES_X);
        float maxU = minU + 1.0f / NUM_TILES_X;
        float maxV = minV + 1.0f / NUM_TILES_Y;
        if (frame >= (FACE_ANIM_FRAMES / 2)) {
            float tmp = minU;
            minU = maxU;
            maxU = tmp;
        }

        vertex(buffer, pMatrix, -FACE_RADIUS, FACE_RADIUS, offsetZ, minU, minV, light);
        vertex(buffer, pMatrix, -FACE_RADIUS, -FACE_RADIUS, offsetZ, minU, maxV, light);
        vertex(buffer, pMatrix, FACE_RADIUS, -FACE_RADIUS, offsetZ, maxU, maxV, light);
        vertex(buffer, pMatrix, FACE_RADIUS, FACE_RADIUS, offsetZ, maxU, minV, light);

        pMatrix.popPose();

        // draw legs
        float limbSwingAmount = pEntity.getLimbSwingAmount(partialTicks);
        float limbSwingSpeed = pEntity.getLimbSwingSpeed(partialTicks);
        float leftLegRot = Mth.cos(limbSwingAmount * 0.6662F + (float)Math.PI) * 2.0F * limbSwingSpeed * 0.5F;
        float rightLegRot = Mth.cos(limbSwingAmount * 0.6662F) * 2.0F * limbSwingSpeed * 0.5F;

        renderLeg(pMatrix, buffer, light, leftLegRot, true);
        renderLeg(pMatrix, buffer, light, rightLegRot, false);

        pMatrix.popPose();
    }

    private static void renderLeg(PoseStack pMatrix, VertexConsumer buffer, int light, float legRot, boolean left) {
        pMatrix.pushPose();
        pMatrix.translate(left ? -LEGS_X_OFFSET : LEGS_X_OFFSET, LEGS_Y_OFFSET, 0);
        pMatrix.mulPose(Vector3f.XP.rotation(legRot));

        float minU = (LEGS_TILE_X + (left ? LEFT_LEG_TILE_X_FRACTION : RIGHT_LEG_TILE_X_FRACTION)) / NUM_TILES_X;
        float maxU = minU + LEGS_TILE_WIDTH_FRACTION / NUM_TILES_X;
        float minV = (float) LEGS_TILE_Y / NUM_TILES_Y;
        float maxV = minV + 1f / NUM_TILES_Y;

        vertex(buffer, pMatrix, -LEGS_X_RADIUS, 0, 0, minU, minV, light);
        vertex(buffer, pMatrix, -LEGS_X_RADIUS, -LEGS_HEIGHT, 0, minU, maxV, light);
        vertex(buffer, pMatrix, LEGS_X_RADIUS, -LEGS_HEIGHT, 0, maxU, maxV, light);
        vertex(buffer, pMatrix, LEGS_X_RADIUS, 0, 0, maxU, minV, light);

        pMatrix.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack pMatrix, float x, float y, float z, float u, float v, int light) {
        buffer.vertex(pMatrix.last().pose(), x, y, z)
            .color(1f, 1f, 1f, 1f)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(pMatrix.last().normal(), 0, 1, 0)
            .endVertex();
    }
}
