package com.starwinmod.winlator.core;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

public abstract class TarCompressorUtils {
    public enum Type {XZ, ZSTD, GZIP, BZIP2}

    /** Magic bytes for known compression formats. */
    private static final byte[] MAGIC_XZ   = { (byte)0xFD, '7', 'z', 'X', 'Z', 0x00 };
    private static final byte[] MAGIC_ZSTD = { (byte)0x28, (byte)0xB5, (byte)0x2F, (byte)0xFD };
    private static final byte[] MAGIC_GZIP = { (byte)0x1F, (byte)0x8B, 0x08 };
    private static final byte[] MAGIC_BZIP2= { (byte)0x42, (byte)0x5A, (byte)0x68 };

    // Interface to define the exclusion filter
    public interface ExclusionFilter {
        boolean shouldInclude(File file);
    }


    private static void addFile(ArchiveOutputStream tar, File file, String entryName) {
        try {
            tar.putArchiveEntry(tar.createArchiveEntry(file, entryName));
            try (BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(file), StreamUtils.BUFFER_SIZE)) {
                StreamUtils.copy(inStream, tar);
            }
            tar.closeArchiveEntry();
        }
        catch (Exception e) {}
    }

    private static void addLinkFile(ArchiveOutputStream tar, File file, String entryName) {
        try {
            TarArchiveEntry entry = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
            entry.setLinkName(FileUtils.readSymlink(file));
            tar.putArchiveEntry(entry);
            tar.closeArchiveEntry();
        }
        catch (Exception e) {}
    }

    private static void addDirectory(ArchiveOutputStream tar, File folder, String basePath, ExclusionFilter filter) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (filter != null && !filter.shouldInclude(file)) {
                continue; // Skip files that should be excluded
            }
            if (FileUtils.isSymlink(file)) {
                addLinkFile(tar, file, basePath + file.getName());
            } else if (file.isDirectory()) {
                String entryName = basePath + file.getName() + "/";
                tar.putArchiveEntry(tar.createArchiveEntry(folder, entryName));
                tar.closeArchiveEntry();
                addDirectory(tar, file, entryName, filter);
            } else {
                addFile(tar, file, basePath + file.getName());
            }
        }
    }
    public static void compress(Type type, File file, File destination, int level) {
        compress(type, new File[]{file}, destination, level, null);
    }

    public static void compress(Type type, File file, File destination, int level, ExclusionFilter filter) {
        compress(type, new File[]{file}, destination, level, filter);
    }

    public static void compress(Type type, File[] files, File destination, int level, ExclusionFilter filter) {
        try (OutputStream outStream = getCompressorOutputStream(type, destination, level);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(outStream)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            for (File file : files) {
                if (filter != null && !filter.shouldInclude(file)) {
                    continue; // Skip files that should be excluded
                }
                if (FileUtils.isSymlink(file)) {
                    addLinkFile(tar, file, file.getName());
                } else if (file.isDirectory()) {
                    String basePath = file.getName() + "/";
                    tar.putArchiveEntry(tar.createArchiveEntry(file, basePath));
                    tar.closeArchiveEntry();
                    addDirectory(tar, file, basePath, filter);
                } else {
                    addFile(tar, file, file.getName());
                }
            }
            tar.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Detects compression format by reading magic bytes from a PushbackInputStream.
     * The stream must support mark() at len >= 6.
     * Returns null if no format matched.
     */
    public static Type detectFormat(PushbackInputStream in) throws IOException {
        byte[] header = new byte[6];
        int bytesRead = in.read(header);
        if (bytesRead <= 0) return null;
        if (bytesRead < 6) {
            // Pad with zeros so comparison still works
            byte[] padded = new byte[6];
            System.arraycopy(header, 0, padded, 0, bytesRead);
            header = padded;
        }
        // Unread so the stream is intact for the decompressor
        in.unread(header, 0, bytesRead);

        if (startsWith(header, MAGIC_XZ))   return Type.XZ;
        if (startsWith(header, MAGIC_ZSTD)) return Type.ZSTD;
        if (startsWith(header, MAGIC_GZIP)) return Type.GZIP;
        if (startsWith(header, MAGIC_BZIP2))return Type.BZIP2;
        return null;
    }

    /**
     * Detects compression format of a File by reading its first bytes.
     * Returns null if the file doesn't exist or format is unknown.
     */
    public static Type detectFormat(File source) {
        if (source == null || !source.isFile()) return null;
        try (PushbackInputStream in = new PushbackInputStream(new BufferedInputStream(new FileInputStream(source), StreamUtils.BUFFER_SIZE), 6)) {
            return detectFormat(in);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Detects compression format from a Uri by reading the first bytes of the content stream.
     * Returns null if the format is unknown or stream can't be read.
     */
    public static Type detectFormat(Context context, Uri source) {
        if (source == null) return null;
        try {
            InputStream rawIn;
            if (source.toString().startsWith("/")) {
                rawIn = new FileInputStream(source.toString());
            } else {
                rawIn = context.getContentResolver().openInputStream(source);
            }
            if (rawIn == null) return null;
            try (PushbackInputStream in = new PushbackInputStream(new BufferedInputStream(rawIn, StreamUtils.BUFFER_SIZE), 6)) {
                return detectFormat(in);
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Extracts content from a Uri by auto-detecting the compression format.
     * Tries all known formats as fallback if detection fails or the detected
     * decompressor fails.
     */
    public static boolean extractAuto(Context context, Uri source, File destination) {
        return extractAuto(context, source, destination, null);
    }

    /**
     * Extracts content from a Uri by auto-detecting the compression format.
     * Tries all known formats as fallback if detection fails or the detected
     * decompressor fails.
     */
    public static boolean extractAuto(Context context, Uri source, File destination, OnExtractFileListener listener) {
        if (source == null) return false;
        // First try auto-detection via magic-bytes sniffing
        try {
            InputStream rawIn;
            if (source.toString().startsWith("/")) {
                rawIn = new FileInputStream(source.toString());
            } else {
                rawIn = context.getContentResolver().openInputStream(source);
            }
            if (rawIn == null) return extractFallback(context, source, destination, listener);

            PushbackInputStream pushIn = new PushbackInputStream(
                    new BufferedInputStream(rawIn, StreamUtils.BUFFER_SIZE), 6);
            Type detected;
            try {
                detected = detectFormat(pushIn);
            } catch (IOException e) {
                closeQuietly(pushIn);
                return extractFallback(context, source, destination, listener);
            }

            if (detected != null) {
                // pushIn already has the magic bytes unread, ready for decompressor
                if (extract(detected, pushIn, destination, listener)) {
                    return true;
                }
            }
            closeQuietly(pushIn);
        } catch (IOException e) {
            // stream open failed — fall through to fallback
        }
        // Auto-detection failed or detected format failed — try every format
        return extractFallback(context, source, destination, listener);
    }

    /**
     * Extracts content from a File by auto-detecting the compression format.
     * Tries all known formats as fallback if detection fails.
     */
    public static boolean extractAuto(File source, File destination) {
        return extractAuto(source, destination, null);
    }

    /**
     * Extracts content from a File by auto-detecting the compression format.
     * Tries all known formats as fallback if detection fails.
     */
    public static boolean extractAuto(File source, File destination, OnExtractFileListener listener) {
        if (source == null || !source.isFile()) return false;
        Type detected = detectFormat(source);
        if (detected != null) {
            if (extract(detected, source, destination, listener)) return true;
        }
        // Detection failed or detected format failed — try all
        return extractFallback(source, destination, listener);
    }

    // ----- fallback helpers -----

    private static boolean extractFallback(Context context, Uri source, File destination, OnExtractFileListener listener) {
        for (Type type : Type.values()) {
            if (extract(type, context, source, destination, listener)) return true;
        }
        return false;
    }

    private static boolean extractFallback(File source, File destination, OnExtractFileListener listener) {
        for (Type type : Type.values()) {
            if (extract(type, source, destination, listener)) return true;
        }
        return false;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try { c.close(); } catch (IOException ignored) {}
        }
    }

    public static boolean extract(Type type, Context context, String assetFile, File destination) {
        return extract(type, context, assetFile, destination, null);
    }

    public static boolean extract(Type type, Context context, String assetFile, File destination, OnExtractFileListener onExtractFileListener) {
        try {
            return extract(type, context.getAssets().open(assetFile), destination, onExtractFileListener);
        }
        catch (IOException e) {
            return false;
        }
    }

    public static boolean extract(Type type, Context context, Uri source, File destination) {
        return extract(type, context, source, destination, null);
    }

    public static boolean extract(Type type, Context context, Uri source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null) return false;
        try {
            if (source.toString().startsWith("/")) {
                return extract(type, new FileInputStream(source.toString()), destination, onExtractFileListener);
            } else {
                return extract(type, context.getContentResolver().openInputStream(source), destination, onExtractFileListener);
            }
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    public static boolean extract(Type type, File source, File destination) {
        return extract(type, source, destination, null);
    }

    public static boolean extract(Type type, File source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null || !source.isFile()) return false;
        try {
            return extract(type, new BufferedInputStream(new FileInputStream(source), StreamUtils.BUFFER_SIZE), destination, onExtractFileListener);
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    private static boolean extract(Type type, InputStream source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null) return false;
        try (InputStream inStream = getCompressorInputStream(type, source);
             ArchiveInputStream tar = new TarArchiveInputStream(inStream)) {
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry)tar.getNextEntry()) != null) {
                if (!tar.canReadEntryData(entry)) continue;
                File file = new File(destination, entry.getName());

                if (onExtractFileListener != null) {
                    file = onExtractFileListener.onExtractFile(file, entry.getSize());
                    if (file == null) continue;
                }

                if (entry.isDirectory()) {
                    if (!file.isDirectory()) file.mkdirs();
                }
                else {
                    if (entry.isSymbolicLink()) {
                        FileUtils.symlink(entry.getLinkName(), file.getAbsolutePath());
                    }
                    else {
                        try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file), StreamUtils.BUFFER_SIZE)) {
                            if (!StreamUtils.copy(tar, outStream)) return false;
                        }
                    }
                }

                FileUtils.chmod(file, 0771);
            }
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static InputStream getCompressorInputStream(Type type, InputStream source) throws IOException {
        if (type == Type.XZ) {
            return new XZCompressorInputStream(source);
        }
        else if (type == Type.ZSTD) {
            return new ZstdCompressorInputStream(source);
        }
        else if (type == Type.GZIP) {
            return new GzipCompressorInputStream(source);
        }
        else if (type == Type.BZIP2) {
            return new BZip2CompressorInputStream(source);
        }
        return null;
    }

    private static OutputStream getCompressorOutputStream(Type type, File destination, int level) throws IOException {
        if (type == Type.XZ) {
            return new XZCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE), level);
        }
        else if (type == Type.ZSTD) {
            return new ZstdCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE), level);
        }
        else if (type == Type.GZIP) {
            return new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE));
        }
        else if (type == Type.BZIP2) {
            return new BZip2CompressorOutputStream(new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE));
        }
        return null;
    }

    public static void archive(File[] files, File destination, ExclusionFilter filter) {
        try (OutputStream outStream = new BufferedOutputStream(new FileOutputStream(destination), StreamUtils.BUFFER_SIZE);
             TarArchiveOutputStream tar = new TarArchiveOutputStream(outStream)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            for (File file : files) {
                if (filter != null && !filter.shouldInclude(file)) {
                    continue; // Skip files that should be excluded
                }
                if (FileUtils.isSymlink(file)) {
                    addLinkFile(tar, file, file.getName());
                } else if (file.isDirectory()) {
                    String basePath = file.getName() + "/";
                    tar.putArchiveEntry(tar.createArchiveEntry(file, basePath));
                    tar.closeArchiveEntry();
                    addDirectory(tar, file, basePath, filter);
                } else {
                    addFile(tar, file, file.getName());
                }
            }
            tar.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean extractTar(File source, File destination, OnExtractFileListener onExtractFileListener) {
        if (source == null || !source.isFile()) return false;
        try (InputStream inStream = new BufferedInputStream(new FileInputStream(source), StreamUtils.BUFFER_SIZE);
             TarArchiveInputStream tar = new TarArchiveInputStream(inStream)) {
            TarArchiveEntry entry;
            String topLevelDirectory = null;
            while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                if (!tar.canReadEntryData(entry)) continue;

                // Get the top-level directory name
                String entryName = entry.getName();
                if (topLevelDirectory == null) {
                    if (entry.isDirectory()) {
                        topLevelDirectory = entryName;
                        continue; // Skip creating the top-level directory
                    }
                }

                // Skip the entire tmp directory
                if (entryName.contains("/tmp/")) {
                    Log.d("RestoreOp", "Skipping tmp directory: " + entryName);
                    continue;
                }

                // Adjust the extraction path to remove the top-level directory
                String adjustedName = entryName.replaceFirst("^" + topLevelDirectory, "");
                File file = new File(destination, adjustedName);

                if (onExtractFileListener != null) {
                    file = onExtractFileListener.onExtractFile(file, entry.getSize());
                    if (file == null) continue;
                }

                if (entry.isDirectory()) {
                    if (!file.isDirectory()) file.mkdirs();
                } else {
                    if (entry.isSymbolicLink()) {
                        FileUtils.symlink(entry.getLinkName(), file.getAbsolutePath());
                    } else {
                        try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(file), StreamUtils.BUFFER_SIZE)) {
                            if (!StreamUtils.copy(tar, outStream)) return false;
                        }
                    }
                }

                FileUtils.chmod(file, 0771);
            }
            return true;
        } catch (IOException e) {
            Log.e("RestoreOp", "Failed to extract tar file", e);
            return false;
        }
    }


}







