/**
 * Copyright 2011 Asakusa Framework Team.
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
package ${package}.batch;

import com.asakusafw.testdriver.BatchTestDriver;

import org.junit.Test;

/**
 * サンプル：バッチのテストクラス
 */
public class ExBatchTest {
    
    /**
     * サンプル：バッチの実行
     * @throws Throwable テストに失敗した場合
     */        
    @Test
    public void testExample() throws Throwable {
        
        BatchTestDriver driver = new BatchTestDriver();
        driver.runTest(ExBatch.class);
        
    }
}