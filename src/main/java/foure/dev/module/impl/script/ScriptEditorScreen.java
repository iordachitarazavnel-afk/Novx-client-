package foure.dev.module.impl.script;

import foure.dev.module.impl.misc.ScriptCompiler.CompileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ScriptEditorScreen extends Screen {

    private static final Path SCRIPTS_DIR = Path.of("config", "foure_scripts");
    private static final int  EDITOR_PADDING = 10;
    private static final int  LINE_HEIGHT    = 10;

    private final ScriptEditorModule module;

    // simple multi-line editor state
    private final List<StringBuilder> lines = new ArrayList<>();
    private int cursorLine = 0;
    private int cursorCol  = 0;
    private int scrollLine = 0;

    private String statusMessage = "";
    private boolean statusError  = false;
    private long statusTime      = 0;

    private TextFieldWidget fileNameField;
    private int editorX, editorY, editorW, editorH;
    private boolean editorFocused = true;

    public ScriptEditorScreen(ScriptEditorModule module) {
        super(Text.literal("Script Editor"));
        this.module = module;
        this.lines.add(new StringBuilder(
            "// Scrie codul tau Java aici\n" +
            "// Exemplu:\n" +
            "// mc.player.sendMessage(Text.literal(\"Hello!\"), false);\n"
        ));
        this.lines.add(new StringBuilder());
        // try load last script
        loadFromFile("last_script.java");
    }

    @Override
    protected void init() {
        int w = this.width, h = this.height;

        editorX = EDITOR_PADDING;
        editorY = 30;
        editorW = w - EDITOR_PADDING * 2;
        editorH = h - 90;

        // file name field
        fileNameField = new TextFieldWidget(
            this.textRenderer, w / 2 - 100, h - 55, 200, 18, Text.literal("filename"));
        fileNameField.setText("my_script.java");
        fileNameField.setMaxLength(64);
        addDrawableChild(fileNameField);

        // Save button
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> saveScript())
            .dimensions(w / 2 - 160, h - 30, 70, 20).build());

        // Load button
        addDrawableChild(ButtonWidget.builder(Text.literal("Load"), btn -> loadScript())
            .dimensions(w / 2 - 85, h - 30, 70, 20).build());

        // Run button
        addDrawableChild(ButtonWidget.builder(Text.literal("▶ Run"), btn -> runScript())
            .dimensions(w / 2 - 5, h - 30, 70, 20).build());

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
            .dimensions(w / 2 + 70, h - 30, 70, 20).build());
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // dark background
        context.fill(0, 0, this.width, this.height, 0xE0101018);

        // title
        context.drawTextWithShadow(this.textRenderer,
            Text.literal("§b§lScript Editor §7- §ffoure.dev"), 10, 8, 0xFFFFFF);

        // editor background
        context.fill(editorX, editorY, editorX + editorW, editorY + editorH, 0xFF1A1A2E);
        context.fill(editorX, editorY, editorX + editorW, editorY + 1, 0xFF7B2FBE); // top border
        context.fill(editorX, editorY + editorH - 1, editorX + editorW, editorY + editorH, 0xFF7B2FBE); // bottom border
        context.fill(editorX, editorY, editorX + 1, editorY + editorH, 0xFF7B2FBE); // left border
        context.fill(editorX + editorW - 1, editorY, editorX + editorW, editorY + editorH, 0xFF7B2FBE); // right border

        // line numbers gutter
        int gutterW = 30;
        context.fill(editorX + 1, editorY + 1, editorX + gutterW, editorY + editorH - 1, 0xFF141420);

        // render visible lines
        int visibleLines = (editorH - 6) / LINE_HEIGHT;
        int textStartX = editorX + gutterW + 4;
        int textStartY = editorY + 4;

        for (int i = 0; i < visibleLines; i++) {
            int lineIdx = scrollLine + i;
            int renderY = textStartY + i * LINE_HEIGHT;

            // line number
            String lineNum = String.valueOf(lineIdx + 1);
            context.drawTextWithShadow(this.textRenderer,
                Text.literal("§7" + lineNum),
                editorX + gutterW - 4 - this.textRenderer.getWidth(lineNum),
                renderY, 0x888888);

            if (lineIdx >= lines.size()) continue;

            String lineText = lines.get(lineIdx).toString();

            // highlight current line
            if (lineIdx == cursorLine) {
                context.fill(textStartX - 2, renderY - 1,
                    editorX + editorW - 2, renderY + LINE_HEIGHT - 1, 0x22FFFFFF);
            }

            // syntax highlight + render
            renderHighlightedLine(context, lineText, textStartX, renderY);

            // cursor
            if (editorFocused && lineIdx == cursorLine) {
                int col = Math.min(cursorCol, lineText.length());
                int cursorX = textStartX + this.textRenderer.getWidth(lineText.substring(0, col));
                long time = System.currentTimeMillis();
                if ((time / 500) % 2 == 0) {
                    context.fill(cursorX, renderY - 1, cursorX + 1, renderY + LINE_HEIGHT - 1, 0xFFCC88FF);
                }
            }
        }

        // status message
        if (System.currentTimeMillis() - statusTime < 4000 && !statusMessage.isEmpty()) {
            int color = statusError ? 0xFFFF5555 : 0xFF55FF55;
            context.drawTextWithShadow(this.textRenderer,
                Text.literal(statusMessage), editorX, editorY + editorH + 4, color);
        }

        // file name label
        context.drawTextWithShadow(this.textRenderer,
            Text.literal("§7File: "), this.width / 2 - 130, this.height - 50, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHighlightedLine(DrawContext context, String text, int x, int y) {
        // simple keyword highlighting
        String[] keywords = {"public", "private", "static", "void", "int", "boolean",
            "String", "float", "double", "return", "if", "else", "for", "while",
            "new", "class", "import", "try", "catch", "null", "true", "false"};

        // for simplicity, render whole line with basic color
        // green for comments
        String trimmed = text.trim();
        if (trimmed.startsWith("//")) {
            context.drawTextWithShadow(this.textRenderer,
                Text.literal("§2" + text), x, y, 0x55FF55);
            return;
        }

        // check if line has keyword
        String colored = text;
        // render normally but check for string literals
        if (text.contains("\"")) {
            context.drawTextWithShadow(this.textRenderer,
                Text.literal("§f" + text), x, y, 0xFFFFFF);
        } else {
            boolean hasKeyword = false;
            for (String kw : keywords) {
                if (text.contains(kw)) { hasKeyword = true; break; }
            }
            int color = hasKeyword ? 0xCC88FF : 0xDDDDDD;
            context.drawTextWithShadow(this.textRenderer,
                Text.literal(text), x, y, color);
        }
    }

    // ── Input handling ────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editorFocused) return super.keyPressed(keyCode, scanCode, modifiers);

        boolean ctrl = (modifiers & 2) != 0; // GLFW_MOD_CONTROL

        switch (keyCode) {
            case 256: // ESC
                close(); return true;
            case 257: case 335: // ENTER / NUMPAD ENTER
                insertNewLine(); return true;
            case 259: // BACKSPACE
                backspace(); return true;
            case 261: // DELETE
                deleteChar(); return true;
            case 264: // DOWN
                if (cursorLine < lines.size() - 1) {
                    cursorLine++;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                    ensureVisible();
                } return true;
            case 265: // UP
                if (cursorLine > 0) {
                    cursorLine--;
                    cursorCol = Math.min(cursorCol, lines.get(cursorLine).length());
                    ensureVisible();
                } return true;
            case 263: // LEFT
                if (cursorCol > 0) cursorCol--;
                else if (cursorLine > 0) { cursorLine--; cursorCol = lines.get(cursorLine).length(); ensureVisible(); }
                return true;
            case 262: // RIGHT
                if (cursorCol < lines.get(cursorLine).length()) cursorCol++;
                else if (cursorLine < lines.size() - 1) { cursorLine++; cursorCol = 0; ensureVisible(); }
                return true;
            case 268: // HOME
                cursorCol = 0; return true;
            case 269: // END
                cursorCol = lines.get(cursorLine).length(); return true;
        }

        if (ctrl) {
            if (keyCode == 83) { saveScript(); return true; } // Ctrl+S
            if (keyCode == 82) { runScript();  return true; } // Ctrl+R
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!editorFocused) return super.charTyped(chr, modifiers);
        StringBuilder line = lines.get(cursorLine);
        line.insert(cursorCol, chr);
        cursorCol++;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        editorFocused = mouseX >= editorX && mouseX <= editorX + editorW
            && mouseY >= editorY && mouseY <= editorY + editorH;

        if (editorFocused) {
            int gutterW = 30;
            int textStartX = editorX + gutterW + 4;
            int textStartY = editorY + 4;
            int clickedLine = scrollLine + (int)((mouseY - textStartY) / LINE_HEIGHT);
            clickedLine = Math.max(0, Math.min(clickedLine, lines.size() - 1));
            cursorLine = clickedLine;
            // approximate column from x
            String lineText = lines.get(cursorLine).toString();
            int relX = (int)(mouseX - textStartX);
            cursorCol = 0;
            for (int i = 0; i <= lineText.length(); i++) {
                if (this.textRenderer.getWidth(lineText.substring(0, i)) >= relX) {
                    cursorCol = i; break;
                }
                if (i == lineText.length()) cursorCol = i;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= editorX && mouseX <= editorX + editorW
                && mouseY >= editorY && mouseY <= editorY + editorH) {
            scrollLine = Math.max(0, Math.min(scrollLine - (int) verticalAmount, lines.size() - 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ── Editor operations ─────────────────────────────────────────────────

    private void insertNewLine() {
        StringBuilder current = lines.get(cursorLine);
        String after = current.substring(cursorCol);
        current.delete(cursorCol, current.length());
        cursorLine++;
        lines.add(cursorLine, new StringBuilder(after));
        cursorCol = 0;
        ensureVisible();
    }

    private void backspace() {
        if (cursorCol > 0) {
            lines.get(cursorLine).deleteCharAt(cursorCol - 1);
            cursorCol--;
        } else if (cursorLine > 0) {
            StringBuilder removed = lines.remove(cursorLine);
            cursorLine--;
            cursorCol = lines.get(cursorLine).length();
            lines.get(cursorLine).append(removed);
            ensureVisible();
        }
    }

    private void deleteChar() {
        StringBuilder line = lines.get(cursorLine);
        if (cursorCol < line.length()) {
            line.deleteCharAt(cursorCol);
        } else if (cursorLine < lines.size() - 1) {
            StringBuilder next = lines.remove(cursorLine + 1);
            line.append(next);
        }
    }

    private void ensureVisible() {
        int visibleLines = (editorH - 6) / LINE_HEIGHT;
        if (cursorLine < scrollLine) scrollLine = cursorLine;
        if (cursorLine >= scrollLine + visibleLines) scrollLine = cursorLine - visibleLines + 1;
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

    // ── Script operations ─────────────────────────────────────────────────

    private void saveScript() {
        try {
            Files.createDirectories(SCRIPTS_DIR);
            String name = fileNameField.getText().trim();
            if (name.isEmpty()) name = "script.java";
            if (!name.endsWith(".java")) name += ".java";
            Files.writeString(SCRIPTS_DIR.resolve(name), getFullText());
            // also save as last
            Files.writeString(SCRIPTS_DIR.resolve("last_script.java"), getFullText());
            setStatus("§aSaved: " + name, false);
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
                setStatus("§cFile not found: " + name, true);
            }
        } catch (Exception e) {
            setStatus("§cLoad error: " + e.getMessage(), true);
        }
    }

    private void loadFromFile(String name) {
        try {
            Path p = SCRIPTS_DIR.resolve(name);
            if (Files.exists(p)) setFullText(Files.readString(p));
        } catch (Exception ignored) {}
    }

    private void runScript() {
        String code = getFullText();
        setStatus("§eCompiling...", false);

        new Thread(() -> {
            CompileResult result = ScriptCompiler.compile(code);
            MinecraftClient.getInstance().execute(() -> {
                if (result.success) {
                    module.setCompiledScript(result.runnable);
                    module.setEnabled(true);
                    setStatus("§aCompiled! Module activated.", false);
                } else {
                    setStatus("§cError: " + result.error.replace("\n", " | "), true);
                }
            });
        }, "ScriptCompiler").start();
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

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}

