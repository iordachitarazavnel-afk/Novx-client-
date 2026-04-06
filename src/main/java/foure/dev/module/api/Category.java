package foure.dev.module.api;

import lombok.Generated;

public enum Category {
   COMBAT("Combat", "B"),
   MOVEMENT("Movement", "C"),
   RENDER("Render", "elementCodec"),
   BASEFINDS("Basefinds", "E"),
   MISC("Misc", "D"),
   THEME("Themes", "G"),
   SCRIPT("Script", "R"),
   CONFIG("Config", "j"),
   DONUT("Donut", "O");

   private final String name;
   private final String icon;

   private Category(String name, String icon) {
      this.name = name;
      this.icon = icon;
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getIcon() {
      return this.icon;
   }

   // $FF: synthetic method
   private static Category[] $values() {
      return new Category[]{COMBAT, MOVEMENT, RENDER, BASEFINDS, MISC, THEME, SCRIPT, CONFIG, DONUT};
   }
}
