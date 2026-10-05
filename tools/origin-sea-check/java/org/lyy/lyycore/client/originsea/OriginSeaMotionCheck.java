// 始源星海：暂不使用。原代码逐行注释保留，恢复前请重新验证。
// package org.lyy.lyycore.client.originsea;

// import com.mojang.blaze3d.pipeline.TextureTarget;
// import com.mojang.blaze3d.platform.NativeImage;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.Screenshot;
// import net.minecraft.resources.ResourceLocation;
// import net.minecraft.world.phys.Vec3;
// import net.neoforged.neoforge.client.event.ViewportEvent;
// import org.joml.Matrix4f;
// import org.joml.Quaternionf;
// import org.lwjgl.BufferUtils;
// import org.lwjgl.opengl.GL11;
// import org.lwjgl.opengl.GL15;
// import org.lwjgl.opengl.GL20;
// import org.lwjgl.opengl.GL30;

// import java.nio.FloatBuffer;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.stream.Collectors;

// /** GPU regression checks, deliberately excluded from the release jar. */
// final class OriginSeaMotionCheck {
//     private OriginSeaMotionCheck() {}

//     static void run(Path output) throws Exception {
//         String motion = checkShardMotion();
//         checkSkyCamera(output);
//         Files.writeString(output.resolve("motion-result.txt"), motion
//                 + "PASS: bobbed/unbobbed sky pixels identical; turning and FOV still work; translation invariant.\n");
//     }

//     private static String source(String path) throws Exception {
//         var id = ResourceLocation.fromNamespaceAndPath("lyycore", path);
//         try (var reader = Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow().openAsReader()) {
//             return reader.lines().collect(Collectors.joining("\n"));
//         }
//     }

//     private static String checkShardMotion() throws Exception {
//         // Run the actual shipped vertex source with transform feedback. Sampling
//         // its centers separates movement from nebula/star flicker in screenshots.
//         String vertex = source("shaders/core/origin_shards.vsh").replace(
//                 "#moj_import <lyycore:origin_firmament.glsl>", source("shaders/include/origin_firmament.glsl"));
//         int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
//         int oldVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
//         int oldBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
//         boolean oldDiscard = GL11.glIsEnabled(GL30.GL_RASTERIZER_DISCARD);
//         int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER), program = GL20.glCreateProgram();
//         int vao = GL30.glGenVertexArrays(), ids = GL15.glGenBuffers(), feedback = GL15.glGenBuffers();
//         try {
//             GL20.glShaderSource(shader, vertex);
//             GL20.glCompileShader(shader);
//             require(GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != 0, GL20.glGetShaderInfoLog(shader));
//             GL20.glAttachShader(program, shader);
//             GL30.glTransformFeedbackVaryings(program, new String[]{"gl_Position", "shardColor"}, GL30.GL_INTERLEAVED_ATTRIBS);
//             GL20.glLinkProgram(program);
//             require(GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != 0, GL20.glGetProgramInfoLog(program));
//             GL20.glUseProgram(program);
//             GL30.glBindVertexArray(vao);
//             FloatBuffer vertices = BufferUtils.createFloatBuffer(OriginSeaClient.MAX_SHARDS * 2);
//             for (int i = 0; i < OriginSeaClient.MAX_SHARDS; i++) vertices.put(i).put(0);
//             vertices.flip();
//             GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ids);
//             GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
//             int uv = GL20.glGetAttribLocation(program, "UV0");
//             GL20.glEnableVertexAttribArray(uv);
//             GL20.glVertexAttribPointer(uv, 2, GL11.GL_FLOAT, false, 8, 0L);
//             GL20.glVertexAttrib4f(GL20.glGetAttribLocation(program, "Color"), 1, 1, 1, 1);
//             FloatBuffer identity = new Matrix4f().get(BufferUtils.createFloatBuffer(16));
//             GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, "ModelViewMat"), false, identity);
//             GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, "ProjMat"), false, identity);
//             GL20.glUniform1f(GL20.glGetUniformLocation(program, "ShardCount"), OriginSeaClient.MAX_SHARDS);
//             GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, feedback);
//             GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, OriginSeaClient.MAX_SHARDS * 8L * Float.BYTES, GL15.GL_STREAM_READ);
//             GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, feedback);
//             GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
//             var centerA = sample(program, 7, false);
//             var centerB = sample(program, 7.5F, false);
//             var cornerA = sample(program, 7, true);
//             var cornerB = sample(program, 7.5F, true);
//             FloatBuffer sideways = new Matrix4f().rotateY((float)Math.PI/2).get(BufferUtils.createFloatBuffer(16));
//             GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, "ModelViewMat"), false, sideways);
//             var depthA = sample(program, 7, false);
//             GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(program, "ModelViewMat"), false, identity);
//             float minimumElevation = Float.POSITIVE_INFINITY;
//             for (int i = 0; i < OriginSeaClient.MAX_SHARDS; i++) {
//                 int k = i * 8;
//                 float elevation = (float)Math.toDegrees(Math.atan2(centerA.get(k+1), Math.hypot(centerA.get(k), depthA.get(k))));
//                 minimumElevation = Math.min(minimumElevation, elevation);
//                 require(elevation > 12, "Shard fell out of the firmament into the lower sky: " + i + " at " + elevation);
//             }
//             // Check respawns at frame intervals: a half-second sample can already
//             // show the next plate fully faded in, even though its reset was hidden.
//             var previous = centerA;
//             for (int frame = 1; frame <= 30; frame++) {
//                 var current = sample(program, 7 + frame/60F, false);
//                 for (int i = 0; i < OriginSeaClient.MAX_SHARDS; i++) {
//                     int k = i * 8;
//                     if (i % 5 < 3) {
//                         require(previous.get(k) == current.get(k) && previous.get(k+1) == current.get(k+1),
//                                 "Suspended rim plate drifted away from the lip: " + i);
//                     } else if (current.get(k+1) > previous.get(k+1)) {
//                         require(previous.get(k+7) < 0.02F && current.get(k+7) < 0.02F, "Visible respawn: " + i);
//                     } else require(previous.get(k+1)-current.get(k+1) > 0.002F, "Stationary GPU center: " + i);
//                 }
//                 previous = current;
//             }
//             int fixed = 0, continuous = 0, falling = 0, tumbling = 0;
//             float minimumDrop = Float.POSITIVE_INFINITY;
//             for (int i = 0; i < OriginSeaClient.MAX_SHARDS; i++) {
//                 int k = i * 8;
//                 if (i % 5 < 3) {
//                     fixed++;
//                     continue;
//                 }
//                 // Respawning pieces were already checked with the finer samples.
//                 if (centerB.get(k + 1) > centerA.get(k + 1)) {
//                     continue;
//                 }
//                 continuous++;
//                 float drop = centerA.get(k+1) - centerB.get(k+1);
//                 minimumDrop = Math.min(minimumDrop, drop);
//                 if (drop > 0.10F) falling++;
//                 float dx = (cornerB.get(k)-centerB.get(k)) - (cornerA.get(k)-centerA.get(k));
//                 float dy = (cornerB.get(k+1)-centerB.get(k+1)) - (cornerA.get(k+1)-centerA.get(k+1));
//                 if (dx*dx + dy*dy > 0.000001F) tumbling++;
//             }
//             require(fixed == OriginSeaClient.MAX_SHARDS * 3 / 5, "Missing permanent rim plates");
//             require(continuous > OriginSeaClient.MAX_SHARDS * 0.35, "Too many simultaneous respawns");
//             require(falling == continuous, "Stationary shards: " + (continuous-falling));
//             require(tumbling > continuous * 0.98, "Tumbling missing from too many shards");
//             return "PASS: " + fixed + " fixed rim plates; GPU centers falling " + falling + "/" + continuous + " between respawns; "
//                     + tumbling + " tumbling; minimum drop " + minimumDrop + " units per 0.5 seconds; "
//                     + "minimum elevation " + minimumElevation + " degrees.\n";
//         } finally {
//             if (!oldDiscard) GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);
//             GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
//             GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);
//             GL30.glBindVertexArray(oldVao);
//             GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, oldBuffer);
//             GL20.glUseProgram(oldProgram);
//             GL15.glDeleteBuffers(feedback);
//             GL15.glDeleteBuffers(ids);
//             GL30.glDeleteVertexArrays(vao);
//             GL20.glDeleteProgram(program);
//             GL20.glDeleteShader(shader);
//         }
//     }

//     private static FloatBuffer sample(int program, float seconds, boolean corner) {
//         GL20.glVertexAttrib3f(GL20.glGetAttribLocation(program, "Position"), corner ? 0.7F : 0,
//                 corner ? 0.4F : 0, corner ? 0.05F : 0);
//         GL20.glUniform1f(GL20.glGetUniformLocation(program, "SceneTime"), seconds);
//         GL30.glBeginTransformFeedback(GL11.GL_POINTS);
//         GL11.glDrawArrays(GL11.GL_POINTS, 0, OriginSeaClient.MAX_SHARDS);
//         GL30.glEndTransformFeedback();
//         var values = BufferUtils.createFloatBuffer(OriginSeaClient.MAX_SHARDS * 8);
//         GL15.glGetBufferSubData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, values);
//         return values;
//     }

//     private static void checkSkyCamera(Path output) throws Exception {
//         var mc = Minecraft.getInstance();
//         var camera = mc.gameRenderer.getMainCamera();
//         var worldFov = new ViewportEvent.ComputeFov(mc.gameRenderer, camera, 0, 70, true);
//         OriginSeaClient.captureWorldFov(worldFov);
//         worldFov.setFOV(83); // Another event subscriber modifies FOV after ours.
//         OriginSeaClient.captureWorldFov(new ViewportEvent.ComputeFov(mc.gameRenderer, camera, 0, 49, false));
//         Matrix4f projection = mc.gameRenderer.getProjectionMatrix(83);
//         require(OriginSeaClient.skyProjection(new Matrix4f()).equals(projection, 0.00001F), "Lost world FOV / used hand FOV");
//         var bobbed = new Matrix4f(projection).translate(0.055F,-0.085F,0).rotateZ(0.016F).rotateX(0.027F);
//         var view = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
//         var target = new TextureTarget(960, 540, true, Minecraft.ON_OSX);
//         target.setClearColor(0, 0, 0, 1);
//         try {
//             try (var still = skyFrame(target, view, projection); var walking = skyFrame(target, view, bobbed);
//                  var turned = skyFrame(target, new Matrix4f(view).rotateY(0.25F), bobbed)) {
//                 require(differences(still, walking, still.getHeight()) == 0, "Walking bob moved the sky");
//                 require(differences(still, turned, still.getHeight()) > 960*540/5, "Sky no longer follows turning");
//                 still.writeToFile(output.resolve("sky-no-bob.png"));
//                 walking.writeToFile(output.resolve("sky-with-bob.png"));
//             }
//             // World/camera translations must not drag the celestial meshes around.
//             target.clear(Minecraft.ON_OSX);
//             target.bindWrite(true);
//             OriginSeaRenderer.renderSky(view, projection, Vec3.ZERO, 7);
//             try (var first = Screenshot.takeScreenshot(target)) {
//                 target.clear(Minecraft.ON_OSX);
//                 target.bindWrite(true);
//                 OriginSeaRenderer.renderSky(new Matrix4f(view).translate(5,9,3), projection, new Vec3(60,20,-90), 7);
//                 try (var shifted = Screenshot.takeScreenshot(target)) {
//                     // Exclude the lower sea's intentional world-space ripples.
//                     require(differences(first, shifted, first.getHeight()/3) == 0, "Translation moved the celestial background");
//                 }
//             }
//             Path frames = output.resolve("motion-frames");
//             Files.createDirectories(frames);
//             var previewView = new Matrix4f().lookAt(0,0,0, 0,0.62F,1, 0,1,0);
//             var previewProjection = new Matrix4f().perspective((float)Math.toRadians(70), 16F/9, 0.05F, 512);
//             for (int i = 0; i < 96; i++) {
//                 target.clear(Minecraft.ON_OSX);
//                 target.bindWrite(true);
//                 OriginSeaRenderer.renderSky(previewView, previewProjection, Vec3.ZERO, 7 + i/24F);
//                 try (var pixels = Screenshot.takeScreenshot(target)) {
//                     pixels.writeToFile(frames.resolve(String.format("frame-%03d.png", i)));
//                 }
//             }
//             require(GL11.glGetError() == GL11.GL_NO_ERROR, "OpenGL error in motion/camera checks");
//         } finally {
//             target.destroyBuffers();
//             mc.getMainRenderTarget().bindWrite(true);
//         }
//     }

//     private static NativeImage skyFrame(TextureTarget target, Matrix4f view, Matrix4f projection) {
//         var mc = Minecraft.getInstance();
//         target.clear(Minecraft.ON_OSX);
//         target.bindWrite(true);
//         require(mc.level.effects().renderSky(mc.level, 0, 0, view, mc.gameRenderer.getMainCamera(), projection, false, () -> {}),
//                 "Sky hook did not run");
//         return Screenshot.takeScreenshot(target);
//     }

//     private static int differences(NativeImage first, NativeImage second, int rows) {
//         int changed = 0;
//         for (int y = 0; y < rows; y++) for (int x = 0; x < first.getWidth(); x++) {
//             if (first.getPixelRGBA(x,y) != second.getPixelRGBA(x,y)) changed++;
//         }
//         return changed;
//     }

//     private static void require(boolean condition, String message) {
//         if (!condition) throw new IllegalStateException(message);
//     }
// }
