package foure.dev.module.impl.script;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import net.minecraft.text.Text;

@ModuleInfo(
    name = "ScriptEditor",
    category = Category.MISC,
    desc = "Opens a code editor to write and run Java scripts"
)
public class ScriptEditorModule extends Function {

    private final BooleanSetting runOnTick = new BooleanSetting("Run Each Tick", false);

    private Runnable compiledScript = null;

    public ScriptEditorModule() {
        this.addSettings(new Setting[]{this.runOnTick});
        this.setKey(79); // O key default
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (compiledScript == null) {
            mc.execute(() -> mc.setScreen(new ScriptEditorScreen(this)));
            toggle(); // disable self since no script yet
            return;
        }

        if (!((Boolean) runOnTick.getValue())) {
            runScript();
            toggle(); // disable after single run
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!isState() || fullNullCheck()) return;
        if ((Boolean) runOnTick.getValue() && compiledScript != null) {
            runScript();
        }
    }

    public void openEditor() {
        mc.execute(() -> mc.setScreen(new ScriptEditorScreen(this)));
    }

    private void runScript() {
        if (compiledScript == null) return;
        try {
            compiledScript.run();
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.sendMessage(
                    Text.literal("§c[ScriptEditor] Error: " + e.getMessage()), false);
            }
            toggle();
        }
    }

    public void setCompiledScript(Runnable script) {
        this.compiledScript = script;
    }

    public void activateWithScript(Runnable script) {
        this.compiledScript = script;
        if (!isState()) toggle();
    }

    public boolean hasScript() {
        return compiledScript != null;
    }
}
