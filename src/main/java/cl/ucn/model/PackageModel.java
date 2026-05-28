package cl.ucn.model;

public class PackageModel {

    private String name;
    private String qualifiedName;
    private String parent;

    public PackageModel() {
    }

    public PackageModel(String name, String qualifiedName, String parent) {
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.parent = parent;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQualifiedName() { return qualifiedName; }
    public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }

    public String getParent() { return parent; }
    public void setParent(String parent) { this.parent = parent; }
}
