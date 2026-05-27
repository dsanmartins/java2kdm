package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class BodyStatementModel {

    private String type;
    private String statementType;
    private String controlType;

    private String condition;
    private String selector;
    private String value;

    private List<String> targets = new ArrayList<>();

    private String valueKind;
    private String className;

    private CallModel valueCall;
    private List<CallModel> valueCalls = new ArrayList<>();
    private List<CallModel> conditionCalls = new ArrayList<>();
    private List<CallModel> exceptionCalls = new ArrayList<>();

    private List<BodyStatementModel> body = new ArrayList<>();
    private List<BodyStatementModel> elseBody = new ArrayList<>();
    private List<BodyStatementModel> catchClauses = new ArrayList<>();
    private List<BodyStatementModel> finallyBody = new ArrayList<>();

    private String exceptionType;
    private String parameterName;

    private Integer lineStart;
    private Integer lineEnd;

    public BodyStatementModel() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public String getControlType() {
        return controlType;
    }

    public void setControlType(String controlType) {
        this.controlType = controlType;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public String getValueKind() {
        return valueKind;
    }

    public void setValueKind(String valueKind) {
        this.valueKind = valueKind;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public CallModel getValueCall() {
        return valueCall;
    }

    public void setValueCall(CallModel valueCall) {
        this.valueCall = valueCall;
    }

    public List<CallModel> getValueCalls() {
        return valueCalls;
    }

    public void setValueCalls(List<CallModel> valueCalls) {
        this.valueCalls = valueCalls;
    }

    public List<CallModel> getConditionCalls() {
        return conditionCalls;
    }

    public void setConditionCalls(List<CallModel> conditionCalls) {
        this.conditionCalls = conditionCalls;
    }

    public List<CallModel> getExceptionCalls() {
        return exceptionCalls;
    }

    public void setExceptionCalls(List<CallModel> exceptionCalls) {
        this.exceptionCalls = exceptionCalls;
    }

    public List<BodyStatementModel> getBody() {
        return body;
    }

    public void setBody(List<BodyStatementModel> body) {
        this.body = body;
    }

    public List<BodyStatementModel> getElseBody() {
        return elseBody;
    }

    public void setElseBody(List<BodyStatementModel> elseBody) {
        this.elseBody = elseBody;
    }

    public List<BodyStatementModel> getCatchClauses() {
        return catchClauses;
    }

    public void setCatchClauses(List<BodyStatementModel> catchClauses) {
        this.catchClauses = catchClauses;
    }

    public List<BodyStatementModel> getFinallyBody() {
        return finallyBody;
    }

    public void setFinallyBody(List<BodyStatementModel> finallyBody) {
        this.finallyBody = finallyBody;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
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
