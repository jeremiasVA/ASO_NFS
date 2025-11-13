// src/main/java/com/ejemplo/nfs/ExportEntry.java
package com.ejemplo.nfs;

import java.util.*;

public class ExportEntry {
    private String directory;
    private String host;
    private final Set<String> options = new HashSet<>();

    public ExportEntry(String directory, String host) {
        this.directory = directory;
        this.host = host;
    }

    // Getters y Setters
    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public void addOption(String option) { options.add(option); }
    public void removeOption(String option) { options.remove(option); }
    public boolean hasOption(String option) { return options.contains(option); }
    public Set<String> getOptions() { return new HashSet<>(options); }

    @Override
    public String toString() {
        return directory + " " + host + "(" + String.join(",", options) + ")";
    }

    public String toExportsLine() {
        return directory + " " + host + "(" + String.join(",", options) + ")\n";
    }
}