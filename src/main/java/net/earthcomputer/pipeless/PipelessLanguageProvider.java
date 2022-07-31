package net.earthcomputer.pipeless;

import net.minecraft.data.DataGenerator;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.LanguageProvider;

public class PipelessLanguageProvider extends LanguageProvider {
    public PipelessLanguageProvider(DataGenerator gen) {
        super(gen, Pipeless.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(Pipeless.ATTRACTIVE_CHEST_ITEM.get(), "Attractive Chest");
        add(Pipeless.WALKING_ITEM_ENTITY.get(), "Walking Item");
        addSubtitle(Pipeless.WALKING_ITEM_APPEAR_SOUND.get(), "Walking Item Appears");
    }

    private void addSubtitle(SoundEvent sound, String translation) {
        add("subtitles." + Pipeless.MODID + "." + sound.getLocation().getPath(), translation);
    }
}
