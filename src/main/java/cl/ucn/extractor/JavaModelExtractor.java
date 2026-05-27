package cl.ucn.extractor;

import cl.ucn.model.*;
import cl.ucn.resolver.ConstructorDeclarationResolver;
import cl.ucn.resolver.MethodCallResolver;
import cl.ucn.resolver.MethodDeclarationResolver;
import cl.ucn.resolver.TypeResolver;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JavaModelExtractor {

    private final MethodCallResolver methodCallResolver = new MethodCallResolver();

    private final MethodDeclarationResolver methodDeclarationResolver =
            new MethodDeclarationResolver();

    private final ConstructorDeclarationResolver constructorDeclarationResolver =
            new ConstructorDeclarationResolver();

    private final TypeResolver typeResolver = new TypeResolver();

    public ProjectModel extract(String projectName, List<Path> javaFiles) throws IOException {
        ProjectModel projectModel = new ProjectModel(projectName);

        for (Path file : javaFiles) {
            CompilationUnit cu = StaticJavaParser.parse(file);

            String packageName = cu.getPackageDeclaration()
                    .map(pkg -> pkg.getNameAsString())
                    .orElse("");

            SourceFileModel sourceFile = new SourceFileModel(file.toString());
            sourceFile.setPackageName(packageName);

            cu.getImports().forEach(imp -> {
                sourceFile.getImports().add(imp.getNameAsString());

                projectModel.getRelationships().add(
                        new RelationshipModel(
                                "imports",
                                file.toString(),
                                imp.getNameAsString(),
                                file.toString(),
                                imp.getBegin().map(p -> p.line).orElse(null)
                        )
                );
            });

            projectModel.getFiles().add(sourceFile);

            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                TypeModel typeModel = extractType(
                        typeDeclaration,
                        packageName,
                        file.toString(),
                        projectModel
                );

                projectModel.getElements().add(typeModel);
            }
        }

        return projectModel;
    }

    private TypeModel extractType(
            TypeDeclaration<?> typeDeclaration,
            String packageName,
            String filePath,
            ProjectModel projectModel
    ) {
        TypeModel typeModel = new TypeModel();

        typeModel.setName(typeDeclaration.getNameAsString());
        typeModel.setPackageName(packageName);
        typeModel.setFilePath(filePath);

        String qualifiedName = packageName.isBlank()
                ? typeDeclaration.getNameAsString()
                : packageName + "." + typeDeclaration.getNameAsString();

        typeModel.setQualifiedName(qualifiedName);

        typeDeclaration.getModifiers().forEach(modifier ->
                typeModel.getModifiers().add(modifier.getKeyword().asString())
        );

        typeDeclaration.getAnnotations().forEach(annotation ->
                typeModel.getAnnotations().add(annotation.toString())
        );

        if (typeDeclaration instanceof ClassOrInterfaceDeclaration classOrInterface) {
            if (classOrInterface.isInterface()) {
                typeModel.setKind("interface");
            } else {
                typeModel.setKind("class");
            }

            classOrInterface.getExtendedTypes().forEach(parent -> {
                String resolvedParent = typeResolver.resolveType(parent);

                typeModel.getExtendsTypes().add(resolvedParent);

                projectModel.getRelationships().add(
                        new RelationshipModel(
                                "extends",
                                qualifiedName,
                                resolvedParent,
                                filePath,
                                parent.getBegin().map(p -> p.line).orElse(null)
                        )
                );
            });

            classOrInterface.getImplementedTypes().forEach(parent -> {
                String resolvedParent = typeResolver.resolveType(parent);

                typeModel.getImplementsTypes().add(resolvedParent);

                projectModel.getRelationships().add(
                        new RelationshipModel(
                                "implements",
                                qualifiedName,
                                resolvedParent,
                                filePath,
                                parent.getBegin().map(p -> p.line).orElse(null)
                        )
                );
            });

            extractFields(
                    classOrInterface,
                    typeModel,
                    projectModel,
                    qualifiedName,
                    filePath
            );

            extractMethods(
                    classOrInterface,
                    typeModel,
                    projectModel,
                    qualifiedName,
                    filePath
            );

            extractConstructors(
                    classOrInterface,
                    typeModel,
                    projectModel,
                    qualifiedName,
                    filePath
            );
        }

        if (typeDeclaration instanceof EnumDeclaration enumDeclaration) {
            typeModel.setKind("enum");
            extractEnumFieldsAndMethods(enumDeclaration, typeModel, projectModel, qualifiedName, filePath);
        }

        if (typeDeclaration instanceof AnnotationDeclaration) {
            typeModel.setKind("annotation");
        }

        return typeModel;
    }

    private void extractFields(
            ClassOrInterfaceDeclaration declaration,
            TypeModel typeModel,
            ProjectModel projectModel,
            String ownerQualifiedName,
            String filePath
    ) {
        for (FieldDeclaration field : declaration.getFields()) {
            String fieldType = field.getElementType().asString();
            String resolvedFieldType = typeResolver.resolveType(field.getElementType());

            for (VariableDeclarator variable : field.getVariables()) {
                FieldModel fieldModel = new FieldModel(
                        variable.getNameAsString(),
                        fieldType
                );

                fieldModel.setResolvedType(resolvedFieldType);

                field.getModifiers().forEach(modifier ->
                        fieldModel.getModifiers().add(modifier.getKeyword().asString())
                );

                field.getAnnotations().forEach(annotation ->
                        fieldModel.getAnnotations().add(annotation.toString())
                );

                typeModel.getFields().add(fieldModel);

                projectModel.getRelationships().add(
                        new RelationshipModel(
                                "uses_type",
                                ownerQualifiedName + "." + variable.getNameAsString(),
                                resolvedFieldType,
                                filePath,
                                variable.getBegin().map(p -> p.line).orElse(null)
                        )
                );
            }
        }
    }

    private void extractMethods(
            ClassOrInterfaceDeclaration declaration,
            TypeModel typeModel,
            ProjectModel projectModel,
            String ownerQualifiedName,
            String filePath
    ) {
        for (MethodDeclaration method : declaration.getMethods()) {
            MethodModel methodModel = new MethodModel();

            methodModel.setName(method.getNameAsString());
            methodModel.setReturnType(method.getTypeAsString());
            methodModel.setResolvedReturnType(typeResolver.resolveType(method.getType()));
            methodModel.setKind("method");

            method.getModifiers().forEach(modifier ->
                    methodModel.getModifiers().add(modifier.getKeyword().asString())
            );

            method.getAnnotations().forEach(annotation ->
                    methodModel.getAnnotations().add(annotation.toString())
            );

            method.getParameters().forEach(parameter ->
                    methodModel.getParameters().add(
                            new ParameterModel(
                                    parameter.getNameAsString(),
                                    parameter.getTypeAsString(),
                                    typeResolver.resolveType(parameter.getType())
                            )
                    )
            );

            methodModel.setSignature(buildSignature(
                    methodModel.getName(),
                    methodModel.getParameters()
            ));

            String sourceMethod = methodDeclarationResolver.resolveQualifiedSignature(
                    method,
                    ownerQualifiedName,
                    methodModel.getSignature()
            );

            methodModel.setQualifiedSignature(sourceMethod);

            addMethodTypeRelationships(
                    methodModel,
                    sourceMethod,
                    filePath,
                    method.getBegin().map(p -> p.line).orElse(null),
                    projectModel
            );

            extractCallableBody(
                    method,
                    methodModel,
                    sourceMethod,
                    filePath,
                    projectModel
            );

            typeModel.getMethods().add(methodModel);
        }
    }

    private void extractConstructors(
            ClassOrInterfaceDeclaration declaration,
            TypeModel typeModel,
            ProjectModel projectModel,
            String ownerQualifiedName,
            String filePath
    ) {
        for (ConstructorDeclaration constructor : declaration.getConstructors()) {
            MethodModel constructorModel = new MethodModel();

            constructorModel.setName(constructor.getNameAsString());
            constructorModel.setReturnType("void");
            constructorModel.setResolvedReturnType("void");
            constructorModel.setKind("constructor");

            constructor.getModifiers().forEach(modifier ->
                    constructorModel.getModifiers().add(modifier.getKeyword().asString())
            );

            constructor.getAnnotations().forEach(annotation ->
                    constructorModel.getAnnotations().add(annotation.toString())
            );

            constructor.getParameters().forEach(parameter ->
                    constructorModel.getParameters().add(
                            new ParameterModel(
                                    parameter.getNameAsString(),
                                    parameter.getTypeAsString(),
                                    typeResolver.resolveType(parameter.getType())
                            )
                    )
            );

            constructorModel.setSignature(buildSignature(
                    constructorModel.getName(),
                    constructorModel.getParameters()
            ));

            String qualifiedSignature =
                    constructorDeclarationResolver.resolveQualifiedSignature(
                            constructor,
                            ownerQualifiedName,
                            constructorModel.getSignature()
                    );

            constructorModel.setQualifiedSignature(qualifiedSignature);

            addMethodTypeRelationships(
                    constructorModel,
                    qualifiedSignature,
                    filePath,
                    constructor.getBegin().map(p -> p.line).orElse(null),
                    projectModel
            );

            extractCallableBody(
                    constructor,
                    constructorModel,
                    qualifiedSignature,
                    filePath,
                    projectModel
            );

            typeModel.getMethods().add(constructorModel);
        }
    }

    private void extractEnumFieldsAndMethods(
            EnumDeclaration declaration,
            TypeModel typeModel,
            ProjectModel projectModel,
            String ownerQualifiedName,
            String filePath
    ) {
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof MethodDeclaration method) {
                MethodModel methodModel = new MethodModel();

                methodModel.setName(method.getNameAsString());
                methodModel.setReturnType(method.getTypeAsString());
                methodModel.setResolvedReturnType(typeResolver.resolveType(method.getType()));
                methodModel.setKind("method");

                method.getParameters().forEach(parameter ->
                        methodModel.getParameters().add(
                                new ParameterModel(
                                        parameter.getNameAsString(),
                                        parameter.getTypeAsString(),
                                        typeResolver.resolveType(parameter.getType())
                                )
                        )
                );

                methodModel.setSignature(buildSignature(
                        methodModel.getName(),
                        methodModel.getParameters()
                ));

                String sourceMethod = methodDeclarationResolver.resolveQualifiedSignature(
                        method,
                        ownerQualifiedName,
                        methodModel.getSignature()
                );

                methodModel.setQualifiedSignature(sourceMethod);

                extractCallableBody(method, methodModel, sourceMethod, filePath, projectModel);

                typeModel.getMethods().add(methodModel);
            }
        }
    }

    private void extractCallableBody(
            Node callable,
            MethodModel methodModel,
            String qualifiedSignature,
            String filePath,
            ProjectModel projectModel
    ) {
        callable.findAll(VariableDeclarator.class).forEach(variable -> {
            if (variable.findAncestor(FieldDeclaration.class).isPresent()) {
                return;
            }

            LocalVariableModel localVariable = new LocalVariableModel(
                    variable.getNameAsString(),
                    variable.getType().asString(),
                    typeResolver.resolveType(variable.getType()),
                    variable.getBegin().map(p -> p.line).orElse(null)
            );

            enrichLocalVariableInitializer(localVariable, variable);

            methodModel.getLocalVariables().add(localVariable);

            projectModel.getRelationships().add(
                    new RelationshipModel(
                            "uses_type",
                            qualifiedSignature + ":local:" + variable.getNameAsString(),
                            localVariable.getResolvedType(),
                            filePath,
                            localVariable.getLine()
                    )
            );
        });

        callable.findAll(MethodCallExpr.class).forEach(call -> {
            CallModel callModel = createCallModel(call);
            methodModel.getCalls().add(callModel);

            projectModel.getRelationships().add(
                    new RelationshipModel(
                            "calls",
                            qualifiedSignature,
                            callModel.getTargetId(),
                            filePath,
                            call.getBegin().map(p -> p.line).orElse(null)
                    )
            );
        });

        if (callable instanceof MethodDeclaration method) {
            method.getBody().ifPresent(block ->
                    block.getStatements().forEach(statement ->
                            methodModel.getBody().add(extractStatement(statement))
                    )
            );
        }

        if (callable instanceof ConstructorDeclaration constructor) {
            constructor.getBody().getStatements().forEach(statement ->
                    methodModel.getBody().add(extractStatement(statement))
            );
        }
    }

    private BodyStatementModel extractStatement(Statement statement) {
        if (statement.isBlockStmt()) {
            BodyStatementModel block = createBaseBodyNode(statement, "control_structure", "block");
            statement.asBlockStmt().getStatements().forEach(child ->
                    block.getBody().add(extractStatement(child))
            );
            return block;
        }

        if (statement.isExpressionStmt()) {
            return extractExpressionStatement(statement.asExpressionStmt());
        }

        if (statement.isReturnStmt()) {
            return extractReturnStatement(statement.asReturnStmt());
        }

        if (statement.isThrowStmt()) {
            return extractThrowStatement(statement.asThrowStmt());
        }

        if (statement.isIfStmt()) {
            return extractIfStatement(statement.asIfStmt());
        }

        if (statement.isForStmt()) {
            return extractForStatement(statement.asForStmt());
        }

        if (statement.isForEachStmt()) {
            return extractForEachStatement(statement.asForEachStmt());
        }

        if (statement.isWhileStmt()) {
            return extractWhileStatement(statement.asWhileStmt());
        }

        if (statement.isDoStmt()) {
            return extractDoStatement(statement.asDoStmt());
        }

        if (statement.isSwitchStmt()) {
            return extractSwitchStatement(statement.asSwitchStmt());
        }

        if (statement.isTryStmt()) {
            return extractTryStatement(statement.asTryStmt());
        }

        BodyStatementModel generic = createBaseStatement(statement, "statement");
        generic.setValue(statement.toString());
        addValueCalls(generic, statement);
        return generic;
    }

    private BodyStatementModel extractExpressionStatement(ExpressionStmt statement) {
        Expression expression = statement.getExpression();

        if (expression.isAssignExpr()) {
            AssignExpr assignment = expression.asAssignExpr();
            BodyStatementModel model = createBaseStatement(statement, "assignment");
            model.getTargets().add(assignment.getTarget().toString());
            model.setValue(assignment.getValue().toString());
            addExpressionMetadata(model, assignment.getValue());
            return model;
        }

        if (expression.isVariableDeclarationExpr()) {
            BodyStatementModel model = createBaseStatement(statement, "assignment");
            VariableDeclarationExpr declaration = expression.asVariableDeclarationExpr();

            declaration.getVariables().forEach(variable -> {
                model.getTargets().add(variable.getNameAsString());

                variable.getInitializer().ifPresent(initializer -> {
                    model.setValue(initializer.toString());
                    addExpressionMetadata(model, initializer);
                });
            });

            return model;
        }

        if (expression.isUnaryExpr()) {
            UnaryExpr unary = expression.asUnaryExpr();

            if (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT) {

                BodyStatementModel model = createBaseStatement(statement, "assignment");
                model.getTargets().add(unary.getExpression().toString());
                model.setValue(expression.toString());
                return model;
            }
        }

        if (expression.isMethodCallExpr()) {
            BodyStatementModel model = createBaseStatement(statement, "call");
            model.setValue(expression.toString());
            model.setValueCall(createCallModel(expression.asMethodCallExpr()));
            return model;
        }

        if (expression.isObjectCreationExpr()) {
            ObjectCreationExpr creation = expression.asObjectCreationExpr();
            BodyStatementModel model = createBaseStatement(statement, "object_creation");
            model.setValue(expression.toString());
            model.setValueKind("object_creation");
            model.setClassName(normalizeJavaTypeName(creation.getType().asString()));
            model.setValueCall(createConstructorCallModel(creation));
            return model;
        }

        BodyStatementModel model = createBaseStatement(statement, "statement");
        model.setValue(expression.toString());
        addExpressionMetadata(model, expression);
        return model;
    }

    private BodyStatementModel extractReturnStatement(ReturnStmt statement) {
        BodyStatementModel model = createBaseStatement(statement, "return");

        statement.getExpression().ifPresent(expression -> {
            model.setValue(expression.toString());
            addExpressionMetadata(model, expression);
        });

        return model;
    }

    private BodyStatementModel extractThrowStatement(ThrowStmt statement) {
        BodyStatementModel model = createBaseStatement(statement, "throw");
        model.setValue(statement.getExpression().toString());

        statement.getExpression().findFirst(ObjectCreationExpr.class).ifPresent(creation -> {
            model.setExceptionType(creation.getType().asString());
            model.getExceptionCalls().add(createConstructorCallModel(creation));
        });

        addExpressionMetadata(model, statement.getExpression());
        return model;
    }

    private BodyStatementModel extractIfStatement(IfStmt statement) {
        BodyStatementModel model = createBaseBodyNode(statement, "control_structure", "if");
        model.setCondition(statement.getCondition().toString());
        addConditionCalls(model, statement.getCondition());

        appendStatementToBody(model.getBody(), statement.getThenStmt());

        statement.getElseStmt().ifPresent(elseStatement ->
                appendStatementToBody(model.getElseBody(), elseStatement)
        );

        return model;
    }

    private BodyStatementModel extractForStatement(ForStmt statement) {
        BodyStatementModel model = createBaseBodyNode(statement, "control_structure", "for");
        statement.getCompare().ifPresent(compare -> {
            model.setCondition(compare.toString());
            addConditionCalls(model, compare);
        });

        appendStatementToBody(model.getBody(), statement.getBody());
        return model;
    }

    private BodyStatementModel extractForEachStatement(ForEachStmt statement) {
        BodyStatementModel model = createBaseBodyNode(statement, "control_structure", "foreach");
        model.setCondition(statement.getVariable() + " : " + statement.getIterable());
        addConditionCalls(model, statement.getIterable());
        appendStatementToBody(model.getBody(), statement.getBody());
        return model;
    }

    private BodyStatementModel extractWhileStatement(WhileStmt statement) {
        BodyStatementModel model = createBaseBodyNode(statement, "control_structure", "while");
        model.setCondition(statement.getCondition().toString());
        addConditionCalls(model, statement.getCondition());
        appendStatementToBody(model.getBody(), statement.getBody());
        return model;
    }

    private BodyStatementModel extractDoStatement(DoStmt statement) {
        BodyStatementModel model = createBaseBodyNode(statement, "control_structure", "do");
        model.setCondition(statement.getCondition().toString());
        addConditionCalls(model, statement.getCondition());
        appendStatementToBody(model.getBody(), statement.getBody());
        return model;
    }

    private BodyStatementModel extractSwitchStatement(SwitchStmt statement) {
        BodyStatementModel model = createBaseBodyNode(statement, "control_structure", "switch");
        model.setSelector(statement.getSelector().toString());
        model.setCondition(statement.getSelector().toString());
        addConditionCalls(model, statement.getSelector());

        statement.getEntries().forEach(entry ->
                entry.getStatements().forEach(child ->
                        model.getBody().add(extractStatement(child))
                )
        );

        return model;
    }

    private BodyStatementModel extractTryStatement(TryStmt statement) {
        BodyStatementModel model = createBaseBodyNode(statement, "control_structure", "try");

        statement.getTryBlock().getStatements().forEach(child ->
                model.getBody().add(extractStatement(child))
        );

        statement.getCatchClauses().forEach(catchClause -> {
            BodyStatementModel handler = createBaseBodyNode(catchClause, "exception_handler", "catch");
            handler.setExceptionType(catchClause.getParameter().getType().asString());
            handler.setParameterName(catchClause.getParameter().getNameAsString());

            catchClause.getBody().getStatements().forEach(child ->
                    handler.getBody().add(extractStatement(child))
            );

            model.getCatchClauses().add(handler);
        });

        statement.getFinallyBlock().ifPresent(finallyBlock ->
                finallyBlock.getStatements().forEach(child ->
                        model.getFinallyBody().add(extractStatement(child))
                )
        );

        return model;
    }

    private BodyStatementModel createBaseStatement(Node node, String statementType) {
        BodyStatementModel model = new BodyStatementModel();
        model.setType("statement");
        model.setStatementType(statementType);
        setLines(model, node);
        return model;
    }

    private BodyStatementModel createBaseBodyNode(Node node, String type, String controlType) {
        BodyStatementModel model = new BodyStatementModel();
        model.setType(type);
        model.setControlType(controlType);
        setLines(model, node);
        return model;
    }

    private void setLines(BodyStatementModel model, Node node) {
        model.setLineStart(node.getBegin().map(p -> p.line).orElse(null));
        model.setLineEnd(node.getEnd().map(p -> p.line).orElse(null));
    }

    private void addExpressionMetadata(BodyStatementModel model, Expression expression) {
        expression.findFirst(ObjectCreationExpr.class).ifPresent(creation -> {
            model.setValueKind("object_creation");
            model.setClassName(normalizeJavaTypeName(creation.getType().asString()));
            model.setValueCall(createConstructorCallModel(creation));
        });

        expression.findAll(MethodCallExpr.class).forEach(call -> {
            CallModel callModel = createCallModel(call);
            model.getValueCalls().add(callModel);

            if (model.getValueCall() == null) {
                model.setValueCall(callModel);
            }
        });
    }

    private void addValueCalls(BodyStatementModel model, Node node) {
        node.findAll(MethodCallExpr.class).forEach(call ->
                model.getValueCalls().add(createCallModel(call))
        );
    }

    private void addConditionCalls(BodyStatementModel model, Expression expression) {
        expression.findAll(MethodCallExpr.class).forEach(call ->
                model.getConditionCalls().add(createCallModel(call))
        );
    }

    private void appendStatementToBody(List<BodyStatementModel> targetBody, Statement statement) {
        if (statement.isBlockStmt()) {
            statement.asBlockStmt().getStatements().forEach(child ->
                    targetBody.add(extractStatement(child))
            );
            return;
        }

        targetBody.add(extractStatement(statement));
    }

    private CallModel createCallModel(MethodCallExpr call) {
        String scope = call.getScope()
                .map(Object::toString)
                .orElse(null);

        String target = methodCallResolver.resolveQualifiedTarget(call);

        CallModel callModel = new CallModel(
                call.getNameAsString(),
                scope,
                call.getArguments().size(),
                target
        );

        callModel.setClassification("method_call");
        callModel.setKind("call");
        callModel.setLineStart(call.getBegin().map(p -> p.line).orElse(null));
        callModel.setLineEnd(call.getEnd().map(p -> p.line).orElse(null));

        call.getArguments().forEach(argument ->
                callModel.getArguments().add(argument.toString())
        );

        return callModel;
    }

    private CallModel createConstructorCallModel(ObjectCreationExpr creation) {
        String className = normalizeJavaTypeName(creation.getType().asString());

        String target = resolveConstructorTarget(creation, className);

        CallModel callModel = new CallModel(
                className,
                null,
                creation.getArguments().size(),
                target
        );

        callModel.setClassification("constructor");
        callModel.setKind("constructor_call");
        callModel.setLineStart(creation.getBegin().map(p -> p.line).orElse(null));
        callModel.setLineEnd(creation.getEnd().map(p -> p.line).orElse(null));

        creation.getArguments().forEach(argument ->
                callModel.getArguments().add(argument.toString())
        );

        return callModel;
    }

    private String resolveConstructorTarget(ObjectCreationExpr creation, String fallbackClassName) {
        try {
            String resolvedType = creation.calculateResolvedType().describe();
            String normalizedResolvedType = normalizeJavaTypeName(resolvedType);

            if (normalizedResolvedType != null && !normalizedResolvedType.isBlank()) {
                return normalizedResolvedType + ".<init>(" + creation.getArguments().size() + ")";
            }
        } catch (Exception ignored) {
            // Fall back to the syntactic type below.
        }

        return fallbackClassName + ".<init>(" + creation.getArguments().size() + ")";
    }

    private void enrichLocalVariableInitializer(
            LocalVariableModel localVariable,
            VariableDeclarator variable
    ) {
        variable.getInitializer().ifPresent(initializer -> {
            localVariable.setAssignedValue(initializer.toString());
            localVariable.setValueKind(classifyExpressionKind(initializer));
            localVariable.setValueType(resolveExpressionType(initializer));

            if (initializer.isObjectCreationExpr()) {
                ObjectCreationExpr creation = initializer.asObjectCreationExpr();
                localVariable.setAssignedType(
                        normalizeJavaTypeName(creation.getType().asString())
                );
            } else {
                localVariable.setAssignedType(localVariable.getValueType());
            }

            if (localVariable.getResolvedType() != null
                    && localVariable.getValueType() != null
                    && localVariable.getResolvedType().equals(localVariable.getValueType())) {
                localVariable.setTypeResolution("declared_type_matches_value_type");
            } else if (localVariable.getValueType() != null) {
                localVariable.setTypeResolution("value_type_resolved");
            } else {
                localVariable.setTypeResolution("value_type_unresolved");
            }
        });
    }

    private String classifyExpressionKind(Expression expression) {
        if (expression.isObjectCreationExpr()) {
            return "object_creation";
        }

        if (expression.isMethodCallExpr()) {
            return "method_call";
        }

        if (expression.isLiteralExpr()) {
            return "literal";
        }

        if (expression.isNameExpr() || expression.isFieldAccessExpr()) {
            return "reference";
        }

        if (expression.isBinaryExpr()) {
            return "binary_expression";
        }

        if (expression.isUnaryExpr()) {
            return "unary_expression";
        }

        if (expression.isConditionalExpr()) {
            return "conditional_expression";
        }

        return "expression";
    }

    private String resolveExpressionType(Expression expression) {
        try {
            return normalizeJavaTypeName(expression.calculateResolvedType().describe());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeJavaTypeName(String rawTypeName) {
        if (rawTypeName == null) {
            return null;
        }

        String typeName = rawTypeName.trim();

        if (typeName.isBlank()) {
            return typeName;
        }

        StringBuilder normalized = new StringBuilder();
        int genericDepth = 0;

        for (int i = 0; i < typeName.length(); i++) {
            char character = typeName.charAt(i);

            if (character == '<') {
                genericDepth++;
                continue;
            }

            if (character == '>') {
                genericDepth = Math.max(0, genericDepth - 1);
                continue;
            }

            if (genericDepth == 0) {
                normalized.append(character);
            }
        }

        return normalized.toString()
                .replace("[]", "[]")
                .replace(" ", "")
                .trim();
    }

    private void addMethodTypeRelationships(
            MethodModel methodModel,
            String qualifiedSignature,
            String filePath,
            Integer line,
            ProjectModel projectModel
    ) {
        if (methodModel.getResolvedReturnType() != null
                && !methodModel.getResolvedReturnType().isBlank()
                && !"void".equals(methodModel.getResolvedReturnType())) {

            projectModel.getRelationships().add(
                    new RelationshipModel(
                            "uses_type",
                            qualifiedSignature + ":return",
                            methodModel.getResolvedReturnType(),
                            filePath,
                            line
                    )
            );
        }

        for (ParameterModel parameter : methodModel.getParameters()) {
            if (parameter.getResolvedType() == null
                    || parameter.getResolvedType().isBlank()) {
                continue;
            }

            projectModel.getRelationships().add(
                    new RelationshipModel(
                            "uses_type",
                            qualifiedSignature + ":parameter:" + parameter.getName(),
                            parameter.getResolvedType(),
                            filePath,
                            line
                    )
            );
        }
    }

    private String buildSignature(String methodName, List<ParameterModel> parameters) {
        String parameterTypes = parameters.stream()
                .map(ParameterModel::getType)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        return methodName + "(" + parameterTypes + ")";
    }
}
