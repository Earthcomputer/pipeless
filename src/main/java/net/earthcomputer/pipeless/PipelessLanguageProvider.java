package net.earthcomputer.pipeless;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class PipelessLanguageProvider extends LanguageProvider {
    public PipelessLanguageProvider(DataGenerator gen) {
        super(gen, Pipeless.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(Pipeless.ATTRACTIVE_CHEST_ITEM.get(), "Attractive Chest");
        add(Pipeless.WALKING_ITEM_ENTITY.get(), "Walking Item");
    }
}
