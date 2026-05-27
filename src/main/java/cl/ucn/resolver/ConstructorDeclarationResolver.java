package cl.ucn.resolver;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConstructorDeclarationResolver {

    public String resolveQualifiedSignature(
            ConstructorDeclaration constructor,
            String fallbackOwnerQualifiedName,
            String fallbackSignature
    ) {
        try {
            ResolvedConstructorDeclaration resolved = constructor.resolve();

            String ownerQualifiedName = resolved.declaringType().getQualifiedName();

            String parameterTypes = IntStream.range(0, resolved.getNumberOfParams())
                    .mapToObj(i -> resolved.getParam(i).getType().describe())
                    .collect(Collectors.joining(","));

            return ownerQualifiedName + ".<init>(" + parameterTypes + ")";

        } catch (Exception e) {
            return fallbackOwnerQualifiedName + "." + fallbackSignature;
        }
    }
}