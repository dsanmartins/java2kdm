# java2kdm

`java2kdm` is a Java-based static extractor that analyzes Java source-code projects and generates a generic JSON model suitable for KDM generation.

The project is designed to work as the Java front-end of a larger KDM pipeline. Its main responsibility is to parse Java projects, extract structural and semantic information, and export a language-independent model that can later be transformed into KDM XMI.

```text
Java project
   ↓
java2kdm
   ↓
generic static code model JSON
   ↓
JSON Schema validation
   ↓
KDM generator or another downstream tool
```

---

## Current purpose

The goal of `java2kdm` is not to generate KDM XMI directly. Instead, it generates an intermediate model in JSON format.

This design allows the Java extractor to be reused by different tools and pipelines. For example, the generated JSON can be consumed by a Python-based KDM generator, a model transformation tool, or an architecture recovery process.

---

## Main features

The current version extracts:

- Java source files.
- Package declarations.
- Imports.
- Classes.
- Fields.
- Methods.
- Constructors.
- Method parameters.
- Return types.
- Modifiers.
- Annotations.
- Extended types.
- Implemented types.
- Method calls.
- Resolved method-call targets when available through JavaParser.
- Field type usages.
- Generic relationships such as imports, calls, and uses-type.

The generated model follows a generic structure:

```json
{
  "projectName": "demo-java-project",
  "language": "java",
  "files": [],
  "elements": [],
  "relationships": []
}
```

---

## Technologies

`java2kdm` is implemented in Java using Maven.

Main dependencies:

- JavaParser Core.
- JavaParser Symbol Solver.
- Jackson.
- NetworkNT JSON Schema Validator.
- SLF4J Simple.

The project has been tested with Java 21.

---

## Maven project structure

A recommended structure is:

```text
java2kdm/
├── pom.xml
├── schemas/
│   └── static_code_model.schema.json
├── output/
│   └── model.json
└── src/
    └── main/
        └── java/
            └── cl/
                └── ucn/
                    ├── main/
                    │   └── Main.java
                    ├── extractor/
                    ├── exporter/
                    │   └── JsonExporter.java
                    ├── model/
                    └── validator/
                        └── JsonSchemaValidator.java
```

The exact package names may vary, but the recommended base package is:

```text
cl.ucn
```

---

## Input and output

### Input

The input is a Java project directory.

Example:

```text
/home/dsanmartins/Insync/daniel.sanmartin@ucn.cl/GDrive/GDrive/Developments/demo-java-project/
```

The extractor scans the project and analyzes Java files.

### Output

The output is a JSON model.

Example:

```text
output/model.json
```

The output can optionally be validated against a JSON Schema.

---

## CLI usage

The expected CLI format is:

```bash
java -jar target/java2kdm-1.0-SNAPSHOT.jar <projectPath> <outputJsonPath> [schemaPath]
```

Example:

```bash
java -jar target/java2kdm-1.0-SNAPSHOT.jar \
  /home/dsanmartins/Insync/daniel.sanmartin@ucn.cl/GDrive/GDrive/Developments/demo-java-project/ \
  output/model.json \
  schemas/static_code_model.schema.json
```

When the schema path is provided, the generated JSON is validated automatically.

Expected console output:

```text
[java2kdm] Input project: /path/to/demo-java-project
[java2kdm] Output JSON: output/model.json
[java2kdm] Schema: schemas/static_code_model.schema.json
[java2kdm] Extraction completed.
[java2kdm] Java files found: 2
[java2kdm] Model generated at: output/model.json
JSON schema validation: OK
[java2kdm] Schema validation completed successfully.
```

---

## Building the project

From the project root:

```bash
mvn clean package
```

The JAR should be generated in:

```text
target/java2kdm-1.0-SNAPSHOT.jar
```

If you need a runnable JAR with dependencies, configure the Maven Shade Plugin or the Maven Assembly Plugin.

---

## Example output

For a small project with these files:

```text
src/main/java/com/example/service/UserService.java
src/main/java/com/example/repository/UserRepository.java
```

the generated JSON has this shape:

```json
{
  "projectName": "demo-java-project",
  "language": "java",
  "files": [
    {
      "path": "/path/to/UserService.java",
      "packageName": "com.example.service",
      "imports": [
        "com.example.repository.UserRepository"
      ]
    }
  ],
  "elements": [
    {
      "name": "UserService",
      "qualifiedName": "com.example.service.UserService",
      "kind": "class",
      "packageName": "com.example.service",
      "fields": [],
      "methods": []
    }
  ],
  "relationships": [
    {
      "type": "imports",
      "source": "/path/to/UserService.java",
      "target": "com.example.repository.UserRepository",
      "sourceFile": "/path/to/UserService.java",
      "line": null
    }
  ]
}
```

---

## JSON model contract

The generated JSON is intentionally language-independent. Java-specific information should be normalized into generic concepts whenever possible.

### Top-level fields

| Field | Description |
|---|---|
| `projectName` | Name of the analyzed project. |
| `language` | Always `java` for this extractor. |
| `files` | Source files analyzed by the extractor. |
| `elements` | Structural code elements such as classes, methods, fields, and parameters. |
| `relationships` | Semantic relations such as imports, calls, and type usages. |

---

## `files`

Each file entry represents a Java source file.

```json
{
  "path": "/path/to/UserService.java",
  "packageName": "com.example.service",
  "imports": [
    "com.example.repository.UserRepository"
  ]
}
```

Recommended fields:

| Field | Description |
|---|---|
| `path` | Absolute or normalized file path. |
| `packageName` | Java package declared in the file. |
| `imports` | List of imported types or packages. |

---

## `elements`

Each entry in `elements` represents a structural code element.

### Class element

```json
{
  "name": "UserService",
  "qualifiedName": "com.example.service.UserService",
  "kind": "class",
  "packageName": "com.example.service",
  "filePath": "/path/to/UserService.java",
  "modifiers": ["public"],
  "annotations": [],
  "extendsTypes": [],
  "implementsTypes": [],
  "fields": [],
  "methods": []
}
```

### Field element

```json
{
  "name": "repository",
  "type": "UserRepository",
  "resolvedType": "com.example.repository.UserRepository",
  "modifiers": ["private"],
  "annotations": []
}
```

### Method element

```json
{
  "name": "getUserName",
  "returnType": "String",
  "kind": "method",
  "signature": "getUserName(Long)",
  "qualifiedSignature": "com.example.service.UserService.getUserName(java.lang.Long)",
  "modifiers": ["public"],
  "annotations": [],
  "parameters": [],
  "calls": []
}
```

### Constructor element

```json
{
  "name": "UserService",
  "returnType": "void",
  "kind": "constructor",
  "signature": "UserService(UserRepository)",
  "qualifiedSignature": "com.example.service.UserService.<init>(com.example.repository.UserRepository)",
  "modifiers": ["public"],
  "annotations": [],
  "parameters": [],
  "calls": []
}
```

### Parameter element

```json
{
  "name": "id",
  "type": "Long",
  "resolvedType": "java.lang.Long"
}
```

---

## `relationships`

Relationships represent links between source elements.

### Import relationship

```json
{
  "type": "imports",
  "source": "/path/to/UserService.java",
  "target": "com.example.repository.UserRepository",
  "sourceFile": "/path/to/UserService.java",
  "line": null
}
```

### Type usage relationship

```json
{
  "type": "uses_type",
  "source": "com.example.service.UserService.repository",
  "target": "com.example.repository.UserRepository",
  "sourceFile": "/path/to/UserService.java",
  "line": 7
}
```

### Call relationship

```json
{
  "type": "calls",
  "source": "com.example.service.UserService.getUserName(java.lang.Long)",
  "target": "com.example.repository.UserRepository.findNameById(java.lang.Long)",
  "sourceFile": "/path/to/UserService.java",
  "line": 14
}
```

---

## JavaParser symbol resolution

The extractor uses JavaParser Symbol Solver to resolve types and method calls when possible.

For example, a call such as:

```java
repository.findNameById(id);
```

may be resolved to:

```text
com.example.repository.UserRepository.findNameById(java.lang.Long)
```

and represented in JSON as:

```json
{
  "methodName": "findNameById",
  "scope": "repository",
  "argumentCount": 1,
  "resolvedTarget": "com.example.repository.UserRepository.findNameById(java.lang.Long)"
}
```

The corresponding relationship is:

```json
{
  "type": "calls",
  "source": "com.example.service.UserService.getUserName(java.lang.Long)",
  "target": "com.example.repository.UserRepository.findNameById(java.lang.Long)",
  "sourceFile": "/path/to/UserService.java",
  "line": 14
}
```

---

## JSON validation

If a schema is provided, the tool validates the generated model after export.

Example:

```bash
java -jar target/java2kdm-1.0-SNAPSHOT.jar \
  /path/to/project \
  output/model.json \
  schemas/static_code_model.schema.json
```

Expected success message:

```text
JSON schema validation: OK
```

If validation fails, the validator prints the schema errors and the process should finish with a non-zero exit code.

---

## Integration with py2kdm

`java2kdm` can be used by `py2kdm` as an external Java extractor.

Recommended location inside `py2kdm`:

```text
py2kdm/
└── tools/
    └── java2kdm/
        └── java2kdm-1.0-SNAPSHOT.jar
```

Example `py2kdm` config:

```json
{
  "project_name": "demo-java-project",
  "language": "java",
  "input": {
    "source_path": "/path/to/demo-java-project"
  },
  "outputs": {
    "intermediate_json": "outputs/demo-java-project/java_model.json",
    "kdm_xmi": "outputs/demo-java-project/model.kdm.xmi"
  },
  "java_extractor": {
    "jar_path": "tools/java2kdm/java2kdm-1.0-SNAPSHOT.jar",
    "schema_path": "schemas/python_model.schema.json"
  },
  "kdm_generation": {
    "enabled": true,
    "validate": true,
    "input": "intermediate_json"
  }
}
```

Then run:

```bash
python run_pipeline.py --config configs/demo_java_project.json
```

---

## Current validation status

The current version has been tested with a small Java project containing:

```text
UserService
UserRepository
```

The generated JSON validates successfully against the static code model schema.

The JSON can also be transformed into a KDM XMI model by the downstream KDM generator with:

```text
Errors: 0
Warnings: 0
```

---

## Known limitations

The current version focuses mainly on structural information and basic relationships.

Missing or incomplete features include:

- Local variable extraction.
- Assignment extraction.
- Object creation extraction using `new`.
- Return statement extraction.
- Reads and writes.
- Control-flow extraction.
- Try/catch/finally extraction.
- Throw statement extraction.
- Rich annotation semantics.
- Framework-specific concepts such as Spring components, JPA entities, controllers, repositories, services, and dependency injection.

---

## Recommended next milestones

### Milestone 1: Local variables

Extract local variables from method and constructor bodies.

Example target JSON:

```json
{
  "name": "user",
  "type": "User",
  "resolvedType": "com.example.model.User",
  "line": 15
}
```

### Milestone 2: Object creations

Extract `new` expressions.

Example Java:

```java
User user = new User(id, name);
```

Target relationship:

```json
{
  "type": "creates",
  "source": "com.example.service.UserService.createUser(java.lang.Long,java.lang.String)",
  "target": "com.example.model.User.<init>(java.lang.Long,java.lang.String)",
  "sourceFile": "/path/to/UserService.java",
  "line": 20
}
```

### Milestone 3: Assignments

Extract assignments and field writes.

Example Java:

```java
this.repository = repository;
```

Target relationship:

```json
{
  "type": "writes",
  "source": "com.example.service.UserService.<init>(com.example.repository.UserRepository)",
  "target": "com.example.service.UserService.repository",
  "sourceFile": "/path/to/UserService.java",
  "line": 10
}
```

### Milestone 4: Returns

Extract return statements and value calls.

Example Java:

```java
return repository.findNameById(id);
```

Target body information may include:

```json
{
  "statementType": "return",
  "lineStart": 14,
  "lineEnd": 14,
  "valueCalls": []
}
```

### Milestone 5: Exceptions

Extract:

```text
try
catch
finally
throw
```

and generate relationships such as:

```text
throws
exception_flow
exit_flow
```

---

## Development notes

The extractor should continue to produce a generic model, not a Java-specific model.

Good:

```json
{
  "kind": "class",
  "methods": [],
  "relationships": []
}
```

Avoid making downstream tools depend on Java-only field names when a generic equivalent is possible.

The preferred architecture is:

```text
JavaParser AST
   ↓
Java-specific extraction logic
   ↓
Generic static code model
   ↓
JSON Schema validation
```

---

## License

Add the project license here.
