package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class FinalityFloatingCrystalGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition finality_floating_crystal = partdefinition.addOrReplaceChild("finality_floating_crystal", CubeListBuilder.create().texOffs(1, 1).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, -0.2094F, -0.3491F, -0.2443F));

		PartDefinition halo_tier_1 = partdefinition.addOrReplaceChild("halo_tier_1", CubeListBuilder.create().texOffs(1, 96).addBox(-18.0F, -18.0F, -0.03F, 36.0F, 36.0F, 0.06F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, -11.0F));

		return LayerDefinition.create(meshdefinition, 128, 256);
	}
}
