package net.earthcomputer.pipeless;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

public class PipelessRecipeProvider extends RecipeProvider {
    public PipelessRecipeProvider(DataGenerator pGenerator) {
        super(pGenerator);
    }

    @Override
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> recipeConsumer) {
        ShapelessRecipeBuilder.shapeless(Pipeless.ATTRACTIVE_CHEST_ITEM.get())
            .requires(Tags.Items.CHESTS_WOODEN)
            .requires(Items.GOLDEN_APPLE)
            .unlockedBy("has_chest_or_golden_apple", inventoryTrigger(
                ItemPredicate.Builder.item().of(Tags.Items.CHESTS_WOODEN).build(),
                ItemPredicate.Builder.item().of(Items.GOLDEN_APPLE).build()
            ))
            .save(recipeConsumer);
    }
}
