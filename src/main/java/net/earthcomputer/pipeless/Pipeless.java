package net.earthcomputer.pipeless;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
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
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

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

    public static final RegistryObject<SoundEvent> WALKING_ITEM_APPEAR_SOUND = SOUND_EVENTS.register("entity.walking_item.appear",
        () -> new SoundEvent(new ResourceLocation(MODID, "entity.walking_item.appear"), WalkingItemEntity.FOLLOW_DISTANCE + 1));

    public Pipeless() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::gatherData);

        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);

        PipelessNetwork.register();

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void gatherData(final GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new PipelessItemModelProvider(event.getGenerator(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new PipelessSoundProvider(event.getGenerator(), event.getExistingFileHelper()));
        event.getGenerator().addProvider(event.includeClient(), new PipelessLanguageProvider(event.getGenerator()));

        event.getGenerator().addProvider(event.includeServer(), new PipelessRecipeProvider(event.getGenerator()));
        BlockTagsProvider blockTags = new PipelessBlockTagsProvider(event.getGenerator(), event.getExistingFileHelper());
        event.getGenerator().addProvider(event.includeServer(), blockTags);
        event.getGenerator().addProvider(event.includeServer(), new PipelessItemTagsProvider(event.getGenerator(), blockTags, event.getExistingFileHelper()));
    }

    @SubscribeEvent
    public void onEntityInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getSide() == LogicalSide.SERVER
            && event.getItemStack().is(PipelessTags.Items.WALKING_ITEM_TEMPT)
            && event.getTarget() instanceof ArmorStand armorStand
            && !armorStand.isShowArms()
        ) {
            armorStand.setShowArms(true);
            // need to try interacting again, as this event happens after the interaction on the server
            armorStand.interactAt(event.getEntity(), event.getLocalPos(), event.getHand());
        }
    }

    @SubscribeEvent
    public void onEntityStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ItemEntity item) {
            PipelessNetwork.updateClientBobOffset((ServerPlayer) event.getEntity(), item);
        }
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
