package org.lyy.lyycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

/** CPU checks using the real Minecraft PoseStack and baked-quad types; no game window. */
public final class MirroredItemModelCheck {
    public static void main(String[] args) {
        BakedQuad quad = quad();
        int[] original = quad.getVertices().clone();
        BakedQuad reversed = MirroredItemModel.reverseWinding(quad);
        check(Arrays.equals(original, quad.getVertices()), "Source vertex records mutated");
        check(Arrays.equals(original, MirroredItemModel.reverseWinding(reversed).getVertices()), "Reversal not lossless");
        check(reversed.getDirection() == quad.getDirection() && reversed.isShade() == quad.isShade()
                && reversed.hasAmbientOcclusion() == quad.hasAmbientOcclusion() && reversed.getTintIndex() == quad.getTintIndex(),
                "Lighting metadata changed");

        for (float[] scale : new float[][]{{-.7F,.7F,.7F},{.7F,.7F,.7F},{-.7F,-.7F,.7F}}) {
            boolean reflected = scale[0]*scale[1]*scale[2]<0;
            for (boolean guiBaseFlip : new boolean[]{false,true}) {
                Stub originalModel = new Stub(quad, scale);
                BakedModel wrapped = MirroredItemModel.wrap(originalModel);
                PoseStack pose = new PoseStack();
                if (guiBaseFlip) pose.scale(16,-16,16);
                BakedModel transformed = wrapped.applyTransform(ItemDisplayContext.GUI,pose,false);
                BakedModel pass = transformed.getRenderPasses(null,false).getFirst();
                BakedQuad result = pass.getQuads(null,null,null,ModelData.EMPTY,null).getFirst();
                int expected = reflected ? original[3*8] : original[8];
                check(result.getVertices()[8] == expected, "Wrong winding for mirrored / unmirrored GUI");
                check(pass.getQuads(null,null,null,ModelData.EMPTY,null).getFirst()==result,"Repeated draw lost quad cache");
                if (!guiBaseFlip) {
                    Vector3f a=position(result,0,pose),b=position(result,1,pose),c=position(result,2,pose);
                    Vector3f faceNormal=b.sub(a).cross(c.sub(a)).normalize();
                    Vector3f lightingNormal=pose.last().transformNormal(0,0,1,new Vector3f());
                    check(faceNormal.dot(lightingNormal)>.99F,"Visible surface faces away from its lighting normal");
                }
            }
        }
        // Native meshes also contain triangles stored as quads with a duplicated last vertex.
        int[] triangle=original.clone();System.arraycopy(triangle,16,triangle,24,8);
        BakedQuad tri=new BakedQuad(triangle,-1,Direction.SOUTH,null,true,false);
        int[] result=MirroredItemModel.reverseWinding(tri).getVertices();
        check(result[8]==triangle[24]&&result[24]==triangle[8],"Triangle winding was not reversed");
        System.out.println("PASS: mirrored normals/winding, GUI base reflection, positive/even mirrors, render passes, UV/light records and cache");
    }

    private static Vector3f position(BakedQuad q,int vertex,PoseStack pose) {
        int[] v=q.getVertices();int i=vertex*8;
        return pose.last().pose().transformPosition(new Vector3f(Float.intBitsToFloat(v[i]),Float.intBitsToFloat(v[i+1]),Float.intBitsToFloat(v[i+2])));
    }
    private static BakedQuad quad() {
        int[] v=new int[32];float[][] p={{0,0,0},{1,0,0},{1,1,0},{0,1,0}};
        for(int i=0;i<4;i++) {for(int a=0;a<3;a++)v[i*8+a]=Float.floatToRawIntBits(p[i][a]);
            v[i*8+3]=0xff102030+i;v[i*8+4]=Float.floatToRawIntBits(i*.2F);v[i*8+5]=Float.floatToRawIntBits(i*.1F);
            v[i*8+6]=0x00f000f0+i;v[i*8+7]=0x007f0000;}
        return new BakedQuad(v,3,Direction.SOUTH,null,true,false);
    }
    private static void check(boolean ok,String message) {if(!ok)throw new AssertionError(message);}

    private static final class Stub implements BakedModel {
        private final List<BakedQuad> quads;private final float[] scale;
        Stub(BakedQuad q,float[] scale){this.quads=List.of(q);this.scale=scale;}
        public List<BakedQuad> getQuads(BlockState s,Direction d,RandomSource r){return quads;}
        public boolean useAmbientOcclusion(){return false;}
        public boolean isGui3d(){return true;}
        public boolean usesBlockLight(){return false;}
        public boolean isCustomRenderer(){return false;}
        public TextureAtlasSprite getParticleIcon(){return null;}
        public ItemTransforms getTransforms(){return null;}
        public ItemOverrides getOverrides(){return null;}
        public BakedModel applyTransform(ItemDisplayContext c,PoseStack p,boolean left){p.scale(scale[0],scale[1],scale[2]);return this;}
        public List<BakedModel> getRenderPasses(ItemStack stack,boolean fabulous){return List.of(this);}
    }
}
