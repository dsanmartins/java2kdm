package cl.ucn.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonExporter {

    private final ObjectMapper mapper;

    public JsonExporter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void export(Object model, String outputPath) throws IOException {
        Path path = Path.of(outputPath);

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        mapper.writeValue(path.toFile(), model);
    }
}