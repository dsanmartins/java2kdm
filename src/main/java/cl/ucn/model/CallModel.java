package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class CallModel {

    private String methodName;
    private String name;
    private String scope;
    private int argumentCount;
    private List<String> arguments = new ArrayList<>();
    private String resolvedTarget;
    private String targetId;
    private String classification;
    private String kind;
    private Integer lineStart;
    private Integer lineEnd;

    public CallModel() {
    }

    public CallModel(String methodName, String scope, int argumentCount) {
        this.methodName = methodName;
        this.name = methodName;
        this.scope = scope;
        this.argumentCount = argumentCount;
    }

    public CallModel(String methodName, String scope, int argumentCount, String resolvedTarget) {
        this.methodName = methodName;
        this.name = methodName;
        this.scope = scope;
        this.argumentCount = argumentCount;
        this.resolvedTarget = resolvedTarget;
        this.targetId = resolvedTarget;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
        this.name = methodName;
    }

    public String getName() {
        return name == null ? methodName : name;
    }

    public void setName(String name) {
        this.name = name;
        this.methodName = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getArgumentCount() {
        return argumentCount;
    }

    public void setArgumentCount(int argumentCount) {
        this.argumentCount = argumentCount;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
        this.argumentCount = arguments == null ? 0 : arguments.size();
    }

    public String getResolvedTarget() {
        return resolvedTarget;
    }

    public void setResolvedTarget(String resolvedTarget) {
        this.resolvedTarget = resolvedTarget;
        this.targetId = resolvedTarget;
    }

    public String getTargetId() {
        return targetId == null ? resolvedTarget : targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
        this.resolvedTarget = targetId;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Integer getLineStart() {
        return lineStart;
    }

    public void setLineStart(Integer lineStart) {
        this.lineStart = lineStart;
    }

    public Integer getLineEnd() {
        return lineEnd;
    }

    public void setLineEnd(Integer lineEnd) {
        this.lineEnd = lineEnd;
    }
}
