package cl.ucn.main;

import cl.ucn.exporter.JsonExporter;
import cl.ucn.extractor.JavaModelExtractor;
import cl.ucn.model.ProjectModel;
import cl.ucn.resolver.JavaSymbolSolverConfig;
import cl.ucn.scanner.ProjectScanner;
import cl.ucn.validator.JsonSchemaValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final int EXIT_OK = 0;
    private static final int EXIT_INVALID_ARGUMENTS = 1;
    private static final int EXIT_EXTRACTION_ERROR = 2;
    private static final int EXIT_VALIDATION_ERROR = 3;
    private static final int EXIT_UNEXPECTED_ERROR = 4;

    public static void main(String[] args) {
        try {
            int exitCode = run(args);
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("[java2kdm] Unexpected error:");
            e.printStackTrace(System.err);
            System.exit(EXIT_UNEXPECTED_ERROR);
        }
    }

    private static int run(String[] args) {
        if (args.length < 2 || args.length > 3) {
            printUsage();
            return EXIT_INVALID_ARGUMENTS;
        }

        Path inputProjectPath = Path.of(args[0]).toAbsolutePath().normalize();
        Path outputJsonPath = Path.of(args[1]).toAbsolutePath().normalize();

        Path schemaPath = null;
        if (args.length == 3) {
            schemaPath = Path.of(args[2]).toAbsolutePath().normalize();
        }

        try {
            validateInputProject(inputProjectPath);

            if (schemaPath != null) {
                validateSchemaFile(schemaPath);
            }

            Path outputParent = outputJsonPath.getParent();
            if (outputParent != null) {
                Files.createDirectories(outputParent);
            }

            System.out.println("[java2kdm] Input project: " + inputProjectPath);
            System.out.println("[java2kdm] Output JSON: " + outputJsonPath);

            if (schemaPath != null) {
                System.out.println("[java2kdm] Schema: " + schemaPath);
            }

            JavaSymbolSolverConfig.configure(inputProjectPath);

            ProjectScanner scanner = new ProjectScanner();
            List<Path> javaFiles = scanner.findJavaFiles(inputProjectPath);

            if (javaFiles.isEmpty()) {
                System.err.println("[java2kdm] No Java files were found in: " + inputProjectPath);
                return EXIT_EXTRACTION_ERROR;
            }

            String projectName = inputProjectPath.getFileName().toString();

            JavaModelExtractor extractor = new JavaModelExtractor();
            ProjectModel projectModel = extractor.extract(projectName, javaFiles);

            JsonExporter exporter = new JsonExporter();
            exporter.export(projectModel, outputJsonPath.toString());

            System.out.println("[java2kdm] Extraction completed.");
            System.out.println("[java2kdm] Java files found: " + javaFiles.size());
            System.out.println("[java2kdm] Model generated at: " + outputJsonPath);

            if (schemaPath != null) {
                JsonSchemaValidator validator = new JsonSchemaValidator();

                boolean valid = validator.validate(
                        outputJsonPath.toString(),
                        schemaPath.toString()
                );

                if (!valid) {
                    System.err.println("[java2kdm] Schema validation failed.");
                    return EXIT_VALIDATION_ERROR;
                }

                System.out.println("[java2kdm] Schema validation completed successfully.");
            }

            return EXIT_OK;

        } catch (IllegalArgumentException e) {
            System.err.println("[java2kdm] Invalid input:");
            System.err.println("[java2kdm] " + e.getMessage());
            return EXIT_INVALID_ARGUMENTS;

        } catch (Exception e) {
            System.err.println("[java2kdm] Extraction failed:");
            e.printStackTrace(System.err);
            return EXIT_EXTRACTION_ERROR;
        }
    }

    private static void validateInputProject(Path inputProjectPath) {
        if (!Files.exists(inputProjectPath)) {
            throw new IllegalArgumentException(
                    "Input project path does not exist: " + inputProjectPath
            );
        }

        if (!Files.isDirectory(inputProjectPath)) {
            throw new IllegalArgumentException(
                    "Input project path is not a directory: " + inputProjectPath
            );
        }

        if (!Files.isReadable(inputProjectPath)) {
            throw new IllegalArgumentException(
                    "Input project path is not readable: " + inputProjectPath
            );
        }
    }

    private static void validateSchemaFile(Path schemaPath) {
        if (!Files.exists(schemaPath)) {
            throw new IllegalArgumentException(
                    "Schema file does not exist: " + schemaPath
            );
        }

        if (!Files.isRegularFile(schemaPath)) {
            throw new IllegalArgumentException(
                    "Schema path is not a regular file: " + schemaPath
            );
        }

        if (!Files.isReadable(schemaPath)) {
            throw new IllegalArgumentException(
                    "Schema file is not readable: " + schemaPath
            );
        }
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  java -jar java2kdm.jar <input-project-path> <output-json-path> [schema-path]");
        System.err.println();
        System.err.println("Arguments:");
        System.err.println("  <input-project-path>   Path to the Java project to analyze.");
        System.err.println("  <output-json-path>     Path where the generated JSON model will be written.");
        System.err.println("  [schema-path]          Optional path to the JSON schema used for validation.");
        System.err.println();
        System.err.println("Exit codes:");
        System.err.println("  0  OK");
        System.err.println("  1  Invalid arguments or invalid input path");
        System.err.println("  2  Extraction error");
        System.err.println("  3  Schema validation error");
        System.err.println("  4  Unexpected error");
        System.err.println();
        System.err.println("Example:");
        System.err.println("  java -jar java2kdm.jar \\");
        System.err.println("    /path/to/java/project \\");
        System.err.println("    /path/to/output/model.json \\");
        System.err.println("    /path/to/schemas/static_code_model.schema.json");
    }
}