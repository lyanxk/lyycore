import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;

/** Standalone regression for the real smelter hierarchy under reusable pose entries. */
class PoseSnapshotCheck {
    static final class Pool {
        final ArrayDeque<PoseStack.Pose> stack = new ArrayDeque<>(), cache = new ArrayDeque<>();
        Pool() { stack.add(new PoseStack().last().copy()); }
        PoseStack.Pose last() { return stack.getLast(); }
        void push() {
            var previous = last();
            var next = cache.isEmpty() ? previous.copy() : cache.removeLast();
            next.pose().set(previous.pose());
            next.normal().set(previous.normal());
            stack.addLast(next);
        }
        void pop() { cache.addLast(stack.removeLast()); }
    }
    static com.google.gson.JsonArray bones;
    static PoseStack.Pose[] held;
    static void visit(Pool pool, int index, boolean snapshot) {
        var bone = bones.get(index).getAsJsonObject();
        int parent = bone.get("parent").getAsInt();
        var pivot = bone.getAsJsonArray("pivot");
        var parentPivot = parent < 0 ? null : bones.get(parent).getAsJsonObject().getAsJsonArray("pivot");
        float[] delta = new float[3];
        for (int a = 0; a < 3; a++) delta[a] = (pivot.get(a).getAsFloat() - (parentPivot == null ? 0 : parentPivot.get(a).getAsFloat())) / 16;
        pool.push();
        pool.last().pose().translate(delta[0], delta[1], delta[2]);
        var r = bone.getAsJsonArray("rotation");
        float radians = (float)(Math.PI / 180);
        pool.last().pose().rotate(new Quaternionf().rotationZYX(r.get(2).getAsFloat()*radians,
                r.get(1).getAsFloat()*radians, r.get(0).getAsFloat()*radians));
        held[index] = snapshot ? pool.last().copy() : pool.last();
        for (int i = index + 1; i < bones.size(); i++)
            if (bones.get(i).getAsJsonObject().get("parent").getAsInt() == index) visit(pool, i, snapshot);
        pool.pop();
    }
    static Vector3f center(boolean snapshot) {
        var pool = new Pool();
        pool.last().pose().translate(.5f, 0, .5f);
        held = new PoseStack.Pose[bones.size()];
        int crystal = -1;
        for (int i = 0; i < bones.size(); i++) {
            var bone = bones.get(i).getAsJsonObject();
            if (bone.get("parent").getAsInt() < 0) visit(pool, i, snapshot);
            if (bone.get("name").getAsString().equals("pink_crystal_core")) crystal = i;
        }
        if (crystal < 0) throw new AssertionError("Missing crystal");
        return held[crystal].pose().transformPosition(new Vector3f());
    }
    public static void main(String[] args) throws Exception {
        bones = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/lyycore/geometry/pure_smelting_plant.json")))
                .getAsJsonObject().getAsJsonArray("bones");
        var expected = new Vector3f(.5f, 24/16f, .5f);
        var old = center(false);
        var fixed = center(true);
        if (old.distance(expected) < .1f) throw new AssertionError("Did not reproduce pooled-pose corruption");
        if (fixed.distance(expected) > .00001f) throw new AssertionError("Snapshot lost crystal position: " + fixed);
        String renderer = Files.readString(Path.of("src/main/java/org/lyy/lyycore/client/AnimatedMeshModel.java"));
        if (java.util.regex.Pattern.compile("poses\\[[^]]+\\]\\s*=\\s*pose\\.last\\(\\)\\s*;").matcher(renderer).find())
            throw new AssertionError("Renderer still retains mutable pose references");
        System.out.println("Smelter crystal: pooled reference " + old + "; snapshot " + fixed + "; expected " + expected);
        System.out.println("Pooled-pose regression passed; all renderer paths use snapshots.");
    }
}
