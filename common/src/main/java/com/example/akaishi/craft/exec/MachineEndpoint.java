package com.example.akaishi.craft.exec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 真机加工里的「机台端点」：执行器只通过它投料与收货，不关心对面是自研机台还是第三方机器。
 *
 * <p><b>为什么要有这层抽象</b>：自研机台能用槽位语义精准投料（{@code IItemPipeDevice} 的输入/输出槽），
 * 第三方机器只有标准物品能力（forge {@code ITEM_HANDLER}）、没有输入输出之分。执行器若直接持有两种类型，
 * 每个动作都要写两遍分支；抽出端点后，差异只落在两个实现里。
 *
 * <p><b>不锁机台</b>（用户口径）：端点不申请任何占用，多张订单可以同时用同一台机器。
 * 代价是输出槽里可能有别人的东西，因此收货严格遵守"只取本节点期望的那件、且不超期望件数"（见
 * {@link com.example.akaishi.craft.VirtualCraftPlanner.Step#expected()}）。
 */
public interface MachineEndpoint {

    BlockPos pos();

    /** 是否自研机台（false = 经无线接入器认可的第三方机器）：诊断与界面区分来源用 */
    boolean own();

    /**
     * 输入侧还放得下这件物品吗（<b>零副作用</b>预检）。
     * <p>只回答"有没有位置"，不回答"能放多少" —— 执行器把塞不下的余量留着下一 tick 继续投，
     * 不需要在这里算量（算量就要模拟插入，那就不再是零副作用）。
     */
    boolean hasInputRoom(Item item);

    /** 投料：返回<b>未塞入的余量</b>（空 = 全部塞入） */
    ItemStack insert(ItemStack stack);

    /** 从<b>输出侧</b>取回指定物品（最多 max 件）；不匹配返回空堆 */
    ItemStack extract(Item item, int max);

    /** 从<b>输入侧</b>撤回指定物品（中断/失败时的尽力挽回）；取不到返回空堆 */
    ItemStack reclaim(Item item, int max);
}
