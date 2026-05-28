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
import java.util.*;

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

        enrichProjectModel(projectModel);

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
        typeModel.setLineStart(typeDeclaration.getBegin().map(p -> p.line).orElse(null));
        typeModel.setLineEnd(typeDeclaration.getEnd().map(p -> p.line).orElse(null));

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
                fieldModel.setResolvedRawType(extractRawType(resolvedFieldType));
                fieldModel.setResolvedTypeArguments(extractTypeArguments(resolvedFieldType));
                fieldModel.setLineStart(variable.getBegin().map(p -> p.line).orElse(field.getBegin().map(p -> p.line).orElse(null)));
                fieldModel.setLineEnd(variable.getEnd().map(p -> p.line).orElse(field.getEnd().map(p -> p.line).orElse(null)));

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
            methodModel.setResolvedRawReturnType(extractRawType(methodModel.getResolvedReturnType()));
            methodModel.setResolvedReturnTypeArguments(extractTypeArguments(methodModel.getResolvedReturnType()));
            methodModel.setLineStart(method.getBegin().map(p -> p.line).orElse(null));
            methodModel.setLineEnd(method.getEnd().map(p -> p.line).orElse(null));
            methodModel.setKind("method");

            method.getModifiers().forEach(modifier ->
                    methodModel.getModifiers().add(modifier.getKeyword().asString())
            );

            method.getAnnotations().forEach(annotation ->
                    methodModel.getAnnotations().add(annotation.toString())
            );

            method.getParameters().forEach(parameter ->
                    methodModel.getParameters().add(
                            createParameterModel(parameter)
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
                    ownerQualifiedName,
                    typeModel.getFields(),
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
            constructorModel.setResolvedRawReturnType("void");
            constructorModel.setLineStart(constructor.getBegin().map(p -> p.line).orElse(null));
            constructorModel.setLineEnd(constructor.getEnd().map(p -> p.line).orElse(null));
            constructorModel.setKind("constructor");

            constructor.getModifiers().forEach(modifier ->
                    constructorModel.getModifiers().add(modifier.getKeyword().asString())
            );

            constructor.getAnnotations().forEach(annotation ->
                    constructorModel.getAnnotations().add(annotation.toString())
            );

            constructor.getParameters().forEach(parameter ->
                    constructorModel.getParameters().add(
                            createParameterModel(parameter)
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
                    ownerQualifiedName,
                    typeModel.getFields(),
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
                methodModel.setResolvedRawReturnType(extractRawType(methodModel.getResolvedReturnType()));
                methodModel.setResolvedReturnTypeArguments(extractTypeArguments(methodModel.getResolvedReturnType()));
                methodModel.setLineStart(method.getBegin().map(p -> p.line).orElse(null));
                methodModel.setLineEnd(method.getEnd().map(p -> p.line).orElse(null));
                methodModel.setKind("method");

                method.getParameters().forEach(parameter ->
                        methodModel.getParameters().add(
                                createParameterModel(parameter)
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

                extractCallableBody(method, methodModel, sourceMethod, filePath, ownerQualifiedName, typeModel.getFields(), projectModel);

                typeModel.getMethods().add(methodModel);
            }
        }
    }

    private void extractCallableBody(
            Node callable,
            MethodModel methodModel,
            String qualifiedSignature,
            String filePath,
            String ownerQualifiedName,
            List<FieldModel> ownerFields,
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

            localVariable.setResolvedRawType(extractRawType(localVariable.getResolvedType()));
            localVariable.setResolvedTypeArguments(extractTypeArguments(localVariable.getResolvedType()));

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

        addObjectCreationRelationships(callable, qualifiedSignature, filePath, projectModel);
        addThrowRelationships(callable, qualifiedSignature, filePath, projectModel);
        addDataAccessRelationships(callable, methodModel, qualifiedSignature, ownerQualifiedName, ownerFields, filePath, projectModel);
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

        if (statement.isContinueStmt()) {
            BodyStatementModel model = createBaseStatement(statement, "continue");
            model.setValue("continue");
            return model;
        }

        if (statement.isBreakStmt()) {
            BodyStatementModel model = createBaseStatement(statement, "break");
            model.setValue("break");
            return model;
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


    private ParameterModel createParameterModel(Parameter parameter) {
        String resolvedType = typeResolver.resolveType(parameter.getType());
        ParameterModel model = new ParameterModel(
                parameter.getNameAsString(),
                parameter.getTypeAsString(),
                resolvedType
        );

        model.setResolvedRawType(extractRawType(resolvedType));
        model.setResolvedTypeArguments(extractTypeArguments(resolvedType));
        model.setLineStart(parameter.getBegin().map(p -> p.line).orElse(null));
        model.setLineEnd(parameter.getEnd().map(p -> p.line).orElse(null));
        parameter.getAnnotations().forEach(annotation -> model.getAnnotations().add(annotation.toString()));
        return model;
    }

    private void addObjectCreationRelationships(Node callable, String qualifiedSignature, String filePath, ProjectModel projectModel) {
        callable.findAll(ObjectCreationExpr.class).forEach(creation -> {
            String target = resolveExpressionType(creation);
            if (target == null || target.isBlank()) {
                target = normalizeJavaTypeName(creation.getType().asString());
            }
            addRelationshipIfAbsent(projectModel, "creates", qualifiedSignature, target, filePath, creation.getBegin().map(p -> p.line).orElse(null));
        });
    }

    private void addThrowRelationships(Node callable, String qualifiedSignature, String filePath, ProjectModel projectModel) {
        callable.findAll(ThrowStmt.class).forEach(throwStmt -> {
            String target = null;
            Optional<ObjectCreationExpr> creation = throwStmt.getExpression().findFirst(ObjectCreationExpr.class);
            if (creation.isPresent()) {
                target = resolveExpressionType(creation.get());
                if (target == null || target.isBlank()) {
                    target = normalizeJavaTypeName(creation.get().getType().asString());
                }
            } else {
                target = throwStmt.getExpression().toString();
            }
            addRelationshipIfAbsent(projectModel, "throws", qualifiedSignature, target, filePath, throwStmt.getBegin().map(p -> p.line).orElse(null));
        });
    }

    private void addDataAccessRelationships(
            Node callable,
            MethodModel methodModel,
            String qualifiedSignature,
            String ownerQualifiedName,
            List<FieldModel> ownerFields,
            String filePath,
            ProjectModel projectModel
    ) {
        Set<String> fieldNames = new LinkedHashSet<>();
        ownerFields.forEach(field -> fieldNames.add(field.getName()));

        Set<String> localNames = new LinkedHashSet<>();
        methodModel.getLocalVariables().forEach(local -> localNames.add(local.getName()));

        Set<String> parameterNames = new LinkedHashSet<>();
        methodModel.getParameters().forEach(parameter -> parameterNames.add(parameter.getName()));

        callable.findAll(AssignExpr.class).forEach(assign -> {
            String target = resolveVariableReference(assign.getTarget().toString(), qualifiedSignature, ownerQualifiedName, fieldNames, localNames, parameterNames);
            addRelationshipIfAbsent(projectModel, "writes", qualifiedSignature, target, filePath, assign.getBegin().map(p -> p.line).orElse(null));
        });

        callable.findAll(VariableDeclarator.class).forEach(variable -> {
            if (variable.findAncestor(FieldDeclaration.class).isPresent()) {
                return;
            }
            String target = qualifiedSignature + ":local:" + variable.getNameAsString();
            addRelationshipIfAbsent(projectModel, "writes", qualifiedSignature, target, filePath, variable.getBegin().map(p -> p.line).orElse(null));
        });

        callable.findAll(UnaryExpr.class).forEach(unary -> {
            if (unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT
                    || unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT) {
                String target = resolveVariableReference(unary.getExpression().toString(), qualifiedSignature, ownerQualifiedName, fieldNames, localNames, parameterNames);
                addRelationshipIfAbsent(projectModel, "writes", qualifiedSignature, target, filePath, unary.getBegin().map(p -> p.line).orElse(null));
            }
        });

        callable.findAll(NameExpr.class).forEach(nameExpr -> {
            if (isDeclarationName(nameExpr)) {
                return;
            }
            String target = resolveVariableReference(nameExpr.getNameAsString(), qualifiedSignature, ownerQualifiedName, fieldNames, localNames, parameterNames);
            if (!target.equals(nameExpr.getNameAsString())) {
                addRelationshipIfAbsent(projectModel, "reads", qualifiedSignature, target, filePath, nameExpr.getBegin().map(p -> p.line).orElse(null));
            }
        });

        callable.findAll(FieldAccessExpr.class).forEach(fieldAccess -> {
            String target = resolveVariableReference(fieldAccess.toString(), qualifiedSignature, ownerQualifiedName, fieldNames, localNames, parameterNames);
            addRelationshipIfAbsent(projectModel, "reads", qualifiedSignature, target, filePath, fieldAccess.getBegin().map(p -> p.line).orElse(null));
        });
    }

    private boolean isDeclarationName(NameExpr nameExpr) {
        return nameExpr.findAncestor(VariableDeclarator.class)
                .map(variable -> variable.getNameAsString().equals(nameExpr.getNameAsString()))
                .orElse(false);
    }

    private String resolveVariableReference(
            String reference,
            String qualifiedSignature,
            String ownerQualifiedName,
            Set<String> fieldNames,
            Set<String> localNames,
            Set<String> parameterNames
    ) {
        if (reference == null) {
            return "";
        }

        String normalized = reference.trim();
        if (normalized.startsWith("this.")) {
            normalized = normalized.substring("this.".length());
        }

        int lastDot = normalized.lastIndexOf('.');
        String simpleName = lastDot >= 0 ? normalized.substring(lastDot + 1) : normalized;

        if (fieldNames.contains(simpleName)) {
            return ownerQualifiedName + "." + simpleName;
        }
        if (localNames.contains(simpleName)) {
            return qualifiedSignature + ":local:" + simpleName;
        }
        if (parameterNames.contains(simpleName)) {
            return qualifiedSignature + ":parameter:" + simpleName;
        }
        return reference;
    }

    private void addRelationshipIfAbsent(ProjectModel projectModel, String type, String source, String target, String sourceFile, Integer line) {
        if (target == null || target.isBlank()) {
            return;
        }
        boolean exists = projectModel.getRelationships().stream().anyMatch(relationship ->
                Objects.equals(relationship.getType(), type)
                        && Objects.equals(relationship.getSource(), source)
                        && Objects.equals(relationship.getTarget(), target)
                        && Objects.equals(relationship.getLine(), line)
        );
        if (!exists) {
            projectModel.getRelationships().add(new RelationshipModel(type, source, target, sourceFile, line));
        }
    }

    private void enrichProjectModel(ProjectModel projectModel) {
        Set<String> internalTypes = new LinkedHashSet<>();
        projectModel.getElements().forEach(element -> internalTypes.add(element.getQualifiedName()));

        Set<String> packageNames = new LinkedHashSet<>();
        projectModel.getFiles().forEach(file -> addPackageHierarchy(packageNames, file.getPackageName()));
        projectModel.getElements().forEach(element -> addPackageHierarchy(packageNames, element.getPackageName()));

        Map<String, ExternalTypeModel> externalTypes = new LinkedHashMap<>();
        for (RelationshipModel relationship : projectModel.getRelationships()) {
            if (shouldCollectExternalTypeFrom(relationship)) {
                collectExternalType(relationship.getTarget(), internalTypes, externalTypes);
            }
        }

        collectExternalTypesFromAnnotationsAndBodies(projectModel, internalTypes, externalTypes);

        externalTypes.values().forEach(externalType -> addPackageHierarchy(packageNames, externalType.getPackageName()));

        projectModel.setPackages(packageNames.stream()
                .map(this::createPackageModel)
                .toList());
        projectModel.setExternalTypes(new ArrayList<>(externalTypes.values()));
    }

    private void collectExternalType(String rawType, Set<String> internalTypes, Map<String, ExternalTypeModel> externalTypes) {
        String normalized = extractTypeOwner(rawType);
        if (normalized == null || normalized.isBlank() || isPrimitiveOrVoid(normalized) || internalTypes.contains(normalized)) {
            return;
        }

        String raw = extractRawType(normalized);
        if (raw == null || raw.isBlank() || isPrimitiveOrVoid(raw) || internalTypes.contains(raw)) {
            return;
        }

        raw = qualifyKnownExternalType(raw);

        if (!raw.contains(".")) {
            return;
        }

        if (internalTypes.contains(raw)) {
            return;
        }

        ExternalTypeModel externalType = externalTypes.computeIfAbsent(raw, typeName -> {
            int idx = typeName.lastIndexOf('.');
            String packageName = idx >= 0 ? typeName.substring(0, idx) : "";
            String simpleName = idx >= 0 ? typeName.substring(idx + 1) : typeName;
            return new ExternalTypeModel(simpleName, typeName, packageName, guessExternalKind(typeName));
        });

        // Type arguments are intentionally not stored in externalTypes because they are
        // use-site information. They remain in resolvedTypeArguments at fields,
        // parameters, return types, and local variables. We only recurse over them
        // so their base external types are registered as separate externalTypes.
        extractTypeArguments(normalized).forEach(typeArgument ->
                collectExternalType(typeArgument, internalTypes, externalTypes)
        );
    }

    private boolean shouldCollectExternalTypeFrom(RelationshipModel relationship) {
        if (relationship == null || relationship.getType() == null) {
            return false;
        }
        return Set.of("imports", "uses_type", "calls", "creates", "throws", "extends", "implements")
                .contains(relationship.getType());
    }

    private String extractTypeOwner(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.contains("(")) {
            int paren = value.indexOf('(');
            String beforeParen = value.substring(0, paren);
            int lastDot = beforeParen.lastIndexOf('.');
            if (lastDot > 0) {
                return beforeParen.substring(0, lastDot);
            }
        }
        if (value.endsWith(".<init>")) {
            return value.substring(0, value.length() - ".<init>".length());
        }
        if (value.contains(".<init>")) {
            return value.substring(0, value.indexOf(".<init>"));
        }
        return value;
    }

    private void addPackageHierarchy(Set<String> packageNames, String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return;
        }
        String[] parts = packageName.split("\\.");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!current.isEmpty()) {
                current.append('.');
            }
            current.append(part);
            packageNames.add(current.toString());
        }
    }

    private PackageModel createPackageModel(String qualifiedName) {
        int idx = qualifiedName.lastIndexOf('.');
        String name = idx >= 0 ? qualifiedName.substring(idx + 1) : qualifiedName;
        String parent = idx >= 0 ? qualifiedName.substring(0, idx) : null;
        return new PackageModel(name, qualifiedName, parent);
    }


    private void collectExternalTypesFromAnnotationsAndBodies(
            ProjectModel projectModel,
            Set<String> internalTypes,
            Map<String, ExternalTypeModel> externalTypes
    ) {
        Set<String> internalSimpleNames = new LinkedHashSet<>();
        projectModel.getElements().forEach(element -> internalSimpleNames.add(element.getName()));

        for (TypeModel element : projectModel.getElements()) {
            collectAnnotationExternalTypes(element.getAnnotations(), internalTypes, internalSimpleNames, externalTypes);

            element.getFields().forEach(field ->
                    collectAnnotationExternalTypes(field.getAnnotations(), internalTypes, internalSimpleNames, externalTypes)
            );

            for (MethodModel method : element.getMethods()) {
                collectAnnotationExternalTypes(method.getAnnotations(), internalTypes, internalSimpleNames, externalTypes);
                method.getParameters().forEach(parameter ->
                        collectAnnotationExternalTypes(parameter.getAnnotations(), internalTypes, internalSimpleNames, externalTypes)
                );
                collectExternalTypesFromStatements(method.getBody(), internalTypes, externalTypes);
            }
        }
    }

    private void collectAnnotationExternalTypes(
            List<String> annotations,
            Set<String> internalTypes,
            Set<String> internalSimpleNames,
            Map<String, ExternalTypeModel> externalTypes
    ) {
        if (annotations == null) {
            return;
        }

        for (String annotation : annotations) {
            String annotationName = extractAnnotationName(annotation);
            if (annotationName == null || annotationName.isBlank()) {
                continue;
            }
            String rawAnnotation = annotationName.contains(".")
                    ? annotationName
                    : qualifyKnownExternalType(annotationName);

            if (rawAnnotation == null || rawAnnotation.isBlank()) {
                continue;
            }
            if (!rawAnnotation.contains(".") && internalSimpleNames.contains(rawAnnotation)) {
                continue;
            }
            if (internalTypes.contains(rawAnnotation)) {
                continue;
            }
            collectExternalType(rawAnnotation, internalTypes, externalTypes);
        }
    }

    private String extractAnnotationName(String annotation) {
        if (annotation == null) {
            return null;
        }
        String value = annotation.trim();
        if (value.startsWith("@")) {
            value = value.substring(1);
        }
        int paren = value.indexOf('(');
        if (paren >= 0) {
            value = value.substring(0, paren);
        }
        int space = value.indexOf(' ');
        if (space >= 0) {
            value = value.substring(0, space);
        }
        return value.trim();
    }

    private void collectExternalTypesFromStatements(
            List<BodyStatementModel> statements,
            Set<String> internalTypes,
            Map<String, ExternalTypeModel> externalTypes
    ) {
        if (statements == null) {
            return;
        }

        for (BodyStatementModel statement : statements) {
            collectExternalType(statement.getExceptionType(), internalTypes, externalTypes);
            collectExternalType(statement.getClassName(), internalTypes, externalTypes);
            collectExternalTypesFromStatements(statement.getBody(), internalTypes, externalTypes);
            collectExternalTypesFromStatements(statement.getElseBody(), internalTypes, externalTypes);
            collectExternalTypesFromStatements(statement.getCatchClauses(), internalTypes, externalTypes);
            collectExternalTypesFromStatements(statement.getFinallyBody(), internalTypes, externalTypes);
        }
    }

    private String qualifyKnownExternalType(String rawType) {
        if (rawType == null) {
            return null;
        }
        String value = rawType.trim();
        if (value.contains(".")) {
            return value;
        }
        return switch (value) {
            case "Deprecated" -> "java.lang.Deprecated";
            case "RuntimeException" -> "java.lang.RuntimeException";
            case "IllegalArgumentException" -> "java.lang.IllegalArgumentException";
            case "IllegalStateException" -> "java.lang.IllegalStateException";
            case "String" -> "java.lang.String";
            case "Long" -> "java.lang.Long";
            case "StringBuilder" -> "java.lang.StringBuilder";
            case "Retention" -> "java.lang.annotation.Retention";
            case "RetentionPolicy" -> "java.lang.annotation.RetentionPolicy";
            default -> value;
        };
    }

    private String guessExternalKind(String qualifiedName) {
        if (qualifiedName == null) {
            return "class";
        }
        if (qualifiedName.equals("java.lang.annotation.RetentionPolicy")) {
            return "enum";
        }
        if (qualifiedName.equals("java.lang.Deprecated")) {
            return "annotation";
        }
        if (qualifiedName.equals("java.util.List")
                || qualifiedName.equals("java.util.Map")
                || qualifiedName.equals("java.util.Collection")
                || qualifiedName.equals("java.lang.CharSequence")
                || qualifiedName.equals("java.lang.Comparable")
                || qualifiedName.equals("java.io.Serializable")
                || qualifiedName.startsWith("java.lang.annotation.")) {
            return "interface";
        }
        return "class";
    }

    private boolean isPrimitiveOrVoid(String typeName) {
        return Set.of("void", "boolean", "byte", "short", "int", "long", "float", "double", "char").contains(typeName);
    }

    private String extractRawType(String typeName) {
        if (typeName == null) {
            return null;
        }
        String value = typeName.trim();
        int genericStart = value.indexOf('<');
        if (genericStart >= 0) {
            return value.substring(0, genericStart).trim();
        }
        return value;
    }

    private List<String> extractTypeArguments(String typeName) {
        if (typeName == null) {
            return new ArrayList<>();
        }
        int start = typeName.indexOf('<');
        int end = typeName.lastIndexOf('>');
        if (start < 0 || end < start) {
            return new ArrayList<>();
        }
        String arguments = typeName.substring(start + 1, end);
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);
            if (c == '<') {
                depth++;
                current.append(c);
            } else if (c == '>') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.toString().trim().isBlank()) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private String buildSignature(String methodName, List<ParameterModel> parameters) {
        String parameterTypes = parameters.stream()
                .map(ParameterModel::getType)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        return methodName + "(" + parameterTypes + ")";
    }
}
