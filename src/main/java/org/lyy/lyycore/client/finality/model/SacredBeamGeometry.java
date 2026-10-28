package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class SacredBeamGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition sacred_beam = partdefinition.addOrReplaceChild("sacred_beam", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition sacred_beam_body = sacred_beam.addOrReplaceChild("sacred_beam_body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -16.0F, 0.0F, 4.0F, 16.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(9, 0).addBox(-0.5F, -16.0F, 0.0F, 1.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition sacred_beam_body_foot_light_r1 = sacred_beam_body.addOrReplaceChild("sacred_beam_body_foot_light_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		PartDefinition sacred_beam_body_white_axis_1_r1 = sacred_beam_body.addOrReplaceChild("sacred_beam_body_white_axis_1_r1", CubeListBuilder.create().texOffs(9, 0).addBox(-0.5F, -16.0F, 0.0F, 1.0F, 16.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -16.0F, 0.0F, 4.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition sacred_beam_body_soft_column_3_r1 = sacred_beam_body.addOrReplaceChild("sacred_beam_body_soft_column_3_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -16.0F, 0.0F, 4.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));

		PartDefinition sacred_beam_body_soft_column_1_r1 = sacred_beam_body.addOrReplaceChild("sacred_beam_body_soft_column_1_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -16.0F, 0.0F, 4.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

		return LayerDefinition.create(meshdefinition, 16, 28);
	}
}
