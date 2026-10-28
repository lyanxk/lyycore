package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class StarfallImpactGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition starfall_impact = partdefinition.addOrReplaceChild("starfall_impact", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition starfall_impact_body = starfall_impact.addOrReplaceChild("starfall_impact_body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition starfall_impact_shard_7_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_7_cross_r1", CubeListBuilder.create().texOffs(3, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1213F, 0.0F, -2.1213F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_6_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_6_cross_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_5_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_5_cross_r1", CubeListBuilder.create().texOffs(3, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.1213F, 0.0F, -2.1213F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_4_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_4_cross_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_3_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_3_cross_r1", CubeListBuilder.create().texOffs(3, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.1213F, 0.0F, 2.1213F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_2_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_2_cross_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_1_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_1_cross_r1", CubeListBuilder.create().texOffs(3, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1213F, 0.0F, 2.1213F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_0_cross_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_0_cross_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.4363F));

		PartDefinition starfall_impact_shard_7_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_7_r1", CubeListBuilder.create().texOffs(3, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(3, 17).addBox(-0.5F, -1.7F, 4.2426F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.1213F, 0.0F, -2.1213F, 0.0F, 0.0F, 0.4363F));

		PartDefinition starfall_impact_shard_6_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_6_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 17).addBox(-0.5F, -1.7F, 6.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition starfall_impact_shard_5_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_5_r1", CubeListBuilder.create().texOffs(3, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(3, 17).addBox(-0.5F, -1.7F, 4.2426F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.1213F, 0.0F, -2.1213F, 0.0F, 0.0F, 0.4363F));

		PartDefinition starfall_impact_shard_4_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_4_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition starfall_impact_shard_0_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_shard_0_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-0.5F, -1.7F, 0.0F, 1.0F, 1.5F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition starfall_impact_dust_r1 = starfall_impact_body.addOrReplaceChild("starfall_impact_dust_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, 0.0F, 16.0F, 16.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 20);
	}
}
