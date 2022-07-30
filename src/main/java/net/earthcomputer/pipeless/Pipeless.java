package net.earthcomputer.pipeless;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Pipeless.MODID)
public class Pipeless {

    public static final String MODID = "pipeless";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);

    public static final RegistryObject<Item> ATTRACTIVE_CHEST_ITEM = ITEMS.register("attractive_chest",
        () -> new Item(new Item.Properties()
            .tab(CreativeModeTab.TAB_FOOD)
            .food(new FoodProperties.Builder().nutrition(2).build())
        ));

    public static final RegistryObject<EntityType<WalkingItemEntity>> WALKING_ITEM_ENTITY = ENTITY_TYPES.register("walking_item",
        () -> EntityType.Builder.<WalkingItemEntity>of(WalkingItemEntity::new, MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(6)
            .build("walking_item"));

    public Pipeless() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::gatherData);

        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void gatherData(final GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new PipelessItemModelProvider(event.getGenerator(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new PipelessLanguageProvider(event.getGenerator()));

        event.getGenerator().addProvider(event.includeServer(), new PipelessRecipeProvider(event.getGenerator()));
        BlockTagsProvider blockTags = new PipelessBlockTagsProvider(event.getGenerator(), event.getExistingFileHelper());
        event.getGenerator().addProvider(event.includeServer(), blockTags);
        event.getGenerator().addProvider(event.includeServer(), new PipelessItemTagsProvider(event.getGenerator(), blockTags, event.getExistingFileHelper()));
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level.isClientSide) {
            return;
        }
        WalkingItemEntity.onTemptingEntityTick(entity);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(Pipeless.WALKING_ITEM_ENTITY.get(), WalkingItemEntityRenderer::new);
        }
    }
}
