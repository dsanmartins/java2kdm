package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class MethodModel {

    private String name;
    private String returnType;
    private String kind; // method, constructor
    private String signature;
    private String qualifiedSignature;
    private String resolvedReturnType;

    private List<String> modifiers = new ArrayList<>();
    private List<String> annotations = new ArrayList<>();
    private List<ParameterModel> parameters = new ArrayList<>();
    private List<CallModel> calls = new ArrayList<>();
    public List<CallModel> getCalls() {
        return calls;
    }
    private List<LocalVariableModel> localVariables = new ArrayList<>();
    private List<BodyStatementModel> body = new ArrayList<>();

    public MethodModel() {
    }

    public List<LocalVariableModel> getLocalVariables() {
        return localVariables;
    }

    public void setLocalVariables(List<LocalVariableModel> localVariables) {
        this.localVariables = localVariables;
    }

    public List<BodyStatementModel> getBody() {
        return body;
    }

    public void setBody(List<BodyStatementModel> body) {
        this.body = body;
    }

    public void setCalls(List<CallModel> calls) {
        this.calls = calls;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
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

    public List<ParameterModel> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterModel> parameters) {
        this.parameters = parameters;
    }

    public String getQualifiedSignature() {
        return qualifiedSignature;
    }

    public void setQualifiedSignature(String qualifiedSignature) {
        this.qualifiedSignature = qualifiedSignature;
    }

    public String getResolvedReturnType() {
        return resolvedReturnType;
    }

    public void setResolvedReturnType(String resolvedReturnType) {
        this.resolvedReturnType = resolvedReturnType;
    }
}
