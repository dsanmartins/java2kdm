package cl.ucn.model;

public class RelationshipModel {

    private String type;
    private String source;
    private String target;
    private String sourceFile;
    private Integer line;

    public RelationshipModel() {
    }

    public RelationshipModel(String type, String source, String target) {
        this.type = type;
        this.source = source;
        this.target = target;
    }

    public RelationshipModel(String type, String source, String target, String sourceFile, Integer line) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.sourceFile = sourceFile;
        this.line = line;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }
}