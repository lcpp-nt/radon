package me.itzsomebody.radon.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utils for operating on files in general.
 *
 * @author ItzSomebody
 */
public class FileUtils {
    /**
     * Renames an existing file to EXISTINGFILE.jar.BACKUP-X.
     *
     * @param existing existing file to rename.
     * @return the new name of the existing name.
     */
    public static String renameExistingFile(File existing) {
        int i = 0;

        while (true) {
            i++;
            String newName = existing.getAbsolutePath() + ".BACKUP-"
                    + String.valueOf(i);
            File backUpName = new File(newName);
            if (!backUpName.exists()) {
                existing.renameTo(backUpName);
                existing.delete();
                return newName;
            }
        }

    }

    /**
     * Creates a byte array from a given {@link InputStream}.
     *
     * @param in {@link InputStream} to convert to a byte array.
     * @return a byte array from the inputted
     */
    public static byte[] toByteArray(InputStream in) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (in.available() > 0) {
                int data = in.read(buffer);
                out.write(buffer, 0, data);
            }

            in.close();
            out.close();
            return out.toByteArray();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException(ioe.getMessage());
        }
    }
}
