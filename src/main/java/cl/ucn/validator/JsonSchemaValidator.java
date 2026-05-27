package cl.ucn.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class JsonSchemaValidator {

    private final ObjectMapper mapper;

    public JsonSchemaValidator() {
        this.mapper = new ObjectMapper();
    }

    public boolean validate(String jsonPath, String schemaPath) throws IOException {
        Path jsonFile = Path.of(jsonPath);
        Path schemaFile = Path.of(schemaPath);

        validateReadableFile(jsonFile, "JSON model");
        validateReadableFile(schemaFile, "JSON schema");

        JsonNode schemaNode = mapper.readTree(schemaFile.toFile());
        JsonNode jsonNode = mapper.readTree(jsonFile.toFile());

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012
        );

        JsonSchema schema = factory.getSchema(schemaNode);

        Set<ValidationMessage> errors = schema.validate(jsonNode);

        if (errors.isEmpty()) {
            System.out.println("JSON schema validation: OK");
            return true;
        }

        System.err.println("JSON schema validation: FAILED");

        for (ValidationMessage error : errors) {
            System.err.println("- " + error.getMessage());
        }

        return false;
    }

    private void validateReadableFile(Path path, String label) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException(label + " does not exist: " + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new IOException(label + " is not a regular file: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new IOException(label + " is not readable: " + path);
        }
    }
}