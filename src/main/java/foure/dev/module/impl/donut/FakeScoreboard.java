package foure.dev.module.impl.donut;

import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.StringSetting;

@ModuleInfo(
   name = "FakeScoreboard",
   category = Category.DONUT,
   desc = "Shows a fake scoreboard"
)
public class FakeScoreboard extends Function {
   public final StringSetting money = new StringSetting("Money", this, "6b");
   public final StringSetting shards = new StringSetting("Shards", this, "2.3K");
   public final StringSetting kills = new StringSetting("Kills", this, "503");
   public final StringSetting deaths = new StringSetting("Deaths", this, "421");
   public final StringSetting keyAll = new StringSetting("Key All", this, "67m 67s");
   public final StringSetting playtime = new StringSetting("Playtime", this, "22d 9h");
   public final StringSetting team = new StringSetting("Team", this, "novx Team");
   public final BooleanSetting realMoney = new BooleanSetting("Show Real Money", this, false);
   public final BooleanSetting realKey = new BooleanSetting("Show Real KeyAll", this, true);
   public final BooleanSetting hideRegion = new BooleanSetting("Hide the region", this, false);
}
