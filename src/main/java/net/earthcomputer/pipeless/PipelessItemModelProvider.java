package net.earthcomputer.pipeless;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class PipelessItemModelProvider extends ItemModelProvider {
    public PipelessItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, Pipeless.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(Pipeless.ATTRACTIVE_CHEST_ITEM.get());
    }
}
