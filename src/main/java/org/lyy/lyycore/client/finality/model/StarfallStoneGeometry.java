package org.lyy.lyycore.client.finality.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Geometry exported from the supplied Blockbench model. */
public final class StarfallStoneGeometry {
    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition starfall_stone = partdefinition.addOrReplaceChild("starfall_stone", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition starfall_stone_body = starfall_stone.addOrReplaceChild("starfall_stone_body", CubeListBuilder.create().texOffs(0, 0).addBox(2.7F, -1.0F, -1.0F, 1.1F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition starfall_stone_rock_r1 = starfall_stone_body.addOrReplaceChild("starfall_stone_rock_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2094F, -0.3491F, 0.1745F));

		return LayerDefinition.create(meshdefinition, 32, 16);
	}
}
