/*
 * Copyright 2015 Calrissian.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.calrissian.flowmix.api.aggregator;

import org.junit.Test;
import static org.junit.Assert.*;

public class CountAggregatorTest {

    @Test
    public void test() {
        System.out.println("CountAggregatorTest");
        CountAggregator instance = new CountAggregator();
        instance.add((long) 33);
        instance.add((long) 33);
        instance.add((long) 33);
        instance.add((long) 33);
        instance.evict((long) 1);
        Long result = instance.aggregateResult();
        Long expectedResult = (long) 3;
        assertEquals(expectedResult, result);
    }

    @Test
    public void testNoItems() {
        System.out.println("CountAggregatorTest - No Items");
        CountAggregator instance = new CountAggregator();
        instance.add((long) 33);
        instance.add((long) 33);
        instance.add((long) 33);
        instance.add((long) 33);
        instance.evict((long) 33);
        instance.evict((long) 33);
        instance.evict((long) 33);
        instance.evict((long) 33);
        Long result = instance.aggregateResult();
        Long expectedResult = (long) 0;
        assertEquals(expectedResult, result);
    }

}
