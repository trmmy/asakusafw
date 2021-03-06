/**
 * Copyright 2011-2012 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.compiler.flow.stage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.asakusafw.compiler.common.Naming;
import com.asakusafw.compiler.common.Precondition;
import com.asakusafw.compiler.flow.DataClass;
import com.asakusafw.compiler.flow.FlowCompilingEnvironment;
import com.asakusafw.compiler.flow.stage.ShuffleModel.Segment;
import com.asakusafw.runtime.flow.SegmentedWritable;
import com.asakusafw.utils.collections.Lists;
import com.asakusafw.utils.java.model.syntax.Comment;
import com.asakusafw.utils.java.model.syntax.CompilationUnit;
import com.asakusafw.utils.java.model.syntax.Expression;
import com.asakusafw.utils.java.model.syntax.FieldDeclaration;
import com.asakusafw.utils.java.model.syntax.FormalParameterDeclaration;
import com.asakusafw.utils.java.model.syntax.InfixOperator;
import com.asakusafw.utils.java.model.syntax.Javadoc;
import com.asakusafw.utils.java.model.syntax.MethodDeclaration;
import com.asakusafw.utils.java.model.syntax.ModelFactory;
import com.asakusafw.utils.java.model.syntax.Name;
import com.asakusafw.utils.java.model.syntax.SimpleName;
import com.asakusafw.utils.java.model.syntax.Statement;
import com.asakusafw.utils.java.model.syntax.Type;
import com.asakusafw.utils.java.model.syntax.TypeBodyDeclaration;
import com.asakusafw.utils.java.model.syntax.TypeDeclaration;
import com.asakusafw.utils.java.model.syntax.TypeParameterDeclaration;
import com.asakusafw.utils.java.model.util.AttributeBuilder;
import com.asakusafw.utils.java.model.util.ExpressionBuilder;
import com.asakusafw.utils.java.model.util.ImportBuilder;
import com.asakusafw.utils.java.model.util.JavadocBuilder;
import com.asakusafw.utils.java.model.util.Models;
import com.asakusafw.utils.java.model.util.TypeBuilder;

/**
 * シャッフルフェーズで利用する値を生成する。
 */
public class ShuffleValueEmitter {

    static final Logger LOG = LoggerFactory.getLogger(ShuffleValueEmitter.class);

    private FlowCompilingEnvironment environment;

    /**
     * インスタンスを生成する。
     * @param environment 環境オブジェクト
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public ShuffleValueEmitter(FlowCompilingEnvironment environment) {
        Precondition.checkMustNotBeNull(environment, "environment"); //$NON-NLS-1$
        this.environment = environment;
    }

    /**
     * 指定のモデルに対する値を表すクラスを生成し、生成したクラスの完全限定名を返す。
     * @param model 対象のモデル
     * @return 生成したクラスの完全限定名
     * @throws IOException クラスの生成に失敗した場合
     * @throws IllegalArgumentException 引数に{@code null}が指定された場合
     */
    public Name emit(ShuffleModel model) throws IOException {
        Precondition.checkMustNotBeNull(model, "model"); //$NON-NLS-1$
        LOG.debug("{}に対するシャッフル値を生成します", model.getStageBlock());
        Engine engine = new Engine(environment, model);
        CompilationUnit source = engine.generate();
        environment.emit(source);
        Name packageName = source.getPackageDeclaration().getName();
        SimpleName simpleName = source.getTypeDeclarations().get(0).getName();
        Name name = environment.getModelFactory().newQualifiedName(packageName, simpleName);
        LOG.debug("{}のシャッフル値には{}が利用されます",
                model.getStageBlock(),
                name);
        return name;
    }

    private static class Engine {

        private static final String SEGMENT_ID_FIELD_NAME = "segmentId";

        private ShuffleModel model;

        private ModelFactory factory;

        private ImportBuilder importer;

        public Engine(FlowCompilingEnvironment environment, ShuffleModel model) {
            assert environment != null;
            assert model != null;
            this.model = model;
            this.factory = environment.getModelFactory();
            Name packageName = environment.getStagePackageName(model.getStageBlock().getStageNumber());
            this.importer = new ImportBuilder(
                    factory,
                    factory.newPackageDeclaration(packageName),
                    ImportBuilder.Strategy.TOP_LEVEL);
        }

        public CompilationUnit generate() {
            TypeDeclaration type = createType();
            return factory.newCompilationUnit(
                    importer.getPackageDeclaration(),
                    importer.toImportDeclarations(),
                    Collections.singletonList(type),
                    Collections.<Comment>emptyList());
        }

        private TypeDeclaration createType() {
            SimpleName name = factory.newSimpleName(Naming.getShuffleValueClass());
            importer.resolvePackageMember(name);
            List<TypeBodyDeclaration> members = Lists.create();
            members.addAll(createSegmentDistinction());
            members.addAll(createProperties());
            members.addAll(createAccessors());
            members.addAll(createWritables());
            return factory.newClassDeclaration(
                    createJavadoc(),
                    new AttributeBuilder(factory)
                        .annotation(t(SuppressWarnings.class), v("deprecation"))
                        .Public()
                        .Final()
                        .toAttributes(),
                    name,
                    Collections.<TypeParameterDeclaration>emptyList(),
                    null,
                    Collections.singletonList(t(SegmentedWritable.class)),
                    members);
        }

        private List<TypeBodyDeclaration> createSegmentDistinction() {
            List<TypeBodyDeclaration> results = Lists.create();
            results.add(createSegmentIdField());
            results.add(createSegmentIdGetter());
            return results;
        }

        private FieldDeclaration createSegmentIdField() {
            return factory.newFieldDeclaration(
                    new JavadocBuilder(factory)
                        .text("セグメント番号。")
                        .toJavadoc(),
                    new AttributeBuilder(factory)
                        .Public()
                        .toAttributes(),
                    t(int.class),
                    factory.newSimpleName(SEGMENT_ID_FIELD_NAME),
                    v(-1));
        }

        private TypeBodyDeclaration createSegmentIdGetter() {
            Statement body = new ExpressionBuilder(factory, factory.newThis())
                .field(SEGMENT_ID_FIELD_NAME)
                .toReturnStatement();
            return factory.newMethodDeclaration(
                    null,
                    new AttributeBuilder(factory)
                        .annotation(t(Override.class))
                        .Public()
                        .toAttributes(),
                    t(int.class),
                    factory.newSimpleName(SegmentedWritable.ID_GETTER),
                    Collections.<FormalParameterDeclaration>emptyList(),
                    Collections.singletonList(body));
        }

        private List<FieldDeclaration> createProperties() {
            List<FieldDeclaration> results = Lists.create();
            for (Segment segment : model.getSegments()) {
                results.add(createProperty(segment));
            }
            return results;
        }

        private String createPropertyName(Segment segment) {
            return String.format("%s%04d", "port", segment.getPortId());
        }

        private FieldDeclaration createProperty(Segment segment) {
            assert segment != null;
            String name = createPropertyName(segment);
            DataClass target = segment.getTarget();
            return factory.newFieldDeclaration(
                    new JavadocBuilder(factory)
                        .text("{0}#{1}が利用するモデル ({2})。",
                                segment.getPort().getOwner().getDescription().getName(),
                                segment.getPort().getDescription().getName(),
                                segment.getPortId())
                        .toJavadoc(),
                    new AttributeBuilder(factory)
                        .Public()
                        .toAttributes(),
                    t(target.getType()),
                    factory.newSimpleName(name),
                    target.createNewInstance(t(target.getType())));
        }

        private List<MethodDeclaration> createAccessors() {
            List<MethodDeclaration> results = Lists.create();
            for (Segment segment : model.getSegments()) {
                results.add(createGetter(segment));
                results.add(createSetter(segment));
            }
            return results;
        }

        private MethodDeclaration createGetter(Segment segment) {
            assert segment != null;
            String methodName = Naming.getShuffleValueGetter(segment.getPortId());

            List<Statement> statements = Lists.create();
            statements.add(factory.newIfStatement(
                    new ExpressionBuilder(factory, factory.newThis())
                        .field(SEGMENT_ID_FIELD_NAME)
                        .apply(InfixOperator.NOT_EQUALS, v(segment.getPortId()))
                        .toExpression(),
                    new TypeBuilder(factory, t(AssertionError.class))
                        .newObject()
                        .toThrowStatement(),
                    null));

            statements.add(new ExpressionBuilder(factory, factory.newThis())
                .field(createPropertyName(segment))
                .toReturnStatement());

            return factory.newMethodDeclaration(
                    new JavadocBuilder(factory)
                        .text("{0}#{1}のモデルオブジェクトを返す。",
                                segment.getPort().getOwner().getDescription().getName(),
                                segment.getPort().getDescription().getName())
                        .toJavadoc(),
                    new AttributeBuilder(factory)
                        .Public()
                        .toAttributes(),
                    t(segment.getTarget().getType()),
                    factory.newSimpleName(methodName),
                    Collections.<FormalParameterDeclaration>emptyList(),
                    statements);
        }

        private MethodDeclaration createSetter(Segment segment) {
            assert segment != null;
            String methodName = Naming.getShuffleValueSetter(segment.getPortId());
            DataClass type = segment.getTarget();

            SimpleName argument = factory.newSimpleName("model");

            List<Statement> statements = Lists.create();
            statements.add(new ExpressionBuilder(factory, factory.newThis())
                .field(factory.newSimpleName(SEGMENT_ID_FIELD_NAME))
                .assignFrom(v(segment.getPortId()))
                .toStatement());
            statements.add(type.assign(
                    new ExpressionBuilder(factory, factory.newThis())
                        .field(createPropertyName(segment))
                        .toExpression(),
                    argument));

            return factory.newMethodDeclaration(
                    new JavadocBuilder(factory)
                        .text("{0}#{1}で使用するモデルオブジェクトを設定する。",
                                segment.getPort().getOwner().getDescription().getName(),
                                segment.getPort().getDescription().getName())
                        .param(argument)
                            .text("設定するモデルオブジェクト")
                        .toJavadoc(),
                    new AttributeBuilder(factory)
                        .Public()
                        .toAttributes(),
                    t(void.class),
                    factory.newSimpleName(methodName),
                    Collections.singletonList(factory.newFormalParameterDeclaration(
                            t(type.getType()),
                            argument)),
                    statements);
        }

        private List<MethodDeclaration> createWritables() {
            return Arrays.asList(
                    createWriteMethod(),
                    createReadFieldsMethod());
        }

        private MethodDeclaration createWriteMethod() {
            SimpleName out = factory.newSimpleName("out");

            Expression segmentId = new ExpressionBuilder(factory, factory.newThis())
                .field(SEGMENT_ID_FIELD_NAME)
                .toExpression();

            List<Statement> cases = Lists.create();
            for (Segment segment : model.getSegments()) {
                cases.add(factory.newSwitchCaseLabel(v(segment.getPortId())));
                cases.add(new ExpressionBuilder(factory, out)
                    .method("writeInt", v(segment.getPortId()))
                    .toStatement());
                String fieldName = createPropertyName(segment);
                cases.add(segment.getTarget().createWriter(
                        new ExpressionBuilder(factory, factory.newThis())
                            .field(fieldName)
                            .toExpression(),
                        out));
                cases.add(factory.newBreakStatement());
            }
            cases.add(factory.newSwitchDefaultLabel());
            cases.add(new TypeBuilder(factory, t(AssertionError.class))
                .newObject(segmentId)
                .toThrowStatement());

            List<Statement> statements = Lists.create();
            statements.add(factory.newSwitchStatement(segmentId, cases));

            return factory.newMethodDeclaration(
                    null,
                    new AttributeBuilder(factory)
                        .annotation(t(Override.class))
                        .Public()
                        .toAttributes(),
                    Collections.<TypeParameterDeclaration>emptyList(),
                    t(void.class),
                    factory.newSimpleName("write"),
                    Collections.singletonList(factory.newFormalParameterDeclaration(
                            t(DataOutput.class),
                            out)),
                    0,
                    Collections.singletonList(t(IOException.class)),
                    factory.newBlock(statements));
        }

        private MethodDeclaration createReadFieldsMethod() {
            SimpleName in = factory.newSimpleName("in");

            Expression segmentId = new ExpressionBuilder(factory, factory.newThis())
                .field(SEGMENT_ID_FIELD_NAME)
                .toExpression();

            List<Statement> statements = Lists.create();
            statements.add(new ExpressionBuilder(factory, segmentId)
                .assignFrom(new ExpressionBuilder(factory, in)
                    .method("readInt")
                    .toExpression())
                .toStatement());

            List<Statement> cases = Lists.create();
            for (Segment segment : model.getSegments()) {
                cases.add(factory.newSwitchCaseLabel(v(segment.getPortId())));
                String fieldName = createPropertyName(segment);
                cases.add(segment.getTarget().createReader(
                        new ExpressionBuilder(factory, factory.newThis())
                            .field(fieldName)
                            .toExpression(),
                        in));
                cases.add(factory.newBreakStatement());
            }
            cases.add(factory.newSwitchDefaultLabel());
            cases.add(new TypeBuilder(factory, t(AssertionError.class))
                .newObject(segmentId)
                .toThrowStatement());

            statements.add(factory.newSwitchStatement(segmentId, cases));

            return factory.newMethodDeclaration(
                    null,
                    new AttributeBuilder(factory)
                        .annotation(t(Override.class))
                        .Public()
                        .toAttributes(),
                    Collections.<TypeParameterDeclaration>emptyList(),
                    t(void.class),
                    factory.newSimpleName("readFields"),
                    Collections.singletonList(factory.newFormalParameterDeclaration(
                            t(DataInput.class),
                            in)),
                    0,
                    Collections.singletonList(t(IOException.class)),
                    factory.newBlock(statements));
        }

        private Javadoc createJavadoc() {
            return new JavadocBuilder(factory)
                .text("ステージ#{0}シャッフルで利用する値クラス。",
                    model.getStageBlock().getStageNumber())
                .toJavadoc();
        }

        private Type t(java.lang.reflect.Type type) {
            return importer.resolve(Models.toType(factory, type));
        }

        private Expression v(Object value) {
            return Models.toLiteral(factory, value);
        }
    }
}
