package org.lyy.lyycore.client;

import com.google.gson.*;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.*;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Imported cuboids and low-poly meshes, baked once into ordinary chunk/item quads. */
public final class MachineMeshGeometry extends SimpleUnbakedGeometry<MachineMeshGeometry> {
    public static final IGeometryLoader<MachineMeshGeometry> LOADER = MachineMeshGeometry::read;
    private record Face(String texture, boolean emissive, float[][] vertices) { }
    private final List<Face> faces;
    private MachineMeshGeometry(List<Face> faces) { this.faces = List.copyOf(faces); }

    private static MachineMeshGeometry read(JsonObject json, JsonDeserializationContext context) {
        var faces = new ArrayList<Face>();
        for (var entry : json.getAsJsonArray("faces")) {
            var face = entry.getAsJsonObject();
            var points = face.getAsJsonArray("vertices");
            if (points.size() < 3 || points.size() > 4) throw new JsonParseException("Expected a triangle or quad");
            float[][] vertices = new float[points.size()][5];
            for (int i = 0; i < vertices.length; i++) {
                var point = points.get(i).getAsJsonArray();
                if (point.size() != 5) throw new JsonParseException("Expected XYZ and UV");
                for (int j = 0; j < 5; j++) vertices[i][j] = point.get(j).getAsFloat();
            }
            faces.add(new Face(face.get("texture").getAsString(), face.get("emissive").getAsBoolean(), vertices));
        }
        return new MachineMeshGeometry(faces);
    }

    @Override protected void addQuads(IGeometryBakingContext context, IModelBuilder<?> builder, ModelBaker baker,
                                     Function<Material, TextureAtlasSprite> sprites, ModelState state) {
        var transform = state.getRotation().compose(context.getRootTransform()).blockCenterToCorner();
        for (Face face : faces) {
            var sprite = sprites.apply(context.getMaterial(face.texture));
            float[][] v = face.vertices;
            var a = new Vector3f(v[0][0], v[0][1], v[0][2]);
            var normal = new Vector3f(v[1][0], v[1][1], v[1][2]).sub(a)
                    .cross(new Vector3f(v[2][0], v[2][1], v[2][2]).sub(a)).normalize();
            transform.transformNormal(normal);
            var quad = new QuadBakingVertexConsumer();
            quad.setSprite(sprite);
            quad.setTintIndex(-1);
            quad.setShade(!face.emissive);
            quad.setDirection(Direction.getNearest(normal.x, normal.y, normal.z));
            for (int i = 0; i < 4; i++) {
                float[] point = v[Math.min(i, v.length - 1)];
                var position = new Vector4f(point[0], point[1], point[2], 1);
                transform.transformPosition(position);
                quad.addVertex(position.x, position.y, position.z).setColor(-1)
                        .setUv(sprite.getU(point[3]), sprite.getV(point[4]))
                        .setLight(face.emissive ? LightTexture.FULL_BRIGHT : 0).setNormal(normal.x, normal.y, normal.z);
            }
            builder.addUnculledFace(quad.bakeQuad());
        }
    }
}
