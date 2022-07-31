package net.earthcomputer.pipeless;

import net.minecraft.data.DataGenerator;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinitionsProvider;

public class PipelessSoundProvider extends SoundDefinitionsProvider {
    public PipelessSoundProvider(DataGenerator generator, ExistingFileHelper helper) {
        super(generator, Pipeless.MODID, helper);
    }

    @Override
    public void registerSounds() {
        add(Pipeless.WALKING_ITEM_APPEAR_SOUND.get(), definition()
            .subtitle(defaultSubtitle(Pipeless.WALKING_ITEM_APPEAR_SOUND.get()))
            .with(sound("random/pop")));
    }

    private static String defaultSubtitle(SoundEvent event) {
        return "subtitles." + Pipeless.MODID + "." + event.getLocation().getPath();
    }
}
