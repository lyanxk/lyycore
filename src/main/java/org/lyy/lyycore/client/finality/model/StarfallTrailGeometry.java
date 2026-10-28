package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class StarfallTrailGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition starfall_trail = partdefinition.addOrReplaceChild("starfall_trail", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition starfall_trail_body = starfall_trail.addOrReplaceChild("starfall_trail_body", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -16.0F, 0.0F, 1.0F, 16.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 17).addBox(0.9F, -12.0F, 0.0F, 1.0F, 10.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 17).addBox(-1.9F, -12.0F, 0.0F, 1.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition starfall_trail_gold_1_cross_r1 = starfall_trail_body.addOrReplaceChild("starfall_trail_gold_1_cross_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -12.0F, 0.0F, 1.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.4F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition starfall_trail_gold_minus1_cross_r1 = starfall_trail_body.addOrReplaceChild("starfall_trail_gold_minus1_cross_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -12.0F, 0.0F, 1.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.4F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition starfall_trail_light_cross_r1 = starfall_trail_body.addOrReplaceChild("starfall_trail_light_cross_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -16.0F, 0.0F, 1.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		return LayerDefinition.create(meshdefinition, 4, 28);
	}
}
