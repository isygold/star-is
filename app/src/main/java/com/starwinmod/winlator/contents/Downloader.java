package com.starwinmod.winlator.contents;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader {
    /** User-Agent sent to GitHub API and CDN — required for asset downloads. */
    private static final String USER_AGENT = "StarWinMod/1.0";

    /** Connect timeout in milliseconds. */
    private static final int CONNECT_TIMEOUT_MS = 15_000;

    /** Read timeout in milliseconds. */
    private static final int READ_TIMEOUT_MS = 30_000;

    private static HttpURLConnection openConnection(String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        return conn;
    }

    /**
     * Downloads a binary file from {@code address} to {@code file}.
     * Returns true only if the full file was written successfully.
     * Detects HTML error responses from CDNs by checking Content-Type.
     */
    public static boolean downloadFile(String address, File file) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(address);

            // Reject HTML responses (e.g. GitHub CDN error page when User-Agent is missing)
            String contentType = conn.getContentType();
            if (contentType != null && contentType.toLowerCase().startsWith("text/html")) {
                return false;
            }

            int contentLength = conn.getContentLength();
            InputStream input = conn.getInputStream();
            OutputStream output = new FileOutputStream(file.getAbsolutePath());

            byte[] data = new byte[8192];
            long totalRead = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
                totalRead += count;
            }

            output.flush();
            output.close();
            input.close();

            // Verify we got the expected amount of data (if server sent Content-Length)
            return contentLength <= 0 || totalRead >= contentLength;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Downloads a UTF-8 text response from {@code address}.
     * Returns null on any error (network, timeout, non-text response).
     */
    public static String downloadString(String address) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(address);

            // Reject unexpected content types
            String contentType = conn.getContentType();
            if (contentType != null && !contentType.toLowerCase().startsWith("text/") &&
                !contentType.toLowerCase().startsWith("application/json")) {
                return null;
            }

            InputStream input = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}