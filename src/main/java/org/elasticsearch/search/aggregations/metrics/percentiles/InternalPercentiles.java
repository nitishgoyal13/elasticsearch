/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.search.aggregations.metrics.percentiles;

import com.google.common.collect.UnmodifiableIterator;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.xcontent.XContentObject;
import org.elasticsearch.search.aggregations.AggregationStreams;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.metrics.percentiles.tdigest.TDigestState;

import java.io.IOException;
import java.util.Iterator;

/**
*
*/
public class InternalPercentiles extends AbstractInternalPercentiles implements Percentiles {

    // added name for es 5.0
    public final static Type TYPE = new Type("tdigest_percentiles", "percentiles");

    public final static AggregationStreams.Stream STREAM = new AggregationStreams.Stream() {
        @Override
        public InternalPercentiles readResult(StreamInput in) throws IOException {
            InternalPercentiles result = new InternalPercentiles();
            result.readFrom(in);
            return result;
        }

        @Override
        public InternalAggregation readResult(XContentObject in) throws IOException {
            InternalPercentiles result = new InternalPercentiles();
            result.readFrom(in);
            return result;
        }
    };

    public static void registerStreams() {
        AggregationStreams.registerStream(STREAM, TYPE.stream());
        AggregationStreams.registerStream(STREAM, new BytesArray(TYPE.name()));
    }

    InternalPercentiles() {} // for serialization

    public InternalPercentiles(String name, double[] percents, TDigestState state, boolean keyed) {
        super(name, percents, state, keyed);
    }

    @Override
    public Iterator<Percentile> iterator() {
        if (values != null) {
            return new PercentileIterator(values.entrySet().iterator());
        }
        return new Iter(keys, state);
    }

    @Override
    public double percentile(double percent) {
        if (values != null) {
            return values.get(String.valueOf(percent));
        }
        return state.quantile(percent / 100);
    }

    @Override
    public double value(double key) {
        return percentile(key);
    }

    protected AbstractInternalPercentiles createReduced(String name, double[] keys, TDigestState merged, boolean keyed) {
        return new InternalPercentiles(name, keys, merged, keyed);
    }

    @Override
    public Type type() {
        return TYPE;
    }

    public static class Iter extends UnmodifiableIterator<Percentile> {

        private final double[] percents;
        private final TDigestState state;
        private int i;

        public Iter(double[] percents, TDigestState state) {
            this.percents = percents;
            this.state = state;
            i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < percents.length;
        }

        @Override
        public Percentile next() {
            final Percentile next = new InternalPercentile(percents[i], state.quantile(percents[i] / 100));
            ++i;
            return next;
        }
    }

}
