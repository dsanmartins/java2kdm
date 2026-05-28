package cl.ucn.model;

import java.util.ArrayList;
import java.util.List;

public class ProjectModel {

    private String projectName;
    private String language = "java";
    private List<PackageModel> packages = new ArrayList<>();
    private List<SourceFileModel> files = new ArrayList<>();
    private List<TypeModel> elements = new ArrayList<>();
    private List<ExternalTypeModel> externalTypes = new ArrayList<>();
    private List<RelationshipModel> relationships = new ArrayList<>();

    public ProjectModel() {
    }

    public ProjectModel(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<PackageModel> getPackages() { return packages; }
    public void setPackages(List<PackageModel> packages) { this.packages = packages != null ? packages : new ArrayList<>(); }

    public List<SourceFileModel> getFiles() { return files; }
    public void setFiles(List<SourceFileModel> files) { this.files = files; }

    public List<TypeModel> getElements() { return elements; }
    public void setElements(List<TypeModel> elements) { this.elements = elements; }

    public List<ExternalTypeModel> getExternalTypes() { return externalTypes; }
    public void setExternalTypes(List<ExternalTypeModel> externalTypes) { this.externalTypes = externalTypes != null ? externalTypes : new ArrayList<>(); }

    public List<RelationshipModel> getRelationships() { return relationships; }
    public void setRelationships(List<RelationshipModel> relationships) { this.relationships = relationships; }
}
