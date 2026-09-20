package com.example.akaishi.craft.thirdparty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * 第三方认可表的数据包加载器：读 {@code data/<ns>/third_party_process/*.json}。
 *
 * <p>由各加载器在 {@code AddReloadListenerEvent} 里注册（common 只定义监听实现，
 * 挂载点必须在平台侧）。重载即整表替换，与配方热重载同一时机。
 */
public final class ThirdPartyProcessReloadListener extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ThirdPartyProcessReloadListener() {
        super(GSON, "third_party_process");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager,
            ProfilerFiller profiler) {
        ThirdPartyProcesses.apply(files);
    }
}
