package net.earthcomputer.pipeless;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class PipelessBlockTagsProvider extends BlockTagsProvider {
    public PipelessBlockTagsProvider(
        DataGenerator dataGen,
        @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(dataGen, Pipeless.MODID, existingFileHelper);
    }

    @Override
    protected void addTags() {
    }
}
