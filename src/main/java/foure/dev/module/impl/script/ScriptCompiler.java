package foure.dev.module.impl.script;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

public class ScriptCompiler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static class CompileResult {
        public final boolean  success;
        public final String   error;
        public final Runnable runnable;

        public CompileResult(boolean success, String error, Runnable runnable) {
            this.success  = success;
            this.error    = error;
            this.runnable = runnable;
        }
    }

    /**
     * Compiles sourceCode at runtime.
     * The code can be just the body of a run() method — it will be wrapped automatically.
     * Or it can contain a full "public class UserScript { public static void run() { ... } }"
     */
    public static CompileResult compile(String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompileResult(false,
                "Java compiler not available (need JDK, not JRE).", null);
        }

        String fullSource;
        if (sourceCode.contains("class UserScript")) {
            fullSource = sourceCode;
        } else {
            fullSource =
                "import net.minecraft.client.MinecraftClient;\n" +
                "import net.minecraft.text.Text;\n" +
                "import net.minecraft.util.math.BlockPos;\n" +
                "public class UserScript {\n" +
                "    static final MinecraftClient mc = MinecraftClient.getInstance();\n" +
                "    public static void run() throws Exception {\n" +
                sourceCode + "\n" +
                "    }\n" +
                "}\n";
        }

        try {
            Path tmpDir  = Files.createTempDirectory("foure_script_");
            Path srcFile = tmpDir.resolve("UserScript.java");
            Files.writeString(srcFile, fullSource);

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, null, null);

            String classpath = System.getProperty("java.class.path");
            List<String> options = Arrays.asList(
                "-classpath", classpath,
                "-d", tmpDir.toString(),
                "-source", "17",
                "-target", "17"
            );

            Iterable<? extends JavaFileObject> units =
                fileManager.getJavaFileObjects(srcFile.toFile());

            boolean success = compiler.getTask(
                null, fileManager, diagnostics, options, null, units).call();
            fileManager.close();

            if (!success) {
                StringBuilder sb = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        // subtract wrapper lines if code was wrapped
                        long line = sourceCode.contains("class UserScript")
                            ? d.getLineNumber()
                            : d.getLineNumber() - 5;
                        sb.append("Line ").append(Math.max(1, line))
                          .append(": ").append(d.getMessage(null)).append("\n");
                    }
                }
                deleteDir(tmpDir);
                return new CompileResult(false, sb.toString().trim(), null);
            }

            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{tmpDir.toUri().toURL()},
                ScriptCompiler.class.getClassLoader());

            Class<?> clazz     = classLoader.loadClass("UserScript");
            Method   runMethod = clazz.getMethod("run");

            Runnable runnable = () -> {
                try {
                    runMethod.invoke(null);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            Text.literal("§c[Script] Runtime error: " + cause.getMessage()), false);
                    }
                }
            };

            return new CompileResult(true, null, runnable);

        } catch (Exception e) {
            return new CompileResult(false, e.getMessage(), null);
        }
    }

    private static void deleteDir(Path dir) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException ignored) {}
    }
}

