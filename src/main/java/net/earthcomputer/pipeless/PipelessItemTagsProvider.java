package net.earthcomputer.pipeless;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class PipelessItemTagsProvider extends ItemTagsProvider {
    public PipelessItemTagsProvider(
        DataGenerator dataGen,
        BlockTagsProvider blockTags,
        @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(dataGen, blockTags, Pipeless.MODID, existingFileHelper);
    }

    @Override
    protected void addTags() {
        this.tag(PipelessTags.Items.WALKING_ITEM_TEMPT).add(Pipeless.ATTRACTIVE_CHEST_ITEM.get());
    }
}
