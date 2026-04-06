package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.TextFactoryEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.util.others.Friends;

@ModuleInfo(
   name = "NameProtect",
   category = Category.MISC,
   desc = "Protects your nickname and friends' nicknames from being shown in chat"
)
public class NameProtect extends Function {
   StringSetting nameSetting = new StringSetting("Ник", "efef");
   BooleanSetting friendsSetting = new BooleanSetting("Friends", false);

   public NameProtect() {
      this.addSettings(new Setting[]{this.nameSetting, this.friendsSetting});
   }

   @Subscribe
   public void onTextFactory(TextFactoryEvent e) {
      e.replaceText(mc.getSession().getUsername(), (String)this.nameSetting.getValue());
      if ((Boolean)this.friendsSetting.getValue()) {
         Friends.getFriends().forEach((friend) -> {
            e.replaceText(friend.getName(), (String)this.nameSetting.getValue());
         });
      }

   }
}
