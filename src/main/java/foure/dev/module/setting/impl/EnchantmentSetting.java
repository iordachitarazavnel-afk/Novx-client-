package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Generated;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;

public class EnchantmentSetting extends Setting<List<RegistryKey<Enchantment>>> {
   private final Set<String> amethystEnchants = new HashSet();
   private final Map<String, Object> metadata = new HashMap();

   public EnchantmentSetting(String name, Function parent) {
      super(name, parent, new ArrayList());
   }

   public EnchantmentSetting(String name) {
      super(name, (Function)null, new ArrayList());
   }

   public List<RegistryKey<Enchantment>> getEnchantments() {
      return (List)this.getValue();
   }

   public boolean isEmpty() {
      return ((List)this.getValue()).isEmpty() && this.amethystEnchants.isEmpty();
   }

   public void addEnchantment(RegistryKey<Enchantment> enchantment) {
      if (!((List)this.getValue()).contains(enchantment)) {
         ((List)this.getValue()).add(enchantment);
      }

   }

   public void removeEnchantment(RegistryKey<Enchantment> enchantment) {
      ((List)this.getValue()).remove(enchantment);
   }

   public void clear() {
      ((List)this.getValue()).clear();
      this.amethystEnchants.clear();
   }

   public void addAmethystEnchant(String enchantName) {
      this.amethystEnchants.add(enchantName);
      this.saveAmethystMetadata();
   }

   public void removeAmethystEnchant(String enchantName) {
      this.amethystEnchants.remove(enchantName);
      this.saveAmethystMetadata();
   }

   public boolean hasAmethystEnchant(String enchantName) {
      return this.amethystEnchants.contains(enchantName);
   }

   public Set<String> getAmethystEnchants() {
      return new HashSet(this.amethystEnchants);
   }

   public boolean hasAmethystPickaxe() {
      return this.amethystEnchants.contains("Amethyst Pickaxe");
   }

   public boolean hasAmethystAxe() {
      return this.amethystEnchants.contains("Amethyst Axe");
   }

   public boolean hasAmethystSellAxe() {
      return this.amethystEnchants.contains("Amethyst Sell Axe");
   }

   public boolean hasAmethystShovel() {
      return this.amethystEnchants.contains("Amethyst Shovel");
   }

   public int getTotalCount() {
      return ((List)this.getValue()).size() + this.amethystEnchants.size();
   }

   public void setMetadata(String key, Object value) {
      this.metadata.put(key, value);
   }

   public <T> T getMetadata(String key) {
      return (T) this.metadata.get(key);
   }

   public List<String> getMetadataList(String key) {
      Object obj = this.metadata.get(key);
      if (obj instanceof List) {
         List<?> list = (List)obj;
         List<String> result = new ArrayList();
         Iterator var5 = list.iterator();

         while(var5.hasNext()) {
            Object o = var5.next();
            if (o instanceof String) {
               result.add((String)o);
            }
         }

         return result;
      } else {
         return new ArrayList();
      }
   }

   public void loadAmethystFromMetadata() {
      List<String> saved = this.getMetadataList("selectedAmethystEnchants");
      this.amethystEnchants.clear();
      this.amethystEnchants.addAll(saved);
   }

   private void saveAmethystMetadata() {
      this.setMetadata("selectedAmethystEnchants", new ArrayList(this.amethystEnchants));
   }

   @Generated
   public Map<String, Object> getMetadata() {
      return this.metadata;
   }
}
