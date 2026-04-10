package foure.dev.module.impl.Script;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.presss.EventPress;
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
    private boolean  hasRun         = false;

    public ScriptEditorModule() {
        this.addSettings(new Setting[]{this.runOnTick});
        // open GUI on key press (set key in GUI)
        this.setKey(79); // O key by default
    }

    public void onEnable() {
        super.onEnable();
        hasRun = false;

        if (compiledScript == null) {
            // no script compiled yet — open editor
            mc.execute(() -> mc.setScreen(new ScriptEditorScreen(this)));
            setEnabled(false);
            return;
        }

        // run script once on enable
        if (!((Boolean) runOnTick.getValue())) {
            runScript();
            setEnabled(false);
        }
    }

    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!isState() || fullNullCheck()) return;

        // run each tick if setting enabled
        if ((Boolean) runOnTick.getValue() && compiledScript != null) {
            runScript();
        }
    }

    @Subscribe
    public void onPress(EventPress event) {
        // open editor on right-click in module list or via key
        if (event.getAction() != 0) return;
    }

    /**
     * Opens the script editor GUI.
     * Called from ClickGUI on right-click of this module.
     */
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
            setEnabled(false);
        }
    }

    public void setCompiledScript(Runnable script) {
        this.compiledScript = script;
        this.hasRun = false;
    }

    public boolean hasScript() {
        return compiledScript != null;
    }
}

