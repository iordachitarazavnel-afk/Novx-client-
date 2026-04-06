package foure.dev.module.setting.api;

import foure.dev.module.api.Function;
import java.util.function.Supplier;
import lombok.Generated;

public abstract class Setting<T> {
   private final String name;
   private Supplier<String> desc;
   private final Function parent;
   private T value;
   private Supplier<Boolean> visible = () -> {
      return true;
   };

   public Setting(String name, Function parent, T defaultValue) {
      this.name = name;
      this.parent = parent;
      this.value = defaultValue;
   }

   public Setting<T> setDesc(Supplier<String> description) {
      this.desc = description;
      return this;
   }

   public Setting<T> setVisible(Supplier<Boolean> visible) {
      this.visible = visible;
      return this;
   }

   public boolean isVisible() {
      return (Boolean)this.visible.get();
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public Supplier<String> getDesc() {
      return this.desc;
   }

   @Generated
   public Function getParent() {
      return this.parent;
   }

   @Generated
   public T getValue() {
      return this.value;
   }

   @Generated
   public Supplier<Boolean> getVisible() {
      return this.visible;
   }

   @Generated
   public void setValue(T value) {
      this.value = value;
   }
}
