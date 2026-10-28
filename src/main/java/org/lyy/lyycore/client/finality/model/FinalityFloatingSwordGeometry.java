package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class FinalityFloatingSwordGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition finality_floating_sword = partdefinition.addOrReplaceChild("finality_floating_sword", CubeListBuilder.create().texOffs(1, 1).addBox(-11.2F, -11.2F, -0.55F, 4.2F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-7.0F, -9.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 1).addBox(-9.8F, -9.8F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-11.2F, -9.8F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-5.6F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 1).addBox(-7.0F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(61, 1).addBox(-8.4F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 1).addBox(-9.8F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-4.2F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 1).addBox(-5.6F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(61, 1).addBox(-7.0F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 1).addBox(-8.4F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-9.8F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-2.8F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 1).addBox(-4.2F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(61, 1).addBox(-5.6F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(-7.0F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-8.4F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-1.4F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 1).addBox(-2.8F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(61, 1).addBox(-4.2F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(-5.6F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-7.0F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(5.6F, -2.8F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(0.0F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(-1.4F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(61, 1).addBox(-2.8F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(-4.2F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-5.6F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(7.0F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(101, 1).addBox(5.6F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(4.2F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(1.4F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(0.0F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(61, 1).addBox(-1.4F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(-2.8F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-4.2F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(5.6F, 0.0F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 25).addBox(4.2F, 0.0F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(2.8F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(1.4F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 25).addBox(0.0F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(-1.4F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-2.8F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 25).addBox(2.8F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(101, 1).addBox(1.4F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 1).addBox(0.0F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-1.4F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(4.2F, 2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(101, 1).addBox(2.8F, 2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(1.4F, 2.8F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(0.0F, 2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 25).addBox(5.6F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 25).addBox(4.2F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(2.8F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(0.0F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-1.4F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 25).addBox(7.0F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(61, 25).addBox(5.6F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 25).addBox(4.2F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(0.0F, 5.6F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(-1.4F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-2.8F, 5.6F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(8.4F, 7.0F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 25).addBox(7.0F, 7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(81, 25).addBox(5.6F, 7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(-1.4F, 7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(1, 1).addBox(9.8F, 8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(101, 1).addBox(8.4F, 8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(7.0F, 8.4F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(41, 1).addBox(8.4F, 9.8F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F, 0.0F, 0.0F, 3.1416F));

		PartDefinition halo_tier_1 = partdefinition.addOrReplaceChild("halo_tier_1", CubeListBuilder.create().texOffs(1, 96).addBox(-24.0F, -24.0F, -0.03F, 48.0F, 48.0F, 0.06F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 14.0F, -3.0F));

		return LayerDefinition.create(meshdefinition, 128, 256);
	}
}
