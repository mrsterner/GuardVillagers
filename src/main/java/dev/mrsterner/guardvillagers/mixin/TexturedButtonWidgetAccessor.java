package dev.mrsterner.guardvillagers.mixin;

import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TexturedButtonWidget.class)
public interface TexturedButtonWidgetAccessor {
    @Accessor("v")
    int v();

    @Accessor("hoveredVOffset")
    int hoveredVOffset();

    @Accessor("textureWidth")
    int textureWidth();

    @Accessor("textureHeight")
    int textureHeight();
}
