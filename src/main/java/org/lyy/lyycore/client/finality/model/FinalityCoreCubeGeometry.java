package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class FinalityCoreCubeGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition finality_core_cube = partdefinition.addOrReplaceChild("finality_core_cube", CubeListBuilder.create().texOffs(1, 1).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition core = finality_core_cube.addOrReplaceChild("core", CubeListBuilder.create().texOffs(67, 1).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -8.0F, 0.0F));

		PartDefinition halo_tier_1 = partdefinition.addOrReplaceChild("halo_tier_1", CubeListBuilder.create().texOffs(1, 96).addBox(-21.0F, -21.0F, -0.03F, 42.0F, 42.0F, 0.06F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 16.0F, -11.0F));

		return LayerDefinition.create(meshdefinition, 128, 256);
	}
}
