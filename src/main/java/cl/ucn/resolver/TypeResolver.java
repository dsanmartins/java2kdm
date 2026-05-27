package cl.ucn.resolver;

import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;

public class TypeResolver {

    public String resolveType(Type type) {
        try {
            ResolvedType resolvedType = type.resolve();
            return resolvedType.describe();
        } catch (Exception e) {
            return type.asString();
        }
    }
}