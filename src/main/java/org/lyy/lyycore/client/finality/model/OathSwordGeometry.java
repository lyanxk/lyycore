package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class OathSwordGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition oath_sword = partdefinition.addOrReplaceChild("oath_sword", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition oath_sword_body = oath_sword.addOrReplaceChild("oath_sword_body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition oath_sword_blade = oath_sword_body.addOrReplaceChild("oath_sword_blade", CubeListBuilder.create().texOffs(0, 0).addBox(-11.2F, -11.2F, -0.55F, 4.2F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-7.0F, -9.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(27, 0).addBox(-9.8F, -9.8F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-11.2F, -9.8F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.6F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(27, 0).addBox(-7.0F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-8.4F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(27, 0).addBox(-9.8F, -8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-4.2F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(27, 0).addBox(-5.6F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-7.0F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(27, 0).addBox(-8.4F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-9.8F, -7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.8F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(27, 0).addBox(-4.2F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-5.6F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(-7.0F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-8.4F, -5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-1.4F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(27, 0).addBox(-2.8F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-4.2F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(-5.6F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-7.0F, -4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(5.6F, -2.8F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(0.0F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(-1.4F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-2.8F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(-4.2F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-5.6F, -2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(7.0F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(5.6F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(4.2F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(1.4F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(0.0F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-1.4F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(-2.8F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-4.2F, -1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(5.6F, 0.0F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 0).addBox(4.2F, 0.0F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(2.8F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(1.4F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 0).addBox(0.0F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(-1.4F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-2.8F, 0.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(21, 0).addBox(2.8F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(1.4F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(42, 0).addBox(0.0F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-1.4F, 1.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(4.2F, 2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(2.8F, 2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(1.4F, 2.8F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(0.0F, 2.8F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(54, 0).addBox(5.6F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(60, 0).addBox(4.2F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(2.8F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(0.0F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-1.4F, 4.2F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(54, 0).addBox(7.0F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(66, 0).addBox(5.6F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(72, 0).addBox(4.2F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(0.0F, 5.6F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-1.4F, 5.6F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-2.8F, 5.6F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(8.4F, 7.0F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(60, 0).addBox(7.0F, 7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(72, 0).addBox(5.6F, 7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(-1.4F, 7.0F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(9.8F, 8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(48, 0).addBox(8.4F, 8.4F, -0.55F, 1.4F, 1.4F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(7.0F, 8.4F, -0.55F, 1.4F, 2.8F, 1.1F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(8.4F, 9.8F, -0.55F, 2.8F, 1.4F, 1.1F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F));

		return LayerDefinition.create(meshdefinition, 80, 4);
	}
}
