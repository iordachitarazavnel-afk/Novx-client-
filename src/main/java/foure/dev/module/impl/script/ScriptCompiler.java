package foure.dev.module.impl.script;

import foure.dev.util.Script.scripts.Script;
import foure.dev.util.Script.scripts.ScriptAction;
import net.minecraft.client.MinecraftClient;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

public class ScriptCompiler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static class CompileResult {
        public final boolean success;
        public final String  error;
        public final Runnable runnable;

        public CompileResult(boolean success, String error, Runnable runnable) {
            this.success  = success;
            this.error    = error;
            this.runnable = runnable;
        }
    }

    /**
     * Compiles the given Java source code and returns a CompileResult.
     * The code must contain a public class named "UserScript" with a
     * public static void run() method.
     *
     * Example valid script:
     *   public class UserScript {
     *       public static void run() {
     *           // your code here
     *       }
     *   }
     */
    public static CompileResult compile(String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompileResult(false,
                "Java compiler not available. Make sure you are running a JDK, not JRE.", null);
        }

        // wrap code if user didn't write a class
        String fullSource = sourceCode;
        if (!sourceCode.contains("class UserScript")) {
            fullSource = "import net.minecraft.client.MinecraftClient;\n"
                + "public class UserScript {\n"
                + "    static final MinecraftClient mc = MinecraftClient.getInstance();\n"
                + "    public static void run() throws Exception {\n"
                + sourceCode + "\n"
                + "    }\n"
                + "}\n";
        }

        try {
            // write to temp file
            Path tmpDir = Files.createTempDirectory("foure_script_");
            Path srcFile = tmpDir.resolve("UserScript.java");
            Files.writeString(srcFile, fullSource);

            // compile
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

            // add current classpath
            String classpath = System.getProperty("java.class.path");
            List<String> options = Arrays.asList("-classpath", classpath, "-d", tmpDir.toString());

            Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjects(srcFile.toFile());

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, compilationUnits);

            boolean success = task.call();
            fileManager.close();

            if (!success) {
                StringBuilder sb = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        sb.append("Line ").append(d.getLineNumber())
                          .append(": ").append(d.getMessage(null)).append("\n");
                    }
                }
                // cleanup
                deleteDir(tmpDir);
                return new CompileResult(false, sb.toString().trim(), null);
            }

            // load compiled class
            URLClassLoader classLoader = new URLClassLoader(
                new URL[]{tmpDir.toUri().toURL()},
                ScriptCompiler.class.getClassLoader());

            Class<?> clazz = classLoader.loadClass("UserScript");
            Method runMethod = clazz.getMethod("run");

            Runnable runnable = () -> {
                try {
                    runMethod.invoke(null);
                } catch (Exception e) {
                    if (mc.player != null) {
                        mc.player.sendMessage(
                            net.minecraft.text.Text.literal("§c[ScriptEditor] Runtime error: " + e.getCause().getMessage()), false);
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

