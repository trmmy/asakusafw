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
package com.asakusafw.compiler.operator.processor;

import static org.junit.Assert.*;

import org.junit.Test;

import com.asakusafw.compiler.operator.OperatorCompilerTestRoot;
import com.asakusafw.compiler.operator.model.MockHoge;
import com.asakusafw.compiler.operator.model.MockSummarized;
import com.asakusafw.utils.graph.Graph;
import com.asakusafw.vocabulary.flow.testing.MockIn;
import com.asakusafw.vocabulary.flow.testing.MockOut;

/**
 * Test for {@link SummarizeOperatorProcessor}.
 */
public class SummarizeOperatorProcessorTest extends OperatorCompilerTestRoot {

    /**
     * 単純な例。
     */
    @Test
    public void simple() {
        add("com.example.Simple");
        ClassLoader loader = start(new SummarizeOperatorProcessor());
        Object factory = create(loader, "com.example.SimpleFactory");

        MockIn<MockHoge> in = MockIn.of(MockHoge.class, "in");
        MockOut<MockSummarized> out = MockOut.of(MockSummarized.class, "out");
        Object summarize = invoke(factory, "example", in);
        out.add(output(MockSummarized.class, summarize, "out"));

        Graph<String> graph = toGraph(in);
        assertThat(graph.getConnected("in"), isJust("Simple.example"));
        assertThat(graph.getConnected("Simple.example"), isJust("out"));
    }

    /**
     * 具象メソッド。
     */
    @Test
    public void NotAbstract() {
        add("com.example.NotAbstract");
        error(new SummarizeOperatorProcessor());
    }

    /**
     * モデルでない。
     */
    @Test
    public void NotModel() {
        add("com.example.NotModel");
        error(new SummarizeOperatorProcessor());
    }

    /**
     * 集計モデルでない。
     */
    @Test
    public void NotSummarized() {
        add("com.example.NotSummarized");
        error(new SummarizeOperatorProcessor());
    }

    /**
     * パラメーター化。
     */
    @Test
    public void Parameterized() {
        add("com.example.Parameterized");
        error(new SummarizeOperatorProcessor());
    }

    /**
     * Generic method.
     */
    @Test
    public void Generic() {
        add("com.example.Generic");
        error(new SummarizeOperatorProcessor());
    }
}
