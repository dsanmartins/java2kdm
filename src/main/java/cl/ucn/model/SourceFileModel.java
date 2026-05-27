package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class SourceFileModel {

    private String path;
    private String packageName;
    private List<String> imports = new ArrayList<>();

    public SourceFileModel() {
    }

    public SourceFileModel(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }
}
