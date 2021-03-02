/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.idp.tests.aforeport;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class AfoJavaTestParser implements ITestParser {

    private final Map<String, List<Testcase>> parsedTestcasesPerAfo = new HashMap<>();

    @Override
    public void resetMap() {
        parsedTestcasesPerAfo.clear();
    }

    public void parseDirectory(final File rootDir) {
        if (rootDir == null) {
            log.warn("Invalid test source NULL root dir");
        } else {
            final File[] files = rootDir.listFiles();
            if (files == null) {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Invalid test source root dir %s", rootDir.getAbsolutePath()));
                }
            } else {
                Arrays.asList(files).forEach(f -> {
                    if (f.isDirectory()) {
                        parseDirectory(f);
                    } else if (f.getName().endsWith(".java")) {
                        inspectFile(f);
                    }
                });
            }
        }
    }

    private void inspectFile(final File f) {
        try (final FileInputStream in = new FileInputStream(f)) {
            final CompilationUnit cu = StaticJavaParser.parse(in);
            new MethodVisitor(this).visit(cu, null);

        } catch (final IOException ioex) {
            throw new AfoReporterException("Unable to parse " + f.getAbsolutePath(), ioex);
        }
    }

    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes, checking for Afo and Test annotations.
     */
    private static class MethodVisitor extends VoidVisitorAdapter<Object> {

        private final AfoJavaTestParser parser;

        MethodVisitor(final AfoJavaTestParser parser) {
            this.parser = parser;
        }

        private static String getFullyQualifiedName(final ClassOrInterfaceDeclaration testClass) {
            return testClass.getParentNode()
                .flatMap(MethodVisitor::getClass)
                .map(parentClass -> getFullyQualifiedName(parentClass) + "." + testClass.getNameAsString())

                .orElseGet(() -> getCompilationUnit(testClass)
                    .flatMap(CompilationUnit::getPackageDeclaration)
                    .map(PackageDeclaration::getNameAsString)
                    .map(packageName -> packageName + "." + testClass.getNameAsString())

                    .orElse(testClass.getNameAsString()));
        }

        private static Optional<ClassOrInterfaceDeclaration> getClass(final Node method) {
            Node clazz = method;
            while (!(clazz instanceof ClassOrInterfaceDeclaration)) {
                final Optional<Node> cpn = clazz.getParentNode();
                if (cpn.isPresent()) {
                    clazz = cpn.get();
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of((ClassOrInterfaceDeclaration) clazz);
        }

        private static Optional<CompilationUnit> getCompilationUnit(final Node clazz) {
            Node pkgs = clazz;
            while (!(pkgs instanceof CompilationUnit)) {
                final Optional<Node> pn = pkgs.getParentNode();
                if (pn.isPresent()) {
                    pkgs = pn.get();
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of((CompilationUnit) pkgs);
        }

        @Override
        public void visit(final MethodDeclaration n, final Object args) {
            visitMethodAndAddAfoToTestcaseListIfPresent(n);
        }

        private void visitMethodAndAddAfoToTestcaseListIfPresent(final MethodDeclaration n) {
            final String methodname = n.getNameAsString();
            n.getAnnotations().stream()
                .filter(afo -> "Afo".equals(afo.getNameAsString()))
                .forEach(afo -> addTestCaseToAfo(n, methodname, afo));
        }

        private void addTestCaseToAfo(final MethodDeclaration n, final String methodname, final AnnotationExpr afo) {
            final String clazzname = getFullyQualifiedName((ClassOrInterfaceDeclaration) n.getParentNode().orElseThrow(
                () -> new AfoReporterException((
                    String.format("Internal Error. Test Method has no parent node. Method name is %s!", methodname)))));
            if (afo instanceof SingleMemberAnnotationExpr) {
                final String id = ((SingleMemberAnnotationExpr) afo).getMemberValue().asStringLiteralExpr().asString();
                parser.parsedTestcasesPerAfo.computeIfAbsent(id, k -> new ArrayList<>());
                final Testcase tc = new Testcase();
                tc.setClazz(clazzname);
                tc.setMethod(methodname);
                parser.parsedTestcasesPerAfo.get(id).add(tc);
            } else {
                throw new AfoReporterException(
                    "Unsupported Afo Annotation detected in " + clazzname + ":" + methodname + "!");
            }
        }
    }
}