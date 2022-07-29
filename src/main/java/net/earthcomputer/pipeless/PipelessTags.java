package net.earthcomputer.pipeless;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class PipelessTags {
    private PipelessTags() {}

    public static final class Items {
        private Items() {}

        public static final TagKey<Item> WALKING_ITEM_TEMPT = create("walking_item_tempt");

        private static TagKey<Item> create(String name) {
            return ItemTags.create(new ResourceLocation(Pipeless.MODID, name));
        }
    }
}
