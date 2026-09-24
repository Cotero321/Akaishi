/**
 * 统一 HUD 渲染层对外契约（<b>仅客户端</b>，供本模组与附属共用）。
 *
 * <p><b>解决什么问题</b>：每个 HUD 各自 "在屏幕上硬编码坐标 + 各自判断 hideGui/screen" 会导致
 * ① 新增元素必然与既有元素打架；② 锚点与屏幕尺寸的关系散落各处；③ 想避让原版 HUD 只能手工挪常量。
 * 本包把"位置"从元素里抽出来：元素只声明 <b>锚点 + 占位尺寸 + 绘制回调</b>，位置由渲染层统一解算。
 *
 * <p><b>使用方式（附属接入三步）</b>：
 * <ol>
 *   <li>实现 {@link com.example.akaishi.api.hud.AkaishiHudElement}（只依赖本包的 5 个类型）；</li>
 *   <li>在客户端初始化阶段调用
 *       {@link com.example.akaishi.api.hud.AkaishiHudRegistry#register(AkaishiHudElement)}；</li>
 *   <li>在 {@link com.example.akaishi.api.hud.HudRenderContext} 给定的 (x, y, width, height) 内绘制。</li>
 * </ol>
 *
 * <p><b>契约承诺（实现侧可依赖）</b>：
 * <ul>
 *   <li><b>坐标系</b>：逻辑像素、原点在屏幕左上角（与 {@code GuiGraphics} 一致）。</li>
 *   <li><b>何时被调用</b>：客户端渲染线程每帧至多一次；F1 隐藏 HUD 或已打开任意界面时整层不绘制。</li>
 *   <li><b>锚点语义</b>：见 {@link com.example.akaishi.api.hud.HudAnchor}（含每个锚点的锚线与堆叠方向）。</li>
 *   <li><b>不重叠</b>：同锚点元素按 {@code priority} 再按注册顺序纵向堆叠，渲染层保证两两不重叠，
 *       并与原版 HUD 关键区域（快捷栏 / 经验条 / 等级数字 / 生命 / 饥饿 / 护甲 / 氧气 / BOSS 血条）自动避让；
 *       避让只沿堆叠方向推开且始终夹在屏幕内（详见 forge 侧 {@code AkaishiHudLayer}）。</li>
 *   <li><b>线程</b>：注册表线程安全；{@code measure/isVisible/render} 一律在渲染线程调用。</li>
 *   <li><b>异常隔离</b>：单个元素抛错不会中断本帧其它元素的绘制。</li>
 * </ul>
 *
 * <p>本包<b>不</b>接管"全屏视觉层"（边缘泛红 / 噪点低语 / 视野后处理 / 雾效）与"以屏幕中轴为不动点的
 * BOSS 血条"——它们不是可锚定的贴边组件，留在各自的原注册点。
 */
package com.example.akaishi.api.hud;
