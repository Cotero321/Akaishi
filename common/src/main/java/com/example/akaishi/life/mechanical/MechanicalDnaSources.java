package com.example.akaishi.life.mechanical;

import com.example.akaishi.api.life.ISampleGroup;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import net.minecraft.world.item.ItemStack;

/**
 * DNA 来源解析：从 DNA 槽载体（基因序列片段 / 生命样本）推导 DNA 调校模板。
 * <p>
 * 模板制造厂（服务端写入）与界面（客户端置灰预览）共用同一口径，避免两处解析分叉。
 */
public final class MechanicalDnaSources {

    private MechanicalDnaSources() {
    }

    /** DNA 槽载体：基因序列片段或生命样本（两者均带分组与生物来源） */
    public static boolean isDnaSource(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.geneSequence.get()) || stack.is(ModItems.lifeSample.get()));
    }

    /** 读取 DNA 来源推导调校模板；槽位为空或来源缺失时回退无调校（永不为 null）。 */
    public static MechanicalDnaProfile resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return MechanicalDnaProfile.resolveForSample(null, null);
        }
        String groupId = null;
        String entityId = null;
        if (stack.is(ModItems.geneSequence.get())) {
            ISampleGroup group = AkaishiGeneSequenceItem.getGroup(stack);
            groupId = group != null ? group.getId() : null;
            entityId = AkaishiGeneSequenceItem.getEntityId(stack);
        } else if (stack.is(ModItems.lifeSample.get())) {
            ISampleGroup group = AkaishiLifeSampleItem.getGroup(stack);
            groupId = group != null ? group.getId() : null;
            entityId = AkaishiLifeSampleItem.getEntityId(stack);
        }
        return MechanicalDnaProfile.resolveForSample(groupId, entityId);
    }
}
