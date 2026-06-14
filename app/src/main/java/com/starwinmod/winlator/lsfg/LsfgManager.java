package com.starwinmod.winlator.lsfg;

import android.content.Context;
import android.util.Log;

import com.starwinmod.winlator.core.EnvVars;
import com.starwinmod.winlator.core.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Manages the LSFG (Lossless Scaling Frame Generation) Vulkan layer.
 *
 * <p>Responsible for:
 * <ul>
 *   <li>Extracting the layer {@code .so} and Vulkan manifest to a container</li>
 *   <li>Installing a user-provided {@code lossless.dll}</li>
 *   <li>Wiring environment variables so the Vulkan loader discovers the layer</li>
 *   <li>Validating the installed layer files</li>
 * </ul>
 */
public final class LsfgManager {
    private static final String TAG = "LsfgManager";

    // ── Asset and file constants ──────────────────────────────────────────────

    /** Asset subdirectory containing the layer manifest. */
    public static final String LSFG_ASSET_DIR = "lsfg";

    /** Filename of the Vulkan layer shared library. */
    public static final String LAYER_SO_NAME = "libVkLayer_LSFGVK_frame_generation.so";

    /** Filename of the Vulkan implicit-layer manifest. */
    public static final String LAYER_MANIFEST_NAME = "VkLayer_LSFGVK_frame_generation.json";

    /** Relative path inside the container where the layer is installed. */
    public static final String LAYER_INSTALL_DIR = "usr/share/vulkan/implicit_layer.d/lsfg";

    /** Filename of the custom DLL users may provide (from Lossless Scaling on PC). */
    public static final String CUSTOM_DLL_NAME = "lossless.dll";

    /** Environment variable the layer reads to locate {@code lossless.dll}. */
    public static final String LSFG_DLL_PATH_ENV = "LSFG_DLL_PATH";

    // ── Path helpers ──────────────────────────────────────────────────────────

    /** @return The layer directory inside the container at {@code rootDir}. */
    public static File getLayerDir(File rootDir) {
        return new File(rootDir, LAYER_INSTALL_DIR);
    }

    /** @return The path where {@code lossless.dll} would be installed. */
    public static File getDllPath(File rootDir) {
        return new File(getLayerDir(rootDir), CUSTOM_DLL_NAME);
    }

    // ── Installation ──────────────────────────────────────────────────────────

    /**
     * Ensures the LSFG layer is installed at {@code rootDir}.
     * Extracts from APK assets / native libs if not already present.
     *
     * @param context  Android context for asset / native-lib access.
     * @param rootDir  Container root directory (ImageFs root).
     * @return {@code true} if the layer is fully installed and valid.
     */
    public static boolean ensureLayerInstalled(Context context, File rootDir) {
        File layerDir = getLayerDir(rootDir);
        File manifestFile = new File(layerDir, LAYER_MANIFEST_NAME);
        File soFile = new File(layerDir, LAYER_SO_NAME);

        // Fast path — already installed
        if (manifestFile.isFile() && soFile.isFile()) {
            Log.d(TAG, "LSFG layer already present at " + layerDir);
            return true;
        }

        Log.d(TAG, "Installing LSFG layer to " + layerDir);
        if (!layerDir.exists() && !layerDir.mkdirs()) {
            Log.e(TAG, "Failed to create " + layerDir);
            return false;
        }

        try {
            // 1. Manifest JSON from assets
            extractAsset(context, LSFG_ASSET_DIR + "/" + LAYER_MANIFEST_NAME, manifestFile);

            // 2. Layer .so — prefer nativeLibraryDir (jniLibs), fall back to assets
            String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
            File sourceSo = new File(nativeLibDir, LAYER_SO_NAME);
            if (sourceSo.isFile()) {
                Files.copy(sourceSo.toPath(), soFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Log.d(TAG, "Copied .so from " + sourceSo);
            } else {
                extractAsset(context, LSFG_ASSET_DIR + "/" + LAYER_SO_NAME, soFile);
                Log.d(TAG, "Extracted .so from assets");
            }

            if (!soFile.setExecutable(true, false)) {
                Log.w(TAG, "Could not set executable bit on " + soFile);
            }

            if (manifestFile.isFile() && soFile.isFile()) {
                Log.d(TAG, "LSFG layer installed successfully");
                return true;
            }
            Log.e(TAG, "LSFG layer installation incomplete — manifest="
                    + manifestFile.isFile() + " so=" + soFile.isFile());
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Failed to install LSFG layer", e);
            return false;
        }
    }

    /**
     * Installs a user-provided {@code lossless.dll} into the container.
     *
     * @param rootDir  Container root directory.
     * @param dllFile  Source {@code lossless.dll} on the host filesystem.
     * @return {@code true} on success.
     */
    public static boolean installCustomDll(File rootDir, File dllFile) {
        if (dllFile == null || !dllFile.isFile()) {
            Log.e(TAG, "Custom DLL not found: " + (dllFile != null ? dllFile : "null"));
            return false;
        }

        File layerDir = getLayerDir(rootDir);
        if (!layerDir.exists() && !layerDir.mkdirs()) {
            Log.e(TAG, "Failed to create " + layerDir);
            return false;
        }

        File dest = getDllPath(rootDir);
        try {
            Files.copy(dllFile.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            if (!dest.setReadable(true, false)) {
                Log.w(TAG, "Could not set readable bit on " + dest);
            }
            Log.d(TAG, "Installed custom DLL: " + dest);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to install custom DLL", e);
            return false;
        }
    }

    /**
     * Removes the LSFG layer files from the container.
     * @return {@code true} if the layer directory was fully removed.
     */
    public static boolean uninstallLayer(File rootDir) {
        File layerDir = getLayerDir(rootDir);
        if (!layerDir.exists()) return true;
        try {
            FileUtils.delete(layerDir);
            Log.d(TAG, "Uninstalled LSFG layer from " + layerDir);
            return !layerDir.exists();
        } catch (Exception e) {
            Log.e(TAG, "Failed to uninstall LSFG layer", e);
            return false;
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /** Checks whether the layer files are present and valid. */
    public static boolean isLayerValid(File rootDir) {
        File manifestFile = new File(getLayerDir(rootDir), LAYER_MANIFEST_NAME);
        File soFile = new File(getLayerDir(rootDir), LAYER_SO_NAME);
        return manifestFile.isFile() && soFile.isFile() && soFile.canExecute();
    }

    // ── Environment setup ─────────────────────────────────────────────────────

    /**
     * Injects the LSFG environment variables into {@code envVars} so the Vulkan
     * loader discovers the layer and the layer reads its configuration.
     *
     * <p>This should be called <em>after</em> {@link #ensureLayerInstalled}.
     *
     * @param rootDir   Container root directory.
     * @param config    The active {@link LsfgConfig}.
     * @param envVars   Mutable environment-variable map to populate.
     */
    public static void setupEnvironment(File rootDir, LsfgConfig config, EnvVars envVars) {
        String lsfgDir = rootDir.getPath() + "/" + LAYER_INSTALL_DIR;
        String parentLayerDir = rootDir.getPath() + "/usr/share/vulkan/implicit_layer.d";

        // Prepend the LSFG layer dir to VK_LAYER_PATH
        String vkLayerPath = envVars.get("VK_LAYER_PATH");
        envVars.put("VK_LAYER_PATH", lsfgDir + (vkLayerPath.isEmpty() ? "" : ":" + vkLayerPath));

        // Also ensure the parent dir is present (for other layers)
        if (!vkLayerPath.contains(parentLayerDir)) {
            envVars.put("VK_LAYER_PATH", parentLayerDir + ":" + envVars.get("VK_LAYER_PATH"));
        }

        // Add layer dir to LD_LIBRARY_PATH so the .so's dependencies resolve
        String ldLibPath = envVars.get("LD_LIBRARY_PATH");
        envVars.put("LD_LIBRARY_PATH", lsfgDir + (ldLibPath.isEmpty() ? "" : ":" + ldLibPath));

        // Layer configuration
        envVars.put("LSFG_MULTIPLIER",  String.valueOf(config.getMultiplier()));
        envVars.put("LSFG_QUALITY",     config.getQuality());
        envVars.put("LSFG_FLOW_SCALE",  String.valueOf(config.getFlowScale()));
        envVars.put("LSFG_MAX_LATENCY", String.valueOf(config.getMaxLatency()));
        envVars.put("LSFG_GPU_ARCH",    config.getGpuArch());

        // Custom DLL
        if (config.isCustomDllEnabled()) {
            String dllPath = config.getCustomDllPath();
            if (dllPath != null && !dllPath.isEmpty()) {
                File dllFile = new File(dllPath);
                if (installCustomDll(rootDir, dllFile)) {
                    String dllContainerPath = getDllPath(rootDir).getPath();
                    envVars.put(LSFG_DLL_PATH_ENV, dllContainerPath);
                    Log.d(TAG, LSFG_DLL_PATH_ENV + "=" + dllContainerPath);
                } else {
                    Log.w(TAG, "Custom DLL installation failed for: " + dllPath);
                }
            } else {
                Log.w(TAG, "Custom DLL enabled but path is empty");
            }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static void extractAsset(Context context, String assetPath, File dest)
            throws IOException {
        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }

    private LsfgManager() { /* utility class */ }
}
