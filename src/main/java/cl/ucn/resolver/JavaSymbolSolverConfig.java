package cl.ucn.resolver;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.nio.file.Path;

public class JavaSymbolSolverConfig {

    public static void configure(Path projectRoot) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

        /*
         * Resolves JDK classes such as String, Long, List, Map, etc.
         */
        combinedTypeSolver.add(new ReflectionTypeSolver());

        /*
         * Resolves classes declared inside the analyzed Java project.
         * For Maven projects, this should usually point to src/main/java.
         */
        Path sourceRoot = projectRoot.resolve("src/main/java");

        if (sourceRoot.toFile().exists()) {
            combinedTypeSolver.add(new JavaParserTypeSolver(sourceRoot));
        } else {
            combinedTypeSolver.add(new JavaParserTypeSolver(projectRoot));
        }

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(symbolSolver);

        StaticJavaParser.setConfiguration(parserConfiguration);
    }
}