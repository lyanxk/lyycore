package org.lyy.lyycore.client;

import java.util.Arrays;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.lyy.lyycore.content.wings.FeatherAttack;

/** Pure animation-policy checks: do not instantiate client rendering classes on the server. */
@GameTestHolder("lyycore")
@PrefixGameTestTemplate(false)
public final class WingAnimationRegressions {
    @GameTest(template = "empty", batch = "wing_animation_policy")
    public static void extractedPolicyPreservesFourAndSixteenFeatherTiming(GameTestHelper test) {
        for (int count : new int[]{4, 16}) for (int feather = 0; feather < count; feather++) {
            test.assertTrue(WingAttackAnimation.phase(feather, -1, count) == null, "Inactive attack selected an override");
            test.assertTrue(WingAttackAnimation.phase(feather, 0, count) == WingAttackAnimation.Phase.PREPARE,
                    "Prepare phase was skipped");
            test.assertTrue(WingAttackAnimation.phase(feather, FeatherAttack.PREPARE, count) == WingAttackAnimation.Phase.RELEASE,
                    "Release boundary changed");
            float returned = FeatherAttack.returnedAt(feather, count);
            test.assertTrue(WingAttackAnimation.phase(feather, returned, count) == WingAttackAnimation.Phase.RETURN,
                    "Return phase starts at the wrong time");
            test.assertTrue(WingAttackAnimation.seconds(WingAttackAnimation.Phase.RETURN, feather, returned, count) == 0,
                    "Return clip starts partway through its animation");
            test.assertTrue(WingAttackAnimation.phase(feather, returned + FeatherAttack.RETURN, count) == null,
                    "Completed feather still overrides the base animation");
        }
        test.succeed();
    }

    @GameTest(template = "empty", batch = "wing_animation_policy")
    public static void overridesUseResolvedIndicesWithoutMutatingUnrelatedBones(GameTestHelper test) {
        var animation = new WingAttackAnimation(new int[]{1, 3});
        var pose = new float[5 * 9]; Arrays.fill(pose, 7);
        var hidden = new boolean[5];
        var calls = new int[1];
        WingAttackAnimation.Sampler sampler = (clip, bone, time, output) -> {
            calls[0]++; Arrays.fill(output, bone * 9, (bone + 1) * 9, 42);
        };
        float time = FeatherAttack.release(0, 4);
        animation.apply(pose, hidden, time, 1, sampler);
        test.assertTrue(calls[0] == 1 && pose[9] == 42 && pose[0] == 7 && pose[27] == 7,
                "Override touched an unrelated or unselected bone");
        boolean[] drawingVisibility = hidden.clone();
        animation.apply(new float[pose.length], null, time, 1, sampler);
        test.assertTrue(Arrays.equals(hidden, drawingVisibility), "Locator sampling changed drawing visibility");
        test.succeed();
    }
}
