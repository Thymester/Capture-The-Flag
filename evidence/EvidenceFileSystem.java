package evidence;

import game.GameState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvidenceFileSystem {
    private final Map<String, String> files = new LinkedHashMap<>();
    private String currentDirectory = "/";

    public EvidenceFileSystem(GameState.SystemProfile profile, String evidence, String lastLogin) {
        buildFiles(profile, evidence, lastLogin);
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public Map<String, String> getFiles() {
        return new LinkedHashMap<>(files);
    }

    public List<String> listDirectory(boolean showHidden) {
        String directory = currentDirectory.endsWith("/") ? currentDirectory : currentDirectory + "/";
        List<String> entries = new ArrayList<>();

        for (String path : files.keySet()) {
            if (!path.startsWith(directory)) {
                continue;
            }

            String entry = path.substring(directory.length());
            if (entry.contains("/")) {
                entry = entry.substring(0, entry.indexOf('/')) + "/";
            }
            if ((showHidden || !entry.startsWith(".")) && !entries.contains(entry)) {
                entries.add(entry);
            }
        }

        return entries;
    }

    public String readFile(String path) {
        return files.get(resolvePath(path));
    }

    public boolean changeDirectory(String path) {
        String resolvedPath = resolvePath(path);
        if (resolvedPath.equals("/") || containsDirectory(resolvedPath)) {
            currentDirectory = resolvedPath;
            return true;
        }
        return false;
    }

    public List<String> findFiles(String startingPath) {
        String resolvedPath = resolvePath(startingPath);
        List<String> matches = new ArrayList<>();
        String prefix = resolvedPath.endsWith("/") ? resolvedPath : resolvedPath + "/";

        for (String path : files.keySet()) {
            if (path.startsWith(prefix)) {
                matches.add(path);
            }
        }

        return matches;
    }

    public List<String> grepFiles(String searchText) {
        List<String> matches = new ArrayList<>();
        String normalizedSearch = searchText.toLowerCase();

        for (Map.Entry<String, String> file : files.entrySet()) {
            if (file.getValue().toLowerCase().contains(normalizedSearch)) {
                matches.add(file.getKey());
            }
        }

        return matches;
    }

    private void buildFiles(GameState.SystemProfile profile, String evidence, String lastLogin) {
        files.put(profile.getLogPath(),
                "[" + profile.getLogLevel() + "] login accepted for analyst\n"
                + "[INFO] " + profile.getLogMessage() + "\n"
                + "[DEBUG] archived diagnostic: " + profile.getLeadPath());
        files.put(profile.getLeadPath(),
                "maintenance note: old artifact moved after the incident\n"
                + "next evidence: " + profile.getArtifactPath() + "\n"
                + profile.getLogNote());
        files.put(profile.getArtifactPath(),
                profile.getArtifactLabel() + "=" + evidence + "\n"
                + "source timestamp: " + lastLogin);
        files.put("/tmp/README.txt",
                "Temporary files may contain useful evidence. Hidden files are not shown by plain ls.");
    }

    private boolean containsDirectory(String path) {
        String directory = path.endsWith("/") ? path : path + "/";
        for (String file : files.keySet()) {
            if (file.startsWith(directory)) {
                return true;
            }
        }
        return false;
    }

    private String resolvePath(String path) {
        if (path == null || path.isBlank()) {
            return currentDirectory;
        }

        String combinedPath = path.startsWith("/") ? path : currentDirectory + "/" + path;
        List<String> parts = new ArrayList<>();
        for (String part : combinedPath.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
            }
            else {
                parts.add(part);
            }
        }

        return "/" + String.join("/", parts);
    }
}
