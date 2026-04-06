package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import net.minecraft.item.Item;

public class ItemSetting extends Setting<Item> {
   public ItemSetting(String name, Function parent, Item defaultValue) {
      super(name, parent, defaultValue);
   }

   public ItemSetting(String name, Item defaultValue) {
      super(name, (Function)null, defaultValue);
   }

   public Item getItem() {
      return (Item)this.getValue();
   }
}
