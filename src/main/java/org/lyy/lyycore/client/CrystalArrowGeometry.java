package org.lyy.lyycore.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Small emissive JSON mesh, baked once on resource reload and reused every frame. */
public final class CrystalArrowGeometry extends SimpleUnbakedGeometry<CrystalArrowGeometry> {
    public static final IGeometryLoader<CrystalArrowGeometry> LOADER = CrystalArrowGeometry::read;
    private final List<Face> faces;

    private CrystalArrowGeometry(List<Face> faces) {
        this.faces = List.copyOf(faces);
    }

    private static CrystalArrowGeometry read(JsonObject json, JsonDeserializationContext context) {
        var faces = new ArrayList<Face>();
        var entries = GsonHelper.getAsJsonArray(json, "faces");
        if (entries.size() > 64) throw new JsonParseException("Crystal arrows support at most 64 faces");
        for (var entry : entries) {
            var face = entry.getAsJsonObject();
            var points = GsonHelper.getAsJsonArray(face, "vertices");
            if (points.size() < 3 || points.size() > 4) throw new JsonParseException("Expected 3 or 4 vertices");
            var vertices = new ArrayList<Vector3f>();
            for (var point : points) {
                var xyz = point.getAsJsonArray();
                if (xyz.size() != 3) throw new JsonParseException("Expected XYZ coordinates");
                var v = new Vector3f(xyz.get(0).getAsFloat(), xyz.get(1).getAsFloat(), xyz.get(2).getAsFloat());
                if (!v.isFinite()) throw new JsonParseException("Non-finite arrow vertex");
                vertices.add(v.div(16));
            }
            faces.add(new Face(List.copyOf(vertices), GsonHelper.getAsString(face, "texture")));
        }
        return new CrystalArrowGeometry(faces);
    }

    @Override
    protected void addQuads(IGeometryBakingContext context, IModelBuilder<?> builder, ModelBaker baker,
                           Function<Material, TextureAtlasSprite> sprites, ModelState state) {
        var transform = state.getRotation().compose(context.getRootTransform()).blockCenterToCorner();
        for (var face : faces) {
            var texture = sprites.apply(context.getMaterial(face.texture()));
            var points = face.vertices();
            var normal = new Vector3f(points.get(1)).sub(points.get(0))
                    .cross(new Vector3f(points.get(2)).sub(points.get(0))).normalize();
            transform.transformNormal(normal);
            var quad = new QuadBakingVertexConsumer();
            quad.setSprite(texture);
            quad.setTintIndex(-1);
            quad.setShade(false);
            quad.setDirection(Direction.getNearest(normal.x, normal.y, normal.z));
            for (int i = 0; i < 4; i++) {
                var position = new Vector4f(points.get(Math.min(i, points.size() - 1)), 1);
                transform.transformPosition(position);
                quad.addVertex(position.x, position.y, position.z)
                        .setColor(-1)
                        .setUv(texture.getU(0.5F), texture.getV(0.5F))
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(normal.x, normal.y, normal.z);
            }
            builder.addUnculledFace(quad.bakeQuad());
        }
    }

    private record Face(List<Vector3f> vertices, String texture) {}
}
