package hu.steradian.co2coremod.util;

import hu.steradian.co2coremod.Co2CoreMod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.stream.Collectors;

public class ModTags {
    public static class Blocks {
        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Co2CoreMod.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> BOWLS =
                createTag("bowls");

        public static final TagKey<Item> CO2_PROTECTION_HELMETS =
                createTag("co2_protection_helmets");

        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Co2CoreMod.MOD_ID, name));
        }
    }

    public static List<Block> getBlocksFromTag(TagKey<Block> tag) {
        return BuiltInRegistries.BLOCK.getOrThrow(tag).stream().map(Holder::value).collect(Collectors.toList());
    }
}
