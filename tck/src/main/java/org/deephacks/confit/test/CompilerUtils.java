package org.deephacks.confit.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.google.common.io.Closeables;

/**
 * Compile java source code in config.
 */
public class CompilerUtils {

    /**
     * Compile a set of sources into classes.
     *
     * @param source key: classname, value: source.
     * @param outputDir output dir of compiled classes
     */
    public static Set<Class<?>> compile(Map<String, String> source, String packageName,
            File outputDir) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();
        for (String filename : source.keySet()) {
            File dir = new File(outputDir, packageName.replaceAll("\\.", "\\/"));
            if (!dir.exists()) {
                dir.mkdirs();
            }
            writeFile(source.get(filename), new File(dir, "/" + filename + ".java"));

            compilationUnits.add(new DynamicJavaSourceCodeObject(filename, source.get(filename)));
        }
        CompilationTask task = compiler.getTask(null, null, diagnostics,
                Arrays.asList("-d", outputDir.getAbsolutePath()), null, compilationUnits);

        boolean success = task.call();
        if (!success) {
            StringBuffer sb = new StringBuffer();
            for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                /**
                 * Error messages look terrible and needs cleanup
                 */
                sb.append("Error on line " + diagnostic.getLineNumber() + " in " + diagnostic);
            }
            throw new IllegalArgumentException(sb.toString());
        }
        return loadClasses(source.keySet(), packageName, outputDir);
    }

    private static Set<Class<?>> loadClasses(Collection<String> filenames, String packageName,
            File outputDir) {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        List<String> fullclassname = new ArrayList<String>();
        for (String filename : filenames) {
            fullclassname.add(packageName + "." + filename);
        }
        try {
            URLClassLoader cl = new URLClassLoader(new URL[] { outputDir.toURI().toURL() }, Thread
                    .currentThread().getContextClassLoader());
            for (String c : fullclassname) {
                classes.add(cl.loadClass(c));
            }
            Thread.currentThread().setContextClassLoader(cl);
            return classes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class DynamicJavaSourceCodeObject extends SimpleJavaFileObject {
        private String qualifiedName;
        private String sourceCode;

        /**
         * Converts the name to an URI, as that is the format expected by JavaFileObject
         *
         *
         * @param fully qualified name given to the class file
         * @param code the source code string
         */
        protected DynamicJavaSourceCodeObject(String name, String code) {
            super(URI.create("string:///" + name.replaceAll("\\.", "/") + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.qualifiedName = name;
            this.sourceCode = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return sourceCode;
        }

        public String getQualifiedName() {
            return qualifiedName;
        }

        public void setQualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public void setSourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
        }
    }

    public static void writeFile(String line, File file) {
        writeFile(Arrays.asList(new String[] { line }), file);
    }

    public static void writeFile(String[] lines, File file) {
        writeFile(Arrays.asList(lines), file);
    }

    public static void writeFile(List<String> lines, File file) {
        try {
            File parent = file.getParentFile();
            if ((parent != null) && parent.isDirectory() && !parent.exists()) {
                parent.mkdirs();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unxpected exception when trying to create parent folders for file ["
                            + file.getAbsolutePath() + "].");
        }

        try {
            writeFile(lines, new FileOutputStream(file));
        } catch (IOException e) {
            throw new IllegalArgumentException("File [" + file.getAbsolutePath()
                    + "] cant write to file.", e);
        }

    }

    public static void writeFile(List<String> lines, OutputStream stream) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(stream));
            for (String line : lines) {
                bw.write(line + "\n");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            Closeables.closeQuietly(bw);
        }
    }
}