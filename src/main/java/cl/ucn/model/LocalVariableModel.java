package cl.ucn.model;

public class LocalVariableModel {

    private String name;
    private String type;
    private String resolvedType;
    private String assignedValue;
    private String assignedType;
    private String valueKind;
    private String valueType;
    private Integer line;
    private String typeResolution;

    public LocalVariableModel() {
    }

    public LocalVariableModel(String name, String type, String resolvedType, Integer line) {
        this.name = name;
        this.type = type;
        this.resolvedType = resolvedType;
        this.line = line;
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

    public String getResolvedType() {
        return resolvedType;
    }

    public void setResolvedType(String resolvedType) {
        this.resolvedType = resolvedType;
    }

    public String getAssignedValue() {
        return assignedValue;
    }

    public void setAssignedValue(String assignedValue) {
        this.assignedValue = assignedValue;
    }

    public String getAssignedType() {
        return assignedType;
    }

    public void setAssignedType(String assignedType) {
        this.assignedType = assignedType;
    }

    public String getValueKind() {
        return valueKind;
    }

    public void setValueKind(String valueKind) {
        this.valueKind = valueKind;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getTypeResolution() {
        return typeResolution;
    }

    public void setTypeResolution(String typeResolution) {
        this.typeResolution = typeResolution;
    }
}
