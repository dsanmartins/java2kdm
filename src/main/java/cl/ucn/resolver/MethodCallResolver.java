package cl.ucn.resolver;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MethodCallResolver {

    public String resolveQualifiedTarget(MethodCallExpr call) {
        try {
            ResolvedMethodDeclaration resolved = call.resolve();

            String qualifiedClassName = resolved.declaringType().getQualifiedName();
            String methodName = resolved.getName();

            String parameterTypes = IntStream.range(0, resolved.getNumberOfParams())
                    .mapToObj(i -> resolved.getParam(i).getType().describe())
                    .collect(Collectors.joining(","));

            return qualifiedClassName + "." + methodName + "(" + parameterTypes + ")";

        } catch (Exception e) {
            return buildFallbackTarget(call);
        }
    }

    private String buildFallbackTarget(MethodCallExpr call) {
        String scope = call.getScope()
                .map(Object::toString)
                .orElse(null);

        if (scope == null || scope.isBlank()) {
            return call.getNameAsString();
        }

        return scope + "." + call.getNameAsString();
    }
}