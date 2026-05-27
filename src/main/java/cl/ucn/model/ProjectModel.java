package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectModel {

    private String projectName;
    private String language = "java";
    private List<SourceFileModel> files = new ArrayList<>();
    private List<TypeModel> elements = new ArrayList<>();
    private List<RelationshipModel> relationships = new ArrayList<>();

    public ProjectModel() {
    }

    public ProjectModel(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<SourceFileModel> getFiles() {
        return files;
    }

    public void setFiles(List<SourceFileModel> files) {
        this.files = files;
    }

    public List<TypeModel> getElements() {
        return elements;
    }

    public void setElements(List<TypeModel> elements) {
        this.elements = elements;
    }

    public List<RelationshipModel> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipModel> relationships) {
        this.relationships = relationships;
    }
}