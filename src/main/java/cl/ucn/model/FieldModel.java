package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class FieldModel {

    private String name;
    private String type;
    private List<String> modifiers = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private String resolvedType;

    public FieldModel() {
    }

    public FieldModel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getResolvedType() {
        return resolvedType;
    }

    public void setResolvedType(String resolvedType) {
        this.resolvedType = resolvedType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<String> modifiers) {
        this.modifiers = modifiers;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }
}
