package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class TypeModel {

    private String name;
    private String qualifiedName;
    private String kind; // class, interface, enum
    private String packageName;
    private String filePath;

    private List<String> modifiers = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();

    private List<String> extendsTypes = new ArrayList<>();
    private List<String> implementsTypes = new ArrayList<>();

    private List<FieldModel> fields = new ArrayList<>();
    private List<MethodModel> methods = new ArrayList<>();

    public TypeModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public List<String> getExtendsTypes() {
        return extendsTypes;
    }

    public void setExtendsTypes(List<String> extendsTypes) {
        this.extendsTypes = extendsTypes;
    }

    public List<String> getImplementsTypes() {
        return implementsTypes;
    }

    public void setImplementsTypes(List<String> implementsTypes) {
        this.implementsTypes = implementsTypes;
    }

    public List<FieldModel> getFields() {
        return fields;
    }

    public void setFields(List<FieldModel> fields) {
        this.fields = fields;
    }

    public List<MethodModel> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodModel> methods) {
        this.methods = methods;
    }
}
