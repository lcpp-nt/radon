package me.itzsomebody.radon.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import me.itzsomebody.radon.Radon;
import me.itzsomebody.radon.config.Config;
import me.itzsomebody.radon.transformers.AbstractTransformer;
import me.itzsomebody.radon.transformers.misc.Expiry;
import me.itzsomebody.radon.transformers.misc.TrashClasses;
import me.itzsomebody.radon.transformers.renamer.Renamer;
import me.itzsomebody.radon.utils.FileUtils;
import me.itzsomebody.radon.utils.LoggerUtils;
import me.itzsomebody.radon.utils.NumberUtils;
import me.itzsomebody.radon.utils.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * Bootstraps and runs the obfuscation process.
 *
 * @author ItzSomebody
 */
public class Bootstrap { // Eyyy bootstrap bill
    /**
     * HashMap that stores all of the loaded classes.
     */
    private Map<String, ClassNode> classPath = new HashMap<>();

    /**
     * HashMap that stores all of the input classes (and as they are obfuscated).
     */
    private Map<String, ClassNode> classes = new HashMap<>();

    /**
     * Extra classes that are generated by the obfuscator.
     */
    private Map<String, ClassNode> extraClasses = new HashMap<>();

    /**
     * Resources which "pass through" the obfuscator.
     */
    private Map<String, byte[]> passThru = new HashMap<>();

    /**
     * Config object
     */
    private Config config;

    /**
     * Input file that read from.
     */
    private File input;

    /**
     * Output file that will be the obfuscation result.
     */
    private File output;

    /**
     * A {@link ZipOutputStream} which we use to write changes to the classes.
     */
    private ZipOutputStream zos;

    /**
     * A HashMap that stores the locations to each of the libraries that will
     * be loaded into the classpath.
     */
    private HashMap<String, File> libs;

    /**
     * Exempt information.
     */
    private List<String> exempts;

    /**
     * Transformers that will be used.
     */
    private List<AbstractTransformer> transformers;

    /**
     * Dictionary type to use
     */
    private int dictionary;

    /**
     * Integer that determines how many trash classes to generate if any at all.
     */
    private int trashClasses;

    /**
     * String to be watermarked into the output if any at all.
     */
    private String watermarkMsg;

    /**
     * Integer that determines which watermarking technique to use if any at all.
     */
    private int watermarkType;

    /**
     * String to encrypt {@link Bootstrap#watermarkMsg} if any at all.
     */
    private String watermarkKey;

    /**
     * Strings to write to log.
     */
    private List<String> logStrings;

    /**
     * {@link Long} used for entry times.
     */
    private long currentTime;

    /**
     * Constructor used for CLI to create a {@link Bootstrap} object.
     *
     * @param config {@link Config} object.
     */
    public Bootstrap(Config config) {
        this.config = config;
    }

    /**
     * Constructor used for MainGUI to create a {@link Bootstrap} object.
     *
     * @param input         the input {@link File}.
     * @param output        the output {@link File}.
     * @param libs          the {@link HashMap} of libraries.
     * @param exempts       exempt information.
     * @param transformers  transformers that will be run.
     * @param trashClasses  number of trash classes to generate as
     *                      {@link Integer}.
     * @param watermarkMsg  {@link String} to watermark into the output.
     * @param watermarkType watermark type as {@link Integer}.
     * @param watermarkKey  {@link String} to encrypt watermark message.
     * @param dictionary    dictionary type used for string generation.
     */
    public Bootstrap(
            File input,
            File output,
            HashMap<String, File> libs,
            List<String> exempts,
            List<AbstractTransformer> transformers,
            int trashClasses,
            String watermarkMsg,
            int watermarkType,
            String watermarkKey,
            int dictionary) {
        this.input = input;
        this.output = output;
        this.libs = libs;
        this.exempts = exempts;
        this.transformers = transformers;
        this.trashClasses = trashClasses;
        this.watermarkMsg = watermarkMsg;
        this.watermarkType = watermarkType;
        this.watermarkKey = watermarkKey;
        this.dictionary = dictionary;
    }

    /**
     * Actual obfuscation starts here.
     *
     * @param doInit should obfuscator read and set variables according to
     *               values of {@link Bootstrap#config}?
     * @throws Throwable if any errors are thrown.
     */
    public void startTheParty(boolean doInit) throws Throwable {
        try {
            this.logStrings = new ArrayList<>();
            if (doInit) {
                this.init();
                this.logStrings.add(LoggerUtils.stdOut("Successfully parsed " +
                        "config"));
            } else {
                if (output.exists()) {
                    logStrings.add(LoggerUtils.stdOut("Output already exists, renamed to "
                            + FileUtils.renameExistingFile(output)));
                }
                this.zos = new ZipOutputStream(new FileOutputStream(output));
            }
            this.currentTime = System.currentTimeMillis();
            this.loadClassPath();
            this.loadInput();

            for (AbstractTransformer transformer : this.transformers) {
                if (transformer instanceof Renamer) {
                    transformer.init(this.classes, this.classPath, this.passThru, this.exempts, this.dictionary);
                } else {
                    transformer.init(this.classes, this.extraClasses, this.exempts, this.dictionary);
                }
                transformer.obfuscate();
                this.logStrings.addAll(transformer.getLogStrings());
            }

            if (this.trashClasses != -1) {
                this.logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
                for (int i = 0; i < this.trashClasses; i++) {
                    TrashClasses trashClass =
                            new TrashClasses(StringUtils.randomClassName(this.classes.keySet(), this.dictionary));
                    ClassNode classNode = trashClass.returnTrashClass();
                    this.extraClasses.put(classNode.name, classNode);
                }
                this.logStrings.add(LoggerUtils.stdOut("Generated "
                        + String.valueOf(this.trashClasses) + " trash classes"));
            }

            if (this.extraClasses.values().size() != 0) {
                this.logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
                this.logStrings.add(LoggerUtils.stdOut("Writing generated classes to output"));

                // Write the contents of extraClasses to zos
                for (ClassNode classNode : this.extraClasses.values()) {
                    ClassWriter cw = new ClassWriter(0);
                    classNode.accept(cw);

                    ZipEntry newEntry = new ZipEntry(classNode.name + ".class");
                    newEntry.setTime(this.currentTime);
                    newEntry.setCompressedSize(-1);
                    this.zos.putNextEntry(newEntry);
                    this.zos.write(cw.toByteArray());
                    this.zos.closeEntry();
                }
            }

            // Write the contents of classes to zos and recompute maxlocals, maxstack and stackframes
            this.logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
            this.logStrings.add(LoggerUtils.stdOut("Writing classes to output"));
            for (ClassNode classNode : this.classes.values()) {
                ClassWriter cw;

                if (classNode.version > Opcodes.V1_5) {
                    cw = new CustomClassWriter(ClassWriter.COMPUTE_FRAMES);
                } else {
                    cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                }

                try {
                    classNode.accept(cw);
                    if (this.watermarkMsg != null) {
                        if (this.watermarkType == 0
                                && NumberUtils.getRandomInt(10) >= 5) {
                            cw.newUTF8("WMID: "
                                    + StringUtils.aesEncrypt(this.watermarkMsg, this.watermarkKey));

                            this.logStrings.add(LoggerUtils.stdOut("Watermarking "
                                    + this.watermarkMsg + " into " + classNode.name));
                        } else if (this.watermarkType == 1
                                && NumberUtils.getRandomInt(10) >= 5) {
                            classNode.signature =
                                    StringUtils.aesEncrypt("WMID: " + this.watermarkMsg,
                                            this.watermarkKey);

                            this.logStrings.add(LoggerUtils.stdOut("Watermarking "
                                    + this.watermarkMsg + " into " + classNode.name));
                        }
                    }

                    cw.newUTF8("RADON" + Radon.VERSION); // :D
                } catch (Throwable t) {
                    this.logStrings.add(LoggerUtils
                            .stdOut("Error while writing "
                                    + classNode.name + " -> " + t.getMessage()));
                    throw t;
                }

                ZipEntry newEntry = new ZipEntry(classNode.name
                        + ".class");
                newEntry.setTime(this.currentTime);
                newEntry.setCompressedSize(-1);
                this.zos.putNextEntry(newEntry);
                this.zos.write(cw.toByteArray());
                this.zos.closeEntry();
            }

            // Write resources to output
            this.logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
            this.logStrings.add(LoggerUtils.stdOut("Writing resources to output"));
            for (String name : this.passThru.keySet()) {
                ZipEntry newEntry = new ZipEntry(name);
                newEntry.setTime(this.currentTime);
                this.zos.putNextEntry(newEntry);
                this.zos.write(this.passThru.get(name));
                this.zos.closeEntry();
            }

            this.logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
            if (this.zos != null) {
                this.zos.setComment("Obfuscation by Radon obfuscator developed by ItzSomebody"); // Cause why not xD
                this.zos.close();
                this.logStrings.add(LoggerUtils.stdOut("Finished processing file."));
            }
        } catch (Throwable t) {
            this.logStrings.add(LoggerUtils.stdOut("Error happened while processing: "
                    + t.getMessage()));

            if (zos != null) {
                zos.close();
            }
            if (this.output.delete()) {
                this.logStrings.add(LoggerUtils.stdOut("Deleted output."));
            } else {
                this.logStrings.add(LoggerUtils.stdOut("Unable to delete faulty output."));
            }

            t.printStackTrace();
            throw new RuntimeException(t.getMessage());
        } finally {
            this.logStrings.add(LoggerUtils.stdOut("Writing log."));
            LoggerUtils.logWriter(this.logStrings);
        }
    }

    /**
     * Nice big init method that loads details from the configuration file.
     *
     * @throws RuntimeException if some error pops up while parsing values
     *                          from {@link Bootstrap#config}.
     */
    private void init() throws RuntimeException {
        try {
            this.config.loadIntoMap();
            this.config.sortExempts();
            this.config.checkConfig();
            this.exempts = this.config.getExempts();
            this.input = this.config.getInput();
            this.output = this.config.getOutput();
            this.libs = this.config.getLibraries();
            this.transformers = new ArrayList<>();
            AbstractTransformer transformer;
            // Specific order of adding transformers, feel free to change if
            // you wish.
            if ((transformer = this.config.getRenamerType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getInnerClassRemoverType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getNumberObfuscationType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getInvokeDynamicType()) != null) {
                this.transformers.add(transformer);
            }
            String expireMsg;
            long expireTime;
            if ((expireMsg = this.config.getExpiryMsg()) != null
                    && (expireTime = this.config.getExpiryTime()) != -1) {
                transformer = new Expiry(expireTime, expireMsg);
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getStringEncryptionType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getFlowObfuscationType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getStringPoolType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getShufflerType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getLocalVariableObfuscationType())
                    != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getLineNumberObfuscationType()) !=
                    null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getSourceNameObfuscationType()) !=
                    null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getSourceDebugObfuscationType())
                    != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getCrasherType()) != null) {
                this.transformers.add(transformer);
            }
            if ((transformer = this.config.getHideCodeType()) != null) {
                this.transformers.add(transformer);
            }
            this.trashClasses = this.config.getTrashClasses();
            this.watermarkMsg = this.config.getWatermarkMsg();
            this.watermarkType = this.config.getWatermarkType();
            this.watermarkKey = this.config.getWatermarkKey();
            this.dictionary = this.config.getDictionaryType();
            if (this.output.exists()) {
                this.logStrings.add(LoggerUtils.stdOut("Output already exists, renamed to "
                        + FileUtils.renameExistingFile(this.output)));
            }
            this.zos = new ZipOutputStream(new FileOutputStream(this.output));
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error while loading config: "
                    + t.getMessage());
        }
    }

    /**
     * Loads library classes into classpath.
     *
     * @throws RuntimeException if library cannot be be opened as Zip or some
     *                          IOE happens.
     */
    private void loadClassPath() throws RuntimeException {
        ZipFile zipFile;
        Enumeration<? extends ZipEntry> entries;
        ZipEntry zipEntry;
        for (File lib : this.libs.values()) {
            try {
                this.logStrings.add(LoggerUtils.stdOut("Loading library "
                        + lib.getAbsolutePath()));
                zipFile = new ZipFile(lib);
                entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    zipEntry = entries.nextElement();
                    if (zipEntry.getName().endsWith(".class") && !zipEntry.isDirectory()) {
                        ClassReader cr = new ClassReader(zipFile
                                .getInputStream(zipEntry));
                        ClassNode classNode = new ClassNode();
                        classNode.libraryNode = true;

                        // We don't need code in methods for libs
                        cr.accept(classNode, ClassReader.SKIP_DEBUG |
                                ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);


                        this.classPath.put(classNode.name, classNode);
                    }
                }
                zipFile.close();
            } catch (ZipException ze) {
                throw new RuntimeException("There was an error opening "
                        + lib.getAbsolutePath() + " as a zip!");
            } catch (IOException ioe) {
                throw new RuntimeException("Library " + lib.getAbsolutePath()
                        + " does not exist!");
            }
        }
    }

    /**
     * Loads input JAR classes and adds them to {@link Bootstrap#extraClasses}
     * and {@link Bootstrap#classes}.
     *
     * @throws RuntimeException if input cannot be be opened as Zip or some
     *                          IOE happens.
     */
    private void loadInput() throws RuntimeException {
        ZipFile zipFile;
        Enumeration<? extends ZipEntry> entries;
        ZipEntry zipEntry;
        try {
            this.logStrings.add(LoggerUtils.stdOut("Loading classes of "
                    + this.input.getAbsolutePath()));
            zipFile = new ZipFile(this.input);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                zipEntry = entries.nextElement();
                if (!zipEntry.isDirectory()) {
                    if (zipEntry.getName().endsWith(".class")) {
                        ClassReader cr = new ClassReader(zipFile
                                .getInputStream(zipEntry));
                        ClassNode classNode = new ClassNode();
                        classNode.libraryNode = false;

                        // We will manually compute stack frames later
                        cr.accept(classNode, ClassReader.SKIP_FRAMES);

                        this.classes.put(classNode.name, classNode);
                    } else {
                        this.passThru.put(zipEntry.getName(), FileUtils.toByteArray(zipFile.getInputStream(zipEntry)));
                    }
                }
            }
            zipFile.close();
        } catch (ZipException ze) {
            throw new RuntimeException("There was an error opening "
                    + this.input.getAbsolutePath() + " as a zip!");
        } catch (IOException ioe) {
            throw new RuntimeException("Input "
                    + this.input.getAbsolutePath() + " does not exist!");
        }

        this.classPath.putAll(this.classes);
    }

    /**
     * CustomClassWriter that doesn't use the internal Java classpath.
     *
     * @author ItzSomebody
     */
    class CustomClassWriter extends ClassWriter {
        private CustomClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(final String type1,
                                             final String type2) {
            if (type1.equals("java/lang/Object")
                    || type2.equals("java/lang/Object")) {
                return "java/lang/Object";
            }

            return deriveCommonSuperName(type1, type2);
        }

        /**
         * Attempts to find the common superclass.
         *
         * @param type1 first class name to lookup.
         * @param type2 second class name to lookup.
         * @return the common superclass.
         */
        private String deriveCommonSuperName(String type1, String type2) {
            ClassNode first = returnClazz(type1);
            ClassNode second = returnClazz(type2);

            if (isAssignableFrom(first, second)) {
                return type1;
            }

            if (isAssignableFrom(second, first)) {
                return type2;
            }

            if ((first.access & Opcodes.ACC_INTERFACE) == 0
                    || (second.access & Opcodes.ACC_INTERFACE) == 0) {
                return "java/lang/Object";
            } else {
                do {
                    first = returnClazz(first.superName);
                } while (!isAssignableFrom(first, second));
                return first.name;
            }
        }

        /**
         * Returns the {@link ClassNode} object from {@link Bootstrap#classPath}
         * if it exists.
         *
         * @param ref class name to fetch from {@link Bootstrap#classPath}.
         * @return the {@link ClassNode} object from {@link Bootstrap#classPath}
         * if it exists.
         */
        private ClassNode returnClazz(String ref) {
            ClassNode clazz = classPath.get(ref);
            if (clazz == null) {
                throw new RuntimeException(ref
                        + " does not exist in classpath!");
            }
            return clazz;
        }

        /**
         * Returns true/false based on if clazz1 is the superclass of clazz2.
         *
         * @param clazz1 possible superclass.
         * @param clazz2 class to check if assignable from clazz1.
         * @return true/false based on if clazz1 is the superclass of clazz2.
         */
        private boolean isAssignableFrom(ClassNode clazz1, ClassNode clazz2) {
            return (clazz1.name.equals("java/lang/Object")
                    || clazz1.superName.equals(clazz2.name));
        }
    }
}
