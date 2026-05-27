package cl.ucn.resolver;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MethodDeclarationResolver {

    public String resolveQualifiedSignature(
            MethodDeclaration method,
            String fallbackOwnerQualifiedName,
            String fallbackSignature
    ) {
        try {
            ResolvedMethodDeclaration resolved = method.resolve();

            String ownerQualifiedName = resolved.declaringType().getQualifiedName();
            String methodName = resolved.getName();

            String parameterTypes = IntStream.range(0, resolved.getNumberOfParams())
                    .mapToObj(i -> resolved.getParam(i).getType().describe())
                    .collect(Collectors.joining(","));

            return ownerQualifiedName + "." + methodName + "(" + parameterTypes + ")";

        } catch (Exception e) {
            return fallbackOwnerQualifiedName + "." + fallbackSignature;
        }
    }
}