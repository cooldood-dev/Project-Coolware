package com.github.cooldood.utils.client;

import com.github.cooldood.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WindowsMediaProvider {

    public static class MediaInfo {
        public boolean isPlaying = false;
        public String title = "";
        public String artist = "";
        public String album = "";
        public boolean hasThumb = false;
        public String thumbPath = "";
        public ResourceLocation thumbTexture = null;
    }

    private static final MediaInfo currentMedia = new MediaInfo();
    private static volatile boolean running = false;
    private static Thread workerThread = null;
    private static String scriptPath = null;
    private static String lastLoadedThumbPath = "";
    private static DynamicTexture cachedDynamicTexture = null;

    public static MediaInfo getCurrentMedia() {
        return currentMedia;
    }

    public static synchronized void start() {
        if (running) return;
        running = true;

        // Ensure script file is written
        try {
            File dir = new File(Main.extraSavedFeaturesPath);
            if (!dir.exists()) dir.mkdirs();
            File scriptFile = new File(dir, "get_media.ps1");
            scriptPath = scriptFile.getAbsolutePath();

            String scriptContent =
                    "Add-Type -AssemblyName System.Runtime.WindowsRuntime\r\n" +
                    "$asTaskGeneric = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.IsGenericMethod } | Select-Object -First 1\r\n" +
                    "function Await($WinRtTask, $ResultType) {\r\n" +
                    "    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)\r\n" +
                    "    $netTask = $asTask.Invoke($null, @($WinRtTask))\r\n" +
                    "    $netTask.Wait(-1) | Out-Null\r\n" +
                    "    return $netTask.Result\r\n" +
                    "}\r\n" +
                    "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null\r\n" +
                    "[Windows.Storage.Streams.Buffer,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null\r\n" +
                    "$manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\r\n" +
                    "$session = $manager.GetCurrentSession()\r\n" +
                    "if ($session) {\r\n" +
                    "    $media = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\r\n" +
                    "    $title = if ($media.Title) { $media.Title } else { '' }\r\n" +
                    "    $artist = if ($media.Artist) { $media.Artist } else { '' }\r\n" +
                    "    $album = if ($media.AlbumTitle) { $media.AlbumTitle } else { '' }\r\n" +
                    "    $hasThumb = $false\r\n" +
                    "    $outPath = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), 'coolware_song_cover.png')\r\n" +
                    "    if ($media.Thumbnail) {\r\n" +
                    "        try {\r\n" +
                    "            $stream = Await ($media.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\r\n" +
                    "            $buffer = New-Object Windows.Storage.Streams.Buffer $stream.Size\r\n" +
                    "            $readBuffer = Await ($stream.ReadAsync($buffer, $stream.Size, [Windows.Storage.Streams.InputStreamOptions]::None)) ([Windows.Storage.Streams.IBuffer])\r\n" +
                    "            $bytes = [System.Runtime.InteropServices.WindowsRuntime.WindowsRuntimeBufferExtensions]::ToArray($readBuffer)\r\n" +
                    "            [System.IO.File]::WriteAllBytes($outPath, $bytes)\r\n" +
                    "            $hasThumb = $true\r\n" +
                    "        } catch {\r\n" +
                    "            $hasThumb = $false\r\n" +
                    "        }\r\n" +
                    "    }\r\n" +
                    "    $titleEsc = $title.Replace('\\', '\\\\').Replace('\"', '\\\"')\r\n" +
                    "    $artistEsc = $artist.Replace('\\', '\\\\').Replace('\"', '\\\"')\r\n" +
                    "    $albumEsc = $album.Replace('\\', '\\\\').Replace('\"', '\\\"')\r\n" +
                    "    Write-Output \"JSON:{\\\"status\\\":\\\"ok\\\",\\\"title\\\":\\\"$titleEsc\\\",\\\"artist\\\":\\\"$artistEsc\\\",\\\"album\\\":\\\"$albumEsc\\\",\\\"hasThumb\\\":$($hasThumb.ToString().ToLower()),\\\"thumbPath\\\":\\\"$($outPath.Replace('\\', '\\\\'))\\\"}\"\r\n" +
                    "} else {\r\n" +
                    "    Write-Output 'JSON:{\"status\":\"no_session\"}'\r\n" +
                    "}\r\n";

            Files.write(Paths.get(scriptPath), scriptContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        workerThread = new Thread(() -> {
            while (running) {
                try {
                    pollMedia();
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Coolware-WindowsMediaThread");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    private static void pollMedia() {
        if (PlatformUtil.getOS() != PlatformUtil.OS.WINDOWS || scriptPath == null) return;

        try {
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptPath);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            String jsonOutput = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("JSON:")) {
                    jsonOutput = line.substring(5);
                    break;
                }
            }
            process.waitFor();

            if (jsonOutput != null) {
                JsonObject json = new JsonParser().parse(jsonOutput).getAsJsonObject();
                String status = json.get("status").getAsString();

                if ("ok".equals(status)) {
                    synchronized (currentMedia) {
                        currentMedia.isPlaying = true;
                        currentMedia.title = json.has("title") ? json.get("title").getAsString() : "";
                        currentMedia.artist = json.has("artist") ? json.get("artist").getAsString() : "";
                        currentMedia.album = json.has("album") ? json.get("album").getAsString() : "";
                        currentMedia.hasThumb = json.has("hasThumb") && json.get("hasThumb").getAsBoolean();
                        currentMedia.thumbPath = json.has("thumbPath") ? json.get("thumbPath").getAsString() : "";
                    }
                } else {
                    synchronized (currentMedia) {
                        currentMedia.isPlaying = false;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Checks if a new thumbnail image file has been downloaded and uploads it to OpenGL on main render thread.
     */
    public static void updateGlTexture() {
        synchronized (currentMedia) {
            if (!currentMedia.hasThumb || currentMedia.thumbPath.isEmpty()) {
                currentMedia.thumbTexture = null;
                return;
            }

            if (!currentMedia.thumbPath.equals(lastLoadedThumbPath)) {
                try {
                    File file = new File(currentMedia.thumbPath);
                    if (file.exists() && file.length() > 0) {
                        BufferedImage image = ImageIO.read(file);
                        if (image != null) {
                            cachedDynamicTexture = new DynamicTexture(image);
                            currentMedia.thumbTexture = C.mc.getTextureManager().getDynamicTextureLocation("coolware_media_cover", cachedDynamicTexture);
                            lastLoadedThumbPath = currentMedia.thumbPath;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
