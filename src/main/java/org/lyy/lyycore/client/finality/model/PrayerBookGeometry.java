package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class PrayerBookGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition prayer_book = partdefinition.addOrReplaceChild("prayer_book", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F, -0.6109F, 0.0F, -0.3142F));

		PartDefinition spine = prayer_book.addOrReplaceChild("spine", CubeListBuilder.create().texOffs(1, 1).addBox(-0.58F, 0.2F, -7.6F, 1.16F, 1.0F, 15.2F, new CubeDeformation(0.0F))
		.texOffs(36, 1).addBox(-0.3F, -0.73F, -7.15F, 0.6F, 0.93F, 14.3F, new CubeDeformation(0.0F))
		.texOffs(68, 1).addBox(-0.65F, 0.17F, -7.58F, 1.3F, 1.1F, 0.46F, new CubeDeformation(0.0F))
		.texOffs(74, 1).addBox(-0.65F, 0.17F, -5.03F, 1.3F, 1.1F, 0.46F, new CubeDeformation(0.0F))
		.texOffs(80, 1).addBox(-0.65F, 0.17F, 4.57F, 1.3F, 1.1F, 0.46F, new CubeDeformation(0.0F))
		.texOffs(86, 1).addBox(-0.65F, 0.17F, 7.12F, 1.3F, 1.1F, 0.46F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition left_leaf = prayer_book.addOrReplaceChild("left_leaf", CubeListBuilder.create().texOffs(1, 20).addBox(0.2F, 0.3F, -7.65F, 9.8F, 0.65F, 15.3F, new CubeDeformation(0.0F))
		.texOffs(54, 20).addBox(0.38F, -0.9F, -7.13F, 9.17F, 1.19F, 14.26F, new CubeDeformation(0.0F))
		.texOffs(1, 38).addBox(9.55F, 0.24F, -7.4F, 0.27F, 0.66F, 14.8F, new CubeDeformation(0.0F))
		.texOffs(34, 38).addBox(8.65F, 0.0F, -7.7F, 1.4F, 0.29F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(42, 38).addBox(9.78F, 0.28F, -7.7F, 0.3F, 0.72F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(48, 38).addBox(8.65F, 0.28F, -7.72F, 1.13F, 0.72F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(53, 38).addBox(8.65F, 0.0F, 6.45F, 1.4F, 0.29F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(61, 38).addBox(9.78F, 0.28F, 6.45F, 0.3F, 0.72F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(67, 38).addBox(8.65F, 0.28F, 7.42F, 1.13F, 0.72F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

		PartDefinition right_leaf = prayer_book.addOrReplaceChild("right_leaf", CubeListBuilder.create().texOffs(72, 38).addBox(-10.0F, 0.3F, -7.65F, 9.8F, 0.65F, 15.3F, new CubeDeformation(0.0F))
		.texOffs(1, 56).addBox(-9.55F, -0.9F, -7.13F, 9.17F, 1.19F, 14.26F, new CubeDeformation(0.0F))
		.texOffs(50, 56).addBox(-9.82F, 0.24F, -7.4F, 0.27F, 0.66F, 14.8F, new CubeDeformation(0.0F))
		.texOffs(83, 56).addBox(-10.05F, 0.0F, -7.7F, 1.4F, 0.29F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(91, 56).addBox(-10.08F, 0.28F, -7.7F, 0.3F, 0.72F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(97, 56).addBox(-9.78F, 0.28F, -7.72F, 1.13F, 0.72F, 0.3F, new CubeDeformation(0.0F))
		.texOffs(102, 56).addBox(-10.05F, 0.0F, 6.45F, 1.4F, 0.29F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(110, 56).addBox(-10.08F, 0.28F, 6.45F, 0.3F, 0.72F, 1.25F, new CubeDeformation(0.0F))
		.texOffs(116, 56).addBox(-9.78F, 0.28F, 7.42F, 1.13F, 0.72F, 0.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

		PartDefinition page_turn = prayer_book.addOrReplaceChild("page_turn", CubeListBuilder.create().texOffs(1, 132).addBox(-3.0F, -0.01F, -7.05F, 3.0F, 0.01F, 14.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.1F, 0.0F, 0.0F, 0.0F, 0.3927F));

		PartDefinition page_middle = page_turn.addOrReplaceChild("page_middle", CubeListBuilder.create().texOffs(37, 132).addBox(-3.0F, -0.01F, -7.05F, 3.0F, 0.01F, 14.1F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, 0.0F, 0.0F));

		PartDefinition page_edge = page_middle.addOrReplaceChild("page_edge", CubeListBuilder.create().texOffs(73, 132).addBox(-3.0F, -0.01F, -7.05F, 3.0F, 0.01F, 14.1F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, 0.0F, 0.0F));

		PartDefinition halo_tier_1 = partdefinition.addOrReplaceChild("halo_tier_1", CubeListBuilder.create().texOffs(1, 176).addBox(-22.0F, -22.0F, -0.03F, 44.0F, 44.0F, 0.06F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 13.0F, -11.0F));

		return LayerDefinition.create(meshdefinition, 128, 256);
	}
}
