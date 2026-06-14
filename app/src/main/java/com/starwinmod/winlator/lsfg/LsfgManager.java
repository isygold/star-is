package com.starwinmod.winlator.lsfg;

import android.content.Context;
import android.util.Log;

import com.starwinmod.winlator.core.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manages the LSFG (Lossless Scaling Frame Generation) Vulkan layer.
 *
 * On first container boot that has LSFG enabled, this extracts the
 * layer .so and manifest JSON to the container's Vulkan implicit layer
 * directory so the Vulkan loader can discover it via VK_LAYER_PATH.
 *
 * Also supports installing a user-provided lossless.dll (from the
 * Lossless Scaling app on Steam/PC) which the layer can use for
 * proprietary frame-generation shaders instead of the open-source
 * reimplementation.
 */
public class LsfgManager {
    private static final String TAG = "LsfgManager";
    private static final String LSFG_ASSET_DIR = "lsfg";
    private static final String LAYER_SO_NAME = "libVkLayer_LSFGVK_frame_generation.so";
    private static final String LAYER_MANIFEST_NAME = "VkLayer_LSFGVK_frame_generation.json";
    private static final String LAYER_INSTALL_DIR = "usr/share/vulkan/implicit_layer.d/lsfg";

    /** Name of the custom DLL that users can provide from Lossless Scaling on PC. */
    public static final String CUSTOM_DLL_NAME = "lossless.dll";

    /** Environment variable the layer reads to locate lossless.dll. */
    public static final String LSFG_DLL_PATH_ENV = "LSFG_DLL_PATH";

    /**
     * @param rootDir  The container's root directory (ImageFs root)
     * @return The layer directory inside the container
     */
    public static File getLayerDir(File rootDir) {
        return new File(rootDir, LAYER_INSTALL_DIR);
    }

    /**
     * @param rootDir  The container's root directory
     * @return The path where lossless.dll would be placed inside the container
     */
    public static File getDllPath(File rootDir) {
        return new File(getLayerDir(rootDir), CUSTOM_DLL_NAME);
    }

    /**
     * Ensure the LSFG layer files are installed in the given root directory.
     * Extracts from APK assets if not already present.
     *
     * @param context  Android context for asset access
     * @param rootDir  The container's root directory (ImageFs root)
     * @return true if the layer is available after this call
     */
    public static boolean ensureLayerInstalled(Context context, File rootDir) {
        File layerDir = getLayerDir(rootDir);
        File manifestFile = new File(layerDir, LAYER_MANIFEST_NAME);
        File soFile = new File(layerDir, LAYER_SO_NAME);

        if (manifestFile.isFile() && soFile.isFile()) {
            Log.d(TAG, "LSFG layer already installed at " + layerDir);
            return true;
        }

        Log.d(TAG, "Installing LSFG layer to " + layerDir);
        if (!layerDir.exists() && !layerDir.mkdirs()) {
            Log.e(TAG, "Failed to create layer directory: " + layerDir);
            return false;
        }

        try {
            // Extract manifest JSON
            extractAsset(context, LSFG_ASSET_DIR + "/" + LAYER_MANIFEST_NAME, manifestFile);
            // Extract layer .so from native lib dir (it's in jniLibs)
            String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
            File sourceSo = new File(nativeLibDir, LAYER_SO_NAME);
            if (sourceSo.isFile()) {
                FileUtils.copy(sourceSo, soFile);
                FileUtils.chmod(soFile, 0755);
                Log.d(TAG, "Copied LSFG .so from " + sourceSo + " to " + soFile);
            } else {
                // Fallback: try assets
                extractAsset(context, LSFG_ASSET_DIR + "/" + LAYER_SO_NAME, soFile);
                FileUtils.chmod(soFile, 0755);
                Log.d(TAG, "Extracted LSFG .so from assets to " + soFile);
            }

            if (manifestFile.isFile() && soFile.isFile()) {
                Log.d(TAG, "LSFG layer installed successfully");
                return true;
            } else {
                Log.e(TAG, "LSFG layer installation incomplete");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to install LSFG layer: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install a user-provided lossless.dll into the container's Vulkan layer
     * directory. The layer will find it via {@link #LSFG_DLL_PATH_ENV}.
     *
     * @param rootDir  The container's root directory
     * @param dllFile  The lossless.dll file from user device storage
     * @return true if the DLL was successfully installed
     */
    public static boolean installCustomDll(File rootDir, File dllFile) {
        if (dllFile == null || !dllFile.isFile()) {
            Log.e(TAG, "Custom DLL not found: " + (dllFile != null ? dllFile.getPath() : "null"));
            return false;
        }

        File layerDir = getLayerDir(rootDir);
        if (!layerDir.exists() && !layerDir.mkdirs()) {
            Log.e(TAG, "Failed to create layer directory for custom DLL: " + layerDir);
            return false;
        }

        File dest = getDllPath(rootDir);
        try {
            FileUtils.copy(dllFile, dest);
            FileUtils.chmod(dest, 0644);
            Log.d(TAG, "Installed custom DLL from " + dllFile.getPath() + " to " + dest);
            return dest.isFile();
        } catch (Exception e) {
            Log.e(TAG, "Failed to install custom DLL: " + e.getMessage());
            return false;
        }
    }

    private static void extractAsset(Context context, String assetPath, File dest) throws IOException {
        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
