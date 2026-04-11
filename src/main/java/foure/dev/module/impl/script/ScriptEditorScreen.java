package foure.dev.module.impl.script;

import foure.dev.module.impl.script.ScriptCompiler.CompileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ScriptEditorScreen extends Screen {

    private static final Path SCRIPTS_DIR = Path.of("config", "novx_scripts");
    private static final int  LINE_HEIGHT = 10;
    private static final int  PADDING     = 8;

    private final ScriptEditorModule module;
    private final MinecraftClient    mc = MinecraftClient.getInstance();

    // editor state
    private final List<StringBuilder> lines = new ArrayList<>();
    private int  cursorLine  = 0;
    private int  cursorCol   = 0;
    private int  scrollLine  = 0;
    private boolean editorFocused = true;

    // layout
    private int editorX, editorY, editorW, editorH;

    // status
    private String  statusMessage = "";
    private boolean statusError   = false;
    private long    statusTime    = 0;

    // widgets
    private TextFieldWidget fileNameField;

    public ScriptEditorScreen(ScriptEditorModule module) {
        super(Text.literal("Script Editor"));
        this.module = module;
        lines.add(new StringBuilder("// Scrie codul tau Java aici"));
        lines.add(new StringBuilder("// Exemplu:"));
        lines.add(new StringBuilder("// mc.player.sendMessage(Text.literal(\"Hello!\"), false);"));
        lines.add(new StringBuilder());
        tryLoadLastScript();
    }

    @Override
    protected void init() {
        int w = this.width, h = this.height;

        editorX = PADDING;
        editorY = 25;
        editorW = w - PADDING * 2;
        editorH = h - 80;

        // file name input
        fileNameField = new TextFieldWidget(
            this.textRenderer, w / 2 - 100, h - 52, 200, 16,
            Text.literal("filename"));
        fileNameField.setText("my_script.java");
        fileNameField.setMaxLength(64);
        addDrawableChild(fileNameField);

        int btnY = h - 28;
        int bx   = w / 2 - 160;

        addDrawableChild(ButtonWidget.builder(Text.literal("Save"),
            btn -> saveScript()).dimensions(bx,      btnY, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Load"),
            btn -> loadScript()).dimensions(bx + 65, btnY, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶ Run"),
            btn -> runScript()).dimensions(bx + 130, btnY, 70, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"),
            btn -> clearEditor()).dimensions(bx + 205, btnY, 55, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("✕ Close"),
            btn -> close()).dimensions(bx + 265, btnY, 60, 18).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // background
        ctx.fill(0, 0, width, height, 0xF0080810);

        // title bar
        ctx.fill(0, 0, width, 22, 0xFF0D0D1A);
        ctx.drawTextWithShadow(textRenderer,
            Text.literal("§b§lScript Editor  §8| §7foure.dev  §8| §7Ctrl+S = Save  Ctrl+R = Run"),
            6, 6, 0xFFFFFF);

        // editor area
        ctx.fill(editorX, editorY, editorX + editorW, editorY + editorH, 0xFF10101E);
        // border
        ctx.fill(editorX, editorY,              editorX + editorW, editorY + 1,       0xFF7B2FBE);
        ctx.fill(editorX, editorY + editorH - 1, editorX + editorW, editorY + editorH, 0xFF7B2FBE);
        ctx.fill(editorX, editorY,              editorX + 1,       editorY + editorH, 0xFF7B2FBE);
        ctx.fill(editorX + editorW - 1, editorY, editorX + editorW, editorY + editorH, 0xFF7B2FBE);

        // gutter
        int gutterW = 28;
        ctx.fill(editorX + 1, editorY + 1, editorX + gutterW, editorY + editorH - 1, 0xFF0A0A15);

        int visLines   = (editorH - 6) / LINE_HEIGHT;
        int textStartX = editorX + gutterW + 4;
        int textStartY = editorY + 4;

        for (int i = 0; i < visLines; i++) {
            int lineIdx = scrollLine + i;
            int renderY = textStartY + i * LINE_HEIGHT;

            // line number
            String num = String.valueOf(lineIdx + 1);
            ctx.drawTextWithShadow(textRenderer, Text.literal(num),
                editorX + gutterW - 4 - textRenderer.getWidth(num), renderY, 0x555577);

            if (lineIdx >= lines.size()) continue;

            String lineText = lines.get(lineIdx).toString();

            // current line highlight
            if (lineIdx == cursorLine) {
                ctx.fill(textStartX - 2, renderY - 1,
                    editorX + editorW - 2, renderY + LINE_HEIGHT - 1, 0x18CC88FF);
            }

            // syntax coloring
            int color = getSyntaxColor(lineText);
            ctx.drawTextWithShadow(textRenderer, Text.literal(lineText), textStartX, renderY, color);

            // cursor blink
            if (editorFocused && lineIdx == cursorLine) {
                int col   = Math.min(cursorCol, lineText.length());
                int curX  = textStartX + textRenderer.getWidth(lineText.substring(0, col));
                if ((System.currentTimeMillis() / 530) % 2 == 0) {
                    ctx.fill(curX, renderY - 1, curX + 1, renderY + LINE_HEIGHT - 1, 0xFFCC88FF);
                }
            }
        }

        // file label
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7File:"),
            width / 2 - 128, height - 50, 0xAAAAAA);

        // status
        if (!statusMessage.isEmpty() && System.currentTimeMillis() - statusTime < 5000) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(statusMessage),
                PADDING, editorY + editorH + 4, statusError ? 0xFF5555 : 0x55FF55);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private int getSyntaxColor(String line) {
        String t = line.trim();
        if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*"))  return 0x4EC94E;
        if (t.startsWith("import ") || t.startsWith("package "))             return 0x9D7FEA;
        for (String kw : new String[]{"public","private","static","void","int","boolean",
            "String","float","double","return","if","else","for","while","new","class",
            "try","catch","throws","final","null","true","false","this"}) {
            if (line.contains(kw + " ") || line.contains(kw + "(") || line.endsWith(kw)) {
                return 0xCC88FF;
            }
        }
        if (line.contains("\"")) return 0xE6A84E;
        return 0xDDDDDD;
    }

    // ── Key input — uses Screen's keyboard handling ───────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Ctrl+S = save, Ctrl+R = run
        boolean ctrl = (modifiers & 2) != 0;
        if (ctrl) {
            if (keyCode == 83) { saveScript(); return true; }
            if (keyCode == 82) { runScript();  return true; }
        }

        if (!editorFocused) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        switch (keyCode) {
            case 256: close(); return true;             // ESC
            case 257: case 335: insertNewLine(); return true; // ENTER
            case 259: backspace(); return true;         // BACKSPACE
            case 261: deleteChar(); return true;        // DELETE
            case 264: moveCursorDown(); return true;    // DOWN
            case 265: moveCursorUp(); return true;      // UP
            case 263: moveCursorLeft(); return true;    // LEFT
            case 262: moveCursorRight(); return true;   // RIGHT
            case 268: cursorCol = 0; return true;       // HOME
            case 269: cursorCol = currentLine().length(); return true; // END
            case 266: // PAGE UP
                scrollLine = Math.max(0, scrollLine - 10);
                cursorLine = Math.max(0, cursorLine - 10);
                return true;
            case 267: // PAGE DOWN
                scrollLine = Math.min(lines.size() - 1, scrollLine + 10);
                cursorLine = Math.min(lines.size() - 1, cursorLine + 10);
                return true;
        }

        // TAB — insert 4 spaces
        if (keyCode == 258) {
            for (int i = 0; i < 4; i++) {
                currentLine().insert(cursorCol, ' ');
                cursorCol++;
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editorFocused && chr >= 32) {
            currentLine().insert(cursorCol, chr);
            cursorCol++;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        editorFocused = mouseX >= editorX && mouseX <= editorX + editorW
            && mouseY >= editorY && mouseY <= editorY + editorH;

        if (editorFocused) {
            int gutterW    = 28;
            int textStartX = editorX + gutterW + 4;
            int textStartY = editorY + 4;
            int clickLine  = scrollLine + (int)((mouseY - textStartY) / LINE_HEIGHT);
            clickLine = Math.max(0, Math.min(clickLine, lines.size() - 1));
            cursorLine = clickLine;
            String lt  = lines.get(cursorLine).toString();
            int relX   = (int)(mouseX - textStartX);
            cursorCol  = lt.length();
            for (int i = 0; i <= lt.length(); i++) {
                if (textRenderer.getWidth(lt.substring(0, i)) >= relX) {
                    cursorCol = i; break;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                  double horizontalAmount, double verticalAmount) {
        if (mouseX >= editorX && mouseX <= editorX + editorW
                && mouseY >= editorY && mouseY <= editorY + editorH) {
            scrollLine = Math.max(0, Math.min(
                (int)(scrollLine - verticalAmount), lines.size() - 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ── Cursor helpers ────────────────────────────────────────────────────

    private StringBuilder currentLine() {
        return lines.get(cursorLine);
    }

    private void moveCursorDown() {
        if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorCol = Math.min(cursorCol, currentLine().length());
            ensureVisible();
        }
    }

    private void moveCursorUp() {
        if (cursorLine > 0) {
            cursorLine--;
            cursorCol = Math.min(cursorCol, currentLine().length());
            ensureVisible();
        }
    }

    private void moveCursorLeft() {
        if (cursorCol > 0) {
            cursorCol--;
        } else if (cursorLine > 0) {
            cursorLine--;
            cursorCol = currentLine().length();
            ensureVisible();
        }
    }

    private void moveCursorRight() {
        if (cursorCol < currentLine().length()) {
            cursorCol++;
        } else if (cursorLine < lines.size() - 1) {
            cursorLine++;
            cursorCol = 0;
            ensureVisible();
        }
    }

    private void insertNewLine() {
        StringBuilder cur   = currentLine();
        String        after = cur.substring(cursorCol);
        cur.delete(cursorCol, cur.length());
        cursorLine++;
        lines.add(cursorLine, new StringBuilder(after));
        cursorCol = 0;
        ensureVisible();
    }

    private void backspace() {
        if (cursorCol > 0) {
            currentLine().deleteCharAt(cursorCol - 1);
            cursorCol--;
        } else if (cursorLine > 0) {
            StringBuilder removed = lines.remove(cursorLine);
            cursorLine--;
            cursorCol = currentLine().length();
            currentLine().append(removed);
            ensureVisible();
        }
    }

    private void deleteChar() {
        if (cursorCol < currentLine().length()) {
            currentLine().deleteCharAt(cursorCol);
        } else if (cursorLine < lines.size() - 1) {
            StringBuilder next = lines.remove(cursorLine + 1);
            currentLine().append(next);
        }
    }

    private void ensureVisible() {
        int vis = (editorH - 6) / LINE_HEIGHT;
        if (cursorLine < scrollLine) scrollLine = cursorLine;
        if (cursorLine >= scrollLine + vis) scrollLine = cursorLine - vis + 1;
        scrollLine = Math.max(0, scrollLine);
    }

    private String getFullText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private void setFullText(String text) {
        lines.clear();
        for (String line : text.split("\n", -1)) {
            lines.add(new StringBuilder(line));
        }
        if (lines.isEmpty()) lines.add(new StringBuilder());
        cursorLine = 0; cursorCol = 0; scrollLine = 0;
    }

    private void clearEditor() {
        lines.clear();
        lines.add(new StringBuilder());
        cursorLine = 0; cursorCol = 0; scrollLine = 0;
    }

    // ── File operations ───────────────────────────────────────────────────

    private void saveScript() {
        try {
            Files.createDirectories(SCRIPTS_DIR);
            String name = fileNameField.getText().trim();
            if (name.isEmpty()) name = "script.java";
            if (!name.endsWith(".java")) name += ".java";
            String code = getFullText();
            Files.writeString(SCRIPTS_DIR.resolve(name), code);
            Files.writeString(SCRIPTS_DIR.resolve("last_script.java"), code);
            setStatus("§aSaved → config/foure_scripts/" + name, false);
        } catch (Exception e) {
            setStatus("§cSave error: " + e.getMessage(), true);
        }
    }

    private void loadScript() {
        try {
            String name = fileNameField.getText().trim();
            if (name.isEmpty()) name = "script.java";
            if (!name.endsWith(".java")) name += ".java";
            Path p = SCRIPTS_DIR.resolve(name);
            if (Files.exists(p)) {
                setFullText(Files.readString(p));
                setStatus("§aLoaded: " + name, false);
            } else {
                setStatus("§cNot found: " + name, true);
            }
        } catch (Exception e) {
            setStatus("§cLoad error: " + e.getMessage(), true);
        }
    }

    private void tryLoadLastScript() {
        try {
            Path p = SCRIPTS_DIR.resolve("last_script.java");
            if (Files.exists(p)) setFullText(Files.readString(p));
        } catch (Exception ignored) {}
    }

    // ── Compile & Run ─────────────────────────────────────────────────────

    private void runScript() {
        setStatus("§eCompiling...", false);
        String code = getFullText();

        new Thread(() -> {
            CompileResult result = ScriptCompiler.compile(code);
            mc.execute(() -> {
                if (result.success) {
                    module.activateWithScript(result.runnable);
                    setStatus("§a✓ Compiled & activated!", false);
                } else {
                    String err = result.error != null
                        ? result.error.replace("\n", "  |  ") : "Unknown error";
                    setStatus("§c✗ " + err, true);
                }
            });
        }, "ScriptCompiler-Thread").start();
    }

    private void setStatus(String msg, boolean error) {
        statusMessage = msg;
        statusError   = error;
        statusTime    = System.currentTimeMillis();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
