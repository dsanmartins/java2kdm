package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class ParameterModel {

    private String name;
    private String type;
    private String resolvedType;
    private String resolvedRawType;
    private List<String> resolvedTypeArguments = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private Integer lineStart;
    private Integer lineEnd;

    public ParameterModel() { }

    public ParameterModel(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public ParameterModel(String name, String type, String resolvedType) {
        this.name = name;
        this.type = type;
        this.resolvedType = resolvedType;
    }

    public ParameterModel(String name, String type, String resolvedType, List<String> annotations) {
        this.name = name;
        this.type = type;
        this.resolvedType = resolvedType;
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getResolvedType() { return resolvedType; }
    public void setResolvedType(String resolvedType) { this.resolvedType = resolvedType; }

    public String getResolvedRawType() { return resolvedRawType; }
    public void setResolvedRawType(String resolvedRawType) { this.resolvedRawType = resolvedRawType; }

    public List<String> getResolvedTypeArguments() { return resolvedTypeArguments; }
    public void setResolvedTypeArguments(List<String> resolvedTypeArguments) { this.resolvedTypeArguments = resolvedTypeArguments != null ? resolvedTypeArguments : new ArrayList<>(); }

    public List<String> getAnnotations() { return annotations; }
    public void setAnnotations(List<String> annotations) { this.annotations = annotations != null ? annotations : new ArrayList<>(); }

    public Integer getLineStart() { return lineStart; }
    public void setLineStart(Integer lineStart) { this.lineStart = lineStart; }

    public Integer getLineEnd() { return lineEnd; }
    public void setLineEnd(Integer lineEnd) { this.lineEnd = lineEnd; }
}
