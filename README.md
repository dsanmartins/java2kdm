# java2kdm

`java2kdm` is a Java static-analysis extractor that parses Java source-code projects and produces a rich intermediate JSON model for later transformation into KDM XMI.

The tool is intended to work as the Java front-end of a model-driven reverse engineering pipeline. It does not generate KDM directly. Instead, it extracts structural, semantic, and behavioral information from Java projects and exports a generic JSON model that can be consumed by a downstream KDM generator, architecture recovery tool, or model transformation process.

```text
Java project
   ↓
java2kdm
   ↓
Rich intermediate JSON model
   ↓
JSON Schema validation
   ↓
KDM generator / model transformation / architecture analysis
```

---

## Overview

The current version extracts both structural and behavioral information from Java source code.

At the structural level, it extracts files, packages, imports, classes, annotations, fields, methods, constructors, parameters, modifiers, return types, field types, local variables, and resolved type usages.

At the behavioral level, it extracts method and constructor bodies, including assignments, returns, method calls, object creations, control structures, try/catch blocks, throw statements, and call information inside expressions and conditions.

The generated JSON is designed to be generic enough to support KDM generation while still preserving relevant Java-specific details such as qualified signatures, resolved call targets, annotations, and source locations.

---

## Main features

The extractor currently supports:

- Java source-file discovery.
- Package and import extraction.
- Class extraction.
- Annotation declaration detection through `kind = "annotation"`.
- Field extraction.
- Method and constructor extraction.
- Parameter extraction.
- Modifier extraction.
- Java annotation extraction.
- Return type and resolved return type extraction.
- Field, parameter, return, and local-variable type usage relationships.
- Method-call extraction.
- Method-call target resolution using JavaParser Symbol Solver when possible.
- Constructor-call detection for object creations.
- Local-variable extraction.
- Local-variable initializer enrichment:
    - `assignedValue`
    - `assignedType`
    - `valueKind`
    - `valueType`
    - `typeResolution`
- Body statement extraction:
    - assignments
    - returns
    - calls
    - throws
    - object creations
- Control-flow extraction:
    - `if`
    - `for`
    - `foreach`
    - `while`
    - `do`
    - `switch`
- Exception-flow extraction at JSON level:
    - `try`
    - `catch`
    - `finally`
    - `throw`
- Call extraction from:
    - assignment values
    - return values
    - conditions
    - thrown exceptions
- Normalized object creation metadata:
    - `className`
    - `valueKind = "object_creation"`
    - constructor-like `targetId`
- Generic relationships:
    - `imports`
    - `calls`
    - `uses_type`

---

## Technologies

`java2kdm` is implemented in Java with Maven.

Main dependencies:

- JavaParser Core.
- JavaParser Symbol Solver.
- Jackson.
- NetworkNT JSON Schema Validator.
- SLF4J Simple.

The project is configured for Java 21.

---

## Project structure

```text
java2kdm/
├── pom.xml
├── README.md
├── schemas/
│   ├── static_code_model.schema.json
│   └── runtime_code_model_schema.json
├── output/
│   └── model.json
└── src/
    └── main/
        └── java/
            └── cl/
                └── ucn/
                    ├── extractor/
                    │   └── JavaModelExtractor.java
                    ├── exporter/
                    │   └── JsonExporter.java
                    ├── main/
                    │   └── Main.java
                    ├── model/
                    │   ├── BodyStatementModel.java
                    │   ├── CallModel.java
                    │   ├── FieldModel.java
                    │   ├── LocalVariableModel.java
                    │   ├── MethodModel.java
                    │   ├── ParameterModel.java
                    │   ├── ProjectModel.java
                    │   ├── RelationshipModel.java
                    │   ├── SourceFileModel.java
                    │   └── TypeModel.java
                    ├── resolver/
                    │   ├── ConstructorDeclarationResolver.java
                    │   ├── JavaSymbolSolverConfig.java
                    │   ├── MethodCallResolver.java
                    │   ├── MethodDeclarationResolver.java
                    │   └── TypeResolver.java
                    ├── scanner/
                    │   └── ProjectScanner.java
                    └── validator/
                        └── JsonSchemaValidator.java
```

---

## Build

From the project root:

```bash
mvn clean package
```

The executable JAR is generated at:

```text
target/java2kdm-1.0-SNAPSHOT.jar
```

---

## CLI usage

```bash
java -jar target/java2kdm-1.0-SNAPSHOT.jar \
  <input-project-path> \
  <output-json-path> \
  [schema-path]
```

Arguments:

```text
<input-project-path>   Path to the Java project to analyze.
<output-json-path>     Path where the JSON model will be written.
[schema-path]          Optional JSON Schema path for validation.
```

Example:

```bash
java -jar target/java2kdm-1.0-SNAPSHOT.jar \
  /path/to/demo-java-project \
  output/model.json \
  schemas/static_code_model.schema.json
```

Expected output:

```text
[java2kdm] Input project: /path/to/demo-java-project
[java2kdm] Output JSON: /path/to/output/model.json
[java2kdm] Schema: /path/to/schemas/static_code_model.schema.json
[java2kdm] Extraction completed.
[java2kdm] Java files found: 10
[java2kdm] Model generated at: /path/to/output/model.json
JSON schema validation: OK
[java2kdm] Schema validation completed successfully.
```

Exit codes:

```text
0  OK
1  Invalid arguments or invalid input path
2  Extraction error
3  Schema validation error
4  Unexpected error
```

---

## JSON model shape

The generated JSON has the following top-level structure:

```json
{
  "projectName": "demo-java-project",
  "language": "java",
  "files": [],
  "elements": [],
  "relationships": []
}
```

### Top-level fields

| Field | Description |
|---|---|
| `projectName` | Name of the analyzed Java project. |
| `language` | Always `java` for this extractor. |
| `files` | Source files analyzed by the extractor. |
| `elements` | Structural elements such as classes, annotations, fields, methods, constructors, and parameters. |
| `relationships` | Semantic relationships such as imports, calls, and type usages. |

---

## Files

Each file entry represents one Java source file.

```json
{
  "path": "/path/to/UserService.java",
  "packageName": "com.example.service",
  "imports": [
    "com.example.repository.UserRepository",
    "java.util.ArrayList",
    "java.util.List"
  ]
}
```

| Field | Description |
|---|---|
| `path` | Absolute or normalized path to the Java source file. |
| `packageName` | Java package declared in the file. |
| `imports` | Imported types or packages. |

---

## Elements

Each entry in `elements` represents a top-level Java type.

### Class element

```json
{
  "name": "UserService",
  "qualifiedName": "com.example.service.UserService",
  "kind": "class",
  "packageName": "com.example.service",
  "filePath": "/path/to/UserService.java",
  "modifiers": ["public"],
  "annotations": ["@ServiceComponent(name = \"userService\")"],
  "extendsTypes": [],
  "implementsTypes": [],
  "fields": [],
  "methods": []
}
```

### Annotation declaration element

Java annotation declarations are represented with `kind = "annotation"`.

```json
{
  "name": "Audit",
  "qualifiedName": "com.example.service.Audit",
  "kind": "annotation",
  "packageName": "com.example.service",
  "filePath": "/path/to/Audit.java",
  "modifiers": ["public"],
  "annotations": ["@Retention(RetentionPolicy.RUNTIME)"],
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
  "annotations": ["@InjectDependency"]
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
  "resolvedReturnType": "java.lang.String",
  "modifiers": ["public"],
  "annotations": ["@Audit(action = \"read\")"],
  "parameters": [],
  "calls": [],
  "localVariables": [],
  "body": []
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
  "resolvedReturnType": "void",
  "modifiers": ["public"],
  "annotations": [],
  "parameters": [],
  "calls": [],
  "localVariables": [],
  "body": []
}
```

### Parameter element

```json
{
  "name": "id",
  "type": "Long",
  "resolvedType": "java.lang.Long",
  "annotations": []
}
```

---

## Local variables

Local variables are extracted from method and constructor bodies.

Example Java:

```java
String name = repository.findNameById(id);
```

Example JSON:

```json
{
  "name": "name",
  "type": "String",
  "resolvedType": "java.lang.String",
  "assignedValue": "repository.findNameById(id)",
  "assignedType": "java.lang.String",
  "valueKind": "method_call",
  "valueType": "java.lang.String",
  "line": 32,
  "typeResolution": "declared_type_matches_value_type"
}
```

For object creation:

```java
List<String> labels = new ArrayList<>();
```

the JSON preserves the original source expression in `assignedValue`, while normalizing semantic constructor information in body statements:

```json
{
  "name": "labels",
  "type": "List<String>",
  "resolvedType": "java.util.List<java.lang.String>",
  "assignedValue": "new ArrayList<>()",
  "assignedType": "ArrayList",
  "valueKind": "object_creation",
  "valueType": "java.util.ArrayList",
  "line": 94,
  "typeResolution": "value_type_resolved"
}
```

Variables introduced by `foreach` loops may not have an explicit initializer, because they are bound by the iterable expression rather than by a direct assignment.

---

## Body statements

Each method or constructor can contain a `body` array with normalized statements and control structures.

### Assignment

```json
{
  "type": "statement",
  "statementType": "assignment",
  "value": "repository.findNameById(id)",
  "targets": ["name"],
  "valueCall": {
    "methodName": "findNameById",
    "scope": "repository",
    "arguments": ["id"],
    "resolvedTarget": "com.example.repository.UserRepository.findNameById(java.lang.Long)",
    "targetId": "com.example.repository.UserRepository.findNameById(java.lang.Long)",
    "classification": "method_call",
    "kind": "call"
  },
  "lineStart": 32,
  "lineEnd": 32
}
```

### Return

```json
{
  "type": "statement",
  "statementType": "return",
  "value": "name",
  "targets": [],
  "lineStart": 39,
  "lineEnd": 39
}
```

### Method call statement

```json
{
  "type": "statement",
  "statementType": "call",
  "value": "users.put(id, normalizedName)",
  "valueCall": {
    "methodName": "put",
    "scope": "users",
    "argumentCount": 2,
    "arguments": ["id", "normalizedName"],
    "resolvedTarget": "java.util.Map.put(K,V)",
    "targetId": "java.util.Map.put(K,V)",
    "classification": "method_call",
    "kind": "call"
  },
  "lineStart": 53,
  "lineEnd": 53
}
```

### Object creation

```json
{
  "type": "statement",
  "statementType": "assignment",
  "value": "new ArrayList<>()",
  "targets": ["labels"],
  "valueKind": "object_creation",
  "className": "ArrayList",
  "valueCall": {
    "methodName": "ArrayList",
    "name": "ArrayList",
    "scope": null,
    "argumentCount": 0,
    "arguments": [],
    "resolvedTarget": "java.util.ArrayList.<init>(0)",
    "targetId": "java.util.ArrayList.<init>(0)",
    "classification": "constructor",
    "kind": "constructor_call"
  },
  "lineStart": 94,
  "lineEnd": 94
}
```

### Throw

```json
{
  "type": "statement",
  "statementType": "throw",
  "value": "new IllegalArgumentException(\"id cannot be null\")",
  "valueKind": "object_creation",
  "className": "IllegalArgumentException",
  "exceptionType": "IllegalArgumentException",
  "exceptionCalls": [
    {
      "methodName": "IllegalArgumentException",
      "resolvedTarget": "java.lang.IllegalArgumentException.<init>(1)",
      "classification": "constructor",
      "kind": "constructor_call"
    }
  ],
  "lineStart": 69,
  "lineEnd": 69
}
```

---

## Control structures

Control structures are represented as nested body elements with `type = "control_structure"`.

### If

```json
{
  "type": "control_structure",
  "controlType": "if",
  "condition": "name == null",
  "body": [],
  "elseBody": [],
  "lineStart": 34,
  "lineEnd": 36
}
```

### For / foreach / while / switch

The following values are currently used in `controlType`:

```text
if
for
foreach
while
do
switch
try
catch
```

For example:

```json
{
  "type": "control_structure",
  "controlType": "foreach",
  "condition": "String name : names",
  "body": [],
  "lineStart": 96,
  "lineEnd": 103
}
```

A `switch` statement stores its selector in both `condition` and `selector` when possible:

```json
{
  "type": "control_structure",
  "controlType": "switch",
  "condition": "length",
  "selector": "length",
  "body": []
}
```

---

## Try/catch and exceptions

Try/catch blocks are represented as control structures with nested bodies and catch clauses.

```json
{
  "type": "control_structure",
  "controlType": "try",
  "body": [],
  "catchClauses": [
    {
      "type": "exception_handler",
      "controlType": "catch",
      "exceptionType": "RuntimeException",
      "parameterName": "exception",
      "body": [],
      "lineStart": 106,
      "lineEnd": 108
    }
  ],
  "finallyBody": [],
  "lineStart": 98,
  "lineEnd": 108
}
```

This structure is intended to support downstream KDM generation of:

```text
TryUnit
CatchUnit
FinallyUnit
Throws
ExceptionFlow
ExitFlow
```

---

## Calls

Method calls are represented in two places:

1. In the method-level `calls` array.
2. Inside body statements through `valueCall`, `valueCalls`, `conditionCalls`, and `exceptionCalls`.

Example:

```json
{
  "methodName": "findNameById",
  "name": "findNameById",
  "scope": "repository",
  "argumentCount": 1,
  "arguments": ["id"],
  "resolvedTarget": "com.example.repository.UserRepository.findNameById(java.lang.Long)",
  "targetId": "com.example.repository.UserRepository.findNameById(java.lang.Long)",
  "classification": "method_call",
  "kind": "call",
  "lineStart": 32,
  "lineEnd": 32
}
```

Call classification values include:

```text
method_call
constructor
```

Call kind values include:

```text
call
constructor_call
```

---

## Relationships

Relationships represent links between source elements.

### Import relationship

```json
{
  "type": "imports",
  "source": "/path/to/UserService.java",
  "target": "com.example.repository.UserRepository",
  "sourceFile": "/path/to/UserService.java",
  "line": 3
}
```

### Type usage relationship

```json
{
  "type": "uses_type",
  "source": "com.example.service.UserService.repository",
  "target": "com.example.repository.UserRepository",
  "sourceFile": "/path/to/UserService.java",
  "line": 13
}
```

### Call relationship

```json
{
  "type": "calls",
  "source": "com.example.service.UserService.getUserName(java.lang.Long)",
  "target": "com.example.repository.UserRepository.findNameById(java.lang.Long)",
  "sourceFile": "/path/to/UserService.java",
  "line": 32
}
```

---

## Type and call resolution

`java2kdm` uses JavaParser Symbol Solver to resolve types, method calls, and constructor calls when possible.

Examples:

```text
String                  → java.lang.String
UserRepository          → com.example.repository.UserRepository
new ArrayList<>()       → java.util.ArrayList.<init>(0)
new HashMap<>()         → java.util.HashMap.<init>(0)
new IllegalArgumentException(...) → java.lang.IllegalArgumentException.<init>(1)
```

If resolution is not possible, the extractor preserves the best available source-level information.

---

## JSON validation

If a schema path is provided, the generated model is validated after export.

```bash
java -jar target/java2kdm-1.0-SNAPSHOT.jar \
  /path/to/project \
  output/model.json \
  schemas/static_code_model.schema.json
```

Expected success message:

```text
JSON schema validation: OK
[java2kdm] Schema validation completed successfully.
```

---

## Integration with py2kdm

`java2kdm` can be used by `py2kdm` as the Java front-end extractor.

Recommended location inside `py2kdm`:

```text
py2kdm/
└── tools/
    └── java2kdm/
        └── java2kdm-1.0-SNAPSHOT.jar
```

Example workflow:

```bash
cd java2kdm
mvn clean package

cp target/java2kdm-1.0-SNAPSHOT.jar \
  /path/to/py2kdm/tools/java2kdm/java2kdm-1.0-SNAPSHOT.jar

cd /path/to/py2kdm
python run_pipeline.py --config configs/demo_java_project.json
```

Example `py2kdm` configuration fragment:

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

---

## Current validation status

The current version has been tested with a demo Java project containing service and repository classes, custom annotations, method calls, local variables, object creations, control structures, and exception handling.

In the current demo output, the extractor produces:

```text
Files: 10
Elements: 10
Methods/constructors: 18
Local variables: 26
Relationships: 119
Assignments: 34
Returns: 28
Call statements: 12
Throws: 5
Object creations: 9
Try blocks: 2
Catch clauses: 2
If structures: 16
Foreach structures: 3
Switch structures: 2
For structures: 1
While structures: 1
```

The JSON validates successfully against the schema used by the downstream pipeline and is ready to be consumed by the KDM generator.

---

## Known limitations

The current extractor is intentionally static. It does not execute the analyzed Java project.

Known limitations include:

- It does not infer runtime-only behavior.
- Symbol resolution depends on the availability of source files and classpath configuration.
- Generic type compatibility is partially normalized, but some semantic distinctions are intentionally preserved as source expressions.
- Framework-specific semantics are not yet fully interpreted. For example, Spring components, JPA entities, controllers, repositories, services, and dependency injection annotations are extracted as annotations, but not yet lifted into framework-specific architectural concepts.
- `foreach` variables are extracted as local variables, but their implicit binding to the iterable can be further enriched.
- The extractor currently produces generic JSON relationships. Some relations such as reads, writes, creates, throws, and exception flows are expected to be derived by downstream KDM mapping.

---

## Recommended next milestones

### Milestone 1: Complete KDM mapping of Java behavior

Use the enriched Java JSON to generate native KDM relations and elements:

```text
Reads
Writes
Creates
Throws
TryUnit
CatchUnit
FinallyUnit
ExceptionFlow
ExitFlow
```

### Milestone 2: Enrich foreach variable binding

For variables introduced by enhanced for loops, add explicit semantic information such as:

```json
{
  "valueKind": "foreach_variable",
  "assignedValue": "names"
}
```

### Milestone 3: Improve framework-level extraction

Detect and classify framework concepts, for example:

```text
Spring component
Repository
Service
Controller
JPA entity
Dependency injection
Query method
Command method
```

### Milestone 4: Improve external library and classpath support

Improve resolution for external dependencies by integrating Maven classpath information more deeply into symbol solver configuration.

### Milestone 5: Add regression tests

Add tests for:

```text
classes
annotations
fields
methods
constructors
local variables
assignments
returns
method calls
object creations
if / for / foreach / while / switch
try / catch / throw
schema validation
```

---

## Development notes

The extractor should continue to produce a generic intermediate model, not a Java-only output format.

Good:

```json
{
  "kind": "class",
  "methods": [],
  "relationships": []
}
```

Avoid making downstream tools depend on Java-only field names when a generic equivalent is possible.

Preferred architecture:

```text
JavaParser AST
   ↓
Java-specific extraction logic
   ↓
Generic static code model
   ↓
JSON Schema validation
   ↓
KDM mapping or another downstream model transformation
```

---

## License

Add the project license here.
