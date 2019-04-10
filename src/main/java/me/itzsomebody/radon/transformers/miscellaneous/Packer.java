/*
 * Radon - An open-source Java obfuscator
 * Copyright (C) 2019 ItzSomebody
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.itzsomebody.radon.transformers.miscellaneous;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Manifest;
import me.itzsomebody.radon.Logger;
import me.itzsomebody.radon.asm.ClassWrapper;
import me.itzsomebody.radon.config.ConfigurationSetting;
import me.itzsomebody.radon.exceptions.InvalidConfigurationValueException;
import me.itzsomebody.radon.exceptions.RadonException;
import me.itzsomebody.radon.exclusions.ExclusionType;
import me.itzsomebody.radon.transformers.Transformer;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * Packs classes and resources into a stub file which is unpacked on runtime.
 * This is a mega-dumb transformer and is also broken. :omega_lul:
 *
 * @author ItzSomebody.
 */
public class Packer extends Transformer {
    private String mainClass;

    @Override
    public void transform() {
        MemberNames memberNames = new MemberNames();
        AtomicInteger counter = new AtomicInteger();

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            out.writeShort(getClassWrappers().size());

            ArrayList<String> toRemove = new ArrayList<>();

            getClasses().forEach((name, wrapper) -> {
                if (excluded(wrapper))
                    return;

                try {
                    byte[] bytes = radon.class2Bytes(wrapper);

                    out.writeShort(name.length());
                    for (char c : name.replace('/', '.').toCharArray())
                        out.writeChar(c);

                    out.writeInt(bytes.length);
                    for (byte b : bytes)
                        out.writeByte(b);

                    toRemove.add(name);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new RuntimeException();
                }
            });

            out.writeShort(getResources().size() - 1);
            getResources().forEach((name, bytes) -> {
                if (excluded(name))
                    return;

                try {
                    if ("META-INF/MANIFEST.MF".equals(name)) {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        Manifest manifest = new Manifest(new ByteArrayInputStream(bytes));
                        String mainClass = manifest.getMainAttributes().getValue("Main-Class");
                        if (mainClass == null)
                            throw new RadonException("Could not find OEP");

                        this.mainClass = mainClass;
                        manifest.getMainAttributes().putValue("Main-Class", memberNames.className.replace('/', '.'));
                        manifest.write(os);
                        getResources().put(name, os.toByteArray());
                        return;
                    }

                    out.writeShort(name.length());
                    for (char c : name.toCharArray())
                        out.writeChar(c);

                    out.writeInt(bytes.length);
                    for (byte b : bytes)
                        out.writeByte(b);

                    toRemove.add(name);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new RuntimeException();
                }
            });

            if (mainClass == null)
                throw new RadonException("Could not find OEP");

            toRemove.forEach(s -> {
                getClasses().remove(s);
                getResources().remove(s);
            });

            getResources().put(memberNames.stubName.substring(1), bos.toByteArray());
            ClassNode loader = createPackerEntryPoint(memberNames);
            getClasses().put(loader.name, new ClassWrapper(loader, false));

            counter.addAndGet(toRemove.size());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RadonException(e);
        }

        Logger.stdOut("Packed " + counter.get() + " files");
    }

    @Override
    public ExclusionType getExclusionType() {
        return ExclusionType.PACKER;
    }

    @Override
    public String getName() {
        return "Packer";
    }

    @Override
    public Object getConfiguration() {
        return true;
    }

    @Override
    public void setConfiguration(Map<String, Object> config) {
        // Not needed
    }

    @Override
    public void verifyConfiguration(Map<String, Object> config) {
        throw new InvalidConfigurationValueException(ConfigurationSetting.PACKER + " expects a boolean");
    }

    @SuppressWarnings("Duplicates")
    private ClassNode createPackerEntryPoint(MemberNames memberNames) {
        ClassNode cw = new ClassNode();
        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(49, ACC_PUBLIC + ACC_SUPER, memberNames.className, null, "java/lang/ClassLoader", null);

        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, memberNames.classesFieldName, "Ljava/util/Map;", null, null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, memberNames.resourcesFieldName, "Ljava/util/Map;", null, null);
            fv.visitEnd();
        }
        {
            fv = cw.visitField(ACC_PRIVATE + ACC_STATIC, memberNames.bFieldName, "[B", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "<init>", "()V", false);
            mv.visitLabel(l0);
            mv.visitLdcInsn(Type.getType("L" + memberNames.className + ";"));
            mv.visitLdcInsn(memberNames.stubName);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
            mv.visitMethodInsn(INVOKESTATIC, memberNames.className, memberNames.toByteArrayMethodName, "(Ljava/io/InputStream;)[B", false);
            mv.visitFieldInsn(PUTSTATIC, memberNames.className, memberNames.bFieldName, "[B");
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 1);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readShortMethodName, "(I)I", false);
            mv.visitVarInsn(ISTORE, 2);
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitIincInsn(1, 2);
            Label l7 = new Label();
            mv.visitLabel(l7);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 3);
            Label l8 = new Label();
            mv.visitLabel(l8);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitVarInsn(ILOAD, 2);
            Label l9 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l9);
            Label l10 = new Label();
            mv.visitLabel(l10);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readShortMethodName, "(I)I", false);
            mv.visitVarInsn(ISTORE, 4);
            Label l11 = new Label();
            mv.visitLabel(l11);
            mv.visitIincInsn(1, 2);
            Label l12 = new Label();
            mv.visitLabel(l12);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitIntInsn(NEWARRAY, T_CHAR);
            mv.visitVarInsn(ASTORE, 5);
            Label l13 = new Label();
            mv.visitLabel(l13);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 6);
            Label l14 = new Label();
            mv.visitLabel(l14);
            mv.visitVarInsn(ILOAD, 6);
            mv.visitVarInsn(ILOAD, 4);
            Label l15 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l15);
            Label l16 = new Label();
            mv.visitLabel(l16);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitVarInsn(ILOAD, 6);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readCharMethodName, "(I)C", false);
            mv.visitInsn(CASTORE);
            Label l17 = new Label();
            mv.visitLabel(l17);
            mv.visitIincInsn(1, 2);
            Label l18 = new Label();
            mv.visitLabel(l18);
            mv.visitIincInsn(6, 1);
            mv.visitJumpInsn(GOTO, l14);
            mv.visitLabel(l15);
            mv.visitTypeInsn(NEW, "java/lang/String");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false);
            mv.visitVarInsn(ASTORE, 6);
            Label l19 = new Label();
            mv.visitLabel(l19);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readIntMethodName, "(I)I", false);
            mv.visitVarInsn(ISTORE, 7);
            Label l20 = new Label();
            mv.visitLabel(l20);
            mv.visitVarInsn(ILOAD, 7);
            mv.visitIntInsn(NEWARRAY, T_BYTE);
            mv.visitVarInsn(ASTORE, 8);
            Label l21 = new Label();
            mv.visitLabel(l21);
            mv.visitIincInsn(1, 4);
            Label l22 = new Label();
            mv.visitLabel(l22);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 9);
            Label l23 = new Label();
            mv.visitLabel(l23);
            mv.visitVarInsn(ILOAD, 9);
            mv.visitVarInsn(ILOAD, 7);
            Label l24 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l24);
            Label l25 = new Label();
            mv.visitLabel(l25);
            mv.visitVarInsn(ALOAD, 8);
            mv.visitVarInsn(ILOAD, 9);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitIincInsn(1, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readByteMethodName, "(I)B", false);
            mv.visitInsn(BASTORE);
            Label l26 = new Label();
            mv.visitLabel(l26);
            mv.visitIincInsn(9, 1);
            mv.visitJumpInsn(GOTO, l23);
            mv.visitLabel(l24);
            mv.visitFieldInsn(GETSTATIC, memberNames.className, memberNames.classesFieldName, "Ljava/util/Map;");
            mv.visitVarInsn(ALOAD, 6);
            mv.visitVarInsn(ALOAD, 8);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
            Label l27 = new Label();
            mv.visitLabel(l27);
            mv.visitIincInsn(3, 1);
            mv.visitJumpInsn(GOTO, l8);
            mv.visitLabel(l9);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readShortMethodName, "(I)I", false);
            mv.visitVarInsn(ISTORE, 3);
            Label l28 = new Label();
            mv.visitLabel(l28);
            mv.visitIincInsn(1, 2);
            Label l29 = new Label();
            mv.visitLabel(l29);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 4);
            Label l30 = new Label();
            mv.visitLabel(l30);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitJumpInsn(IF_ICMPGE, l1);
            Label l31 = new Label();
            mv.visitLabel(l31);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readShortMethodName, "(I)I", false);
            mv.visitVarInsn(ISTORE, 5);
            Label l32 = new Label();
            mv.visitLabel(l32);
            mv.visitIincInsn(1, 2);
            Label l33 = new Label();
            mv.visitLabel(l33);
            mv.visitVarInsn(ILOAD, 5);
            mv.visitIntInsn(NEWARRAY, T_CHAR);
            mv.visitVarInsn(ASTORE, 6);
            Label l34 = new Label();
            mv.visitLabel(l34);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 7);
            Label l35 = new Label();
            mv.visitLabel(l35);
            mv.visitVarInsn(ILOAD, 7);
            mv.visitVarInsn(ILOAD, 5);
            Label l36 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l36);
            Label l37 = new Label();
            mv.visitLabel(l37);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitVarInsn(ILOAD, 7);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readCharMethodName, "(I)C", false);
            mv.visitInsn(CASTORE);
            Label l38 = new Label();
            mv.visitLabel(l38);
            mv.visitIincInsn(1, 2);
            Label l39 = new Label();
            mv.visitLabel(l39);
            mv.visitIincInsn(7, 1);
            mv.visitJumpInsn(GOTO, l35);
            mv.visitLabel(l36);
            mv.visitTypeInsn(NEW, "java/lang/String");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 6);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false);
            mv.visitVarInsn(ASTORE, 7);
            Label l40 = new Label();
            mv.visitLabel(l40);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readIntMethodName, "(I)I", false);
            mv.visitVarInsn(ISTORE, 8);
            Label l41 = new Label();
            mv.visitLabel(l41);
            mv.visitVarInsn(ILOAD, 8);
            mv.visitIntInsn(NEWARRAY, T_BYTE);
            mv.visitVarInsn(ASTORE, 9);
            Label l42 = new Label();
            mv.visitLabel(l42);
            mv.visitIincInsn(1, 4);
            Label l43 = new Label();
            mv.visitLabel(l43);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, 10);
            Label l44 = new Label();
            mv.visitLabel(l44);
            mv.visitVarInsn(ILOAD, 10);
            mv.visitVarInsn(ILOAD, 8);
            Label l45 = new Label();
            mv.visitJumpInsn(IF_ICMPGE, l45);
            Label l46 = new Label();
            mv.visitLabel(l46);
            mv.visitVarInsn(ALOAD, 9);
            mv.visitVarInsn(ILOAD, 10);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitIincInsn(1, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readByteMethodName, "(I)B", false);
            mv.visitInsn(BASTORE);
            Label l47 = new Label();
            mv.visitLabel(l47);
            mv.visitIincInsn(10, 1);
            mv.visitJumpInsn(GOTO, l44);
            mv.visitLabel(l45);
            mv.visitFieldInsn(GETSTATIC, memberNames.className, memberNames.resourcesFieldName, "Ljava/util/Map;");
            mv.visitVarInsn(ALOAD, 7);
            mv.visitVarInsn(ALOAD, 9);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
            Label l48 = new Label();
            mv.visitLabel(l48);
            mv.visitIincInsn(4, 1);
            mv.visitJumpInsn(GOTO, l30);
            mv.visitLabel(l1);
            Label l49 = new Label();
            mv.visitJumpInsn(GOTO, l49);
            mv.visitLabel(l2);
            mv.visitVarInsn(ASTORE, 1);
            Label l50 = new Label();
            mv.visitLabel(l50);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
            mv.visitLabel(l49);
            mv.visitInsn(RETURN);
            Label l51 = new Label();
            mv.visitLabel(l51);
            mv.visitMaxs(4, 11);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PROTECTED, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;", "(Ljava/lang/String;)Ljava/lang/Class<*>;", new String[]{"java/lang/ClassNotFoundException"});
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC, memberNames.className, memberNames.classesFieldName, "Ljava/util/Map;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "[B");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, 2);
            Label l1 = new Label();
            mv.visitLabel(l1);
            Label l2 = new Label();
            mv.visitJumpInsn(IFNULL, l2);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ARRAYLENGTH);
            mv.visitMethodInsn(INVOKEVIRTUAL, memberNames.className, "defineClass", "(Ljava/lang/String;[BII)Ljava/lang/Class;", false);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "findClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
            mv.visitInsn(ARETURN);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitMaxs(5, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC, memberNames.className, memberNames.resourcesFieldName, "Ljava/util/Map;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "[B");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, 2);
            Label l1 = new Label();
            mv.visitLabel(l1);
            Label l2 = new Label();
            mv.visitJumpInsn(IFNULL, l2);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitTypeInsn(NEW, "java/io/ByteArrayInputStream");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, "java/io/ByteArrayInputStream", "<init>", "([B)V", false);
            mv.visitInsn(ARETURN);
            mv.visitLabel(l2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
            mv.visitInsn(ARETURN);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE, memberNames.readByteMethodName, "(I)B", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC, memberNames.className, memberNames.bFieldName, "[B");
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(BALOAD);
            mv.visitInsn(IRETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE, memberNames.readCharMethodName, "(I)C", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, memberNames.readShortMethodName, "(I)I", false);
            mv.visitInsn(I2C);
            mv.visitInsn(IRETURN);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE, memberNames.readShortMethodName, "(I)I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC, memberNames.className, memberNames.bFieldName, "[B");
            mv.visitVarInsn(ASTORE, 2);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(BALOAD);
            mv.visitIntInsn(SIPUSH, 255);
            mv.visitInsn(IAND);
            mv.visitIntInsn(BIPUSH, 8);
            mv.visitInsn(ISHL);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitInsn(BALOAD);
            mv.visitIntInsn(SIPUSH, 255);
            mv.visitInsn(IAND);
            mv.visitInsn(IOR);
            mv.visitInsn(IRETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE, memberNames.readIntMethodName, "(I)I", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitFieldInsn(GETSTATIC, memberNames.className, memberNames.bFieldName, "[B");
            mv.visitVarInsn(ASTORE, 2);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(BALOAD);
            mv.visitIntInsn(SIPUSH, 255);
            mv.visitInsn(IAND);
            mv.visitIntInsn(BIPUSH, 24);
            mv.visitInsn(ISHL);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitInsn(BALOAD);
            mv.visitIntInsn(SIPUSH, 255);
            mv.visitInsn(IAND);
            mv.visitIntInsn(BIPUSH, 16);
            mv.visitInsn(ISHL);
            mv.visitInsn(IOR);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_2);
            mv.visitInsn(IADD);
            mv.visitInsn(BALOAD);
            mv.visitIntInsn(SIPUSH, 255);
            mv.visitInsn(IAND);
            mv.visitIntInsn(BIPUSH, 8);
            mv.visitInsn(ISHL);
            mv.visitInsn(IOR);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(ICONST_3);
            mv.visitInsn(IADD);
            mv.visitInsn(BALOAD);
            mv.visitIntInsn(SIPUSH, 255);
            mv.visitInsn(IAND);
            mv.visitInsn(IOR);
            mv.visitInsn(IRETURN);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, memberNames.toByteArrayMethodName, "(Ljava/io/InputStream;)[B", null, new String[]{"java/io/IOException"});
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitTypeInsn(NEW, "java/io/ByteArrayOutputStream");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/io/ByteArrayOutputStream", "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, 1);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitIntInsn(SIPUSH, 1024);
            mv.visitIntInsn(NEWARRAY, T_BYTE);
            mv.visitVarInsn(ASTORE, 2);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "available", "()I", false);
            Label l3 = new Label();
            mv.visitJumpInsn(IFLE, l3);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "read", "([B)I", false);
            mv.visitVarInsn(ISTORE, 3);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ByteArrayOutputStream", "write", "([BII)V", false);
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitJumpInsn(GOTO, l2);
            mv.visitLabel(l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
            Label l7 = new Label();
            mv.visitLabel(l7);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ByteArrayOutputStream", "close", "()V", false);
            Label l8 = new Label();
            mv.visitLabel(l8);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/ByteArrayOutputStream", "toByteArray", "()[B", false);
            mv.visitInsn(ARETURN);
            Label l9 = new Label();
            mv.visitLabel(l9);
            mv.visitMaxs(4, 4);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, new String[]{"java/lang/Throwable"});
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitTypeInsn(NEW, memberNames.className);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, memberNames.className, "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, 1);
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(mainClass);
            mv.visitMethodInsn(INVOKEVIRTUAL, memberNames.className, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
            mv.visitVarInsn(ASTORE, 2);
            Label l2 = new Label();
            mv.visitLabel(l2);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn("main");
            mv.visitInsn(ICONST_1);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitLdcInsn(Type.getType("[Ljava/lang/String;"));
            mv.visitInsn(AASTORE);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
            mv.visitVarInsn(ASTORE, 3);
            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitInsn(ICONST_1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V", false);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ICONST_1);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(AASTORE);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitInsn(RETURN);
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitMaxs(6, 4);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            Label l0 = new Label();
            mv.visitLabel(l0);
            mv.visitTypeInsn(NEW, "java/util/HashMap");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            mv.visitFieldInsn(PUTSTATIC, memberNames.className, memberNames.classesFieldName, "Ljava/util/Map;");
            Label l1 = new Label();
            mv.visitLabel(l1);
            mv.visitTypeInsn(NEW, "java/util/HashMap");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            mv.visitFieldInsn(PUTSTATIC, memberNames.className, memberNames.resourcesFieldName, "Ljava/util/Map;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw;
    }

    private class MemberNames {
        private String className = randomString();
        private String classesFieldName = randomString();
        private String resourcesFieldName = randomString();
        private String bFieldName = randomString();
        private String readByteMethodName = randomString();
        private String readCharMethodName = randomString();
        private String readShortMethodName = randomString();
        private String readIntMethodName = randomString();
        private String toByteArrayMethodName = randomString();
        private String stubName = '/' + randomString();
    }
}
