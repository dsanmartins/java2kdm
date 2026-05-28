package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class ExternalTypeModel {

    private String name;
    private String qualifiedName;
    private String packageName;
    private String kind;
    private boolean external = true;
    private List<String> typeArguments = new ArrayList<>();

    public ExternalTypeModel() {
    }

    public ExternalTypeModel(String name, String qualifiedName, String packageName, String kind) {
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.packageName = packageName;
        this.kind = kind;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQualifiedName() { return qualifiedName; }
    public void setQualifiedName(String qualifiedName) { this.qualifiedName = qualifiedName; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public boolean isExternal() { return external; }
    public void setExternal(boolean external) { this.external = external; }

    public List<String> getTypeArguments() { return typeArguments; }
    public void setTypeArguments(List<String> typeArguments) { this.typeArguments = typeArguments != null ? typeArguments : new ArrayList<>(); }
}
