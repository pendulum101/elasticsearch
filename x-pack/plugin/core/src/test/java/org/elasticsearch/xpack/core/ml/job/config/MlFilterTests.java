/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.job.config;

import com.carrotsearch.randomizedtesting.generators.CodepointSetGenerator;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.common.io.stream.Writeable.Reader;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

public class MlFilterTests extends AbstractSerializingTestCase<MlFilter> {

    public static MlFilter createTestFilter() {
        return new MlFilterTests().createTestInstance();
    }

    @Override
    protected MlFilter createTestInstance() {
        return createRandom();
    }

    public static MlFilter createRandom() {
        return createRandom(randomValidFilterId());
    }

    public static String randomValidFilterId() {
        CodepointSetGenerator generator =  new CodepointSetGenerator("abcdefghijklmnopqrstuvwxyz".toCharArray());
        return generator.ofCodePointsLength(random(), 10, 10);
    }

    public static MlFilter createRandom(String filterId) {
        String description = null;
        if (randomBoolean()) {
            description = randomAlphaOfLength(20);
        }

        int size = randomInt(10);
        TreeSet<String> items = new TreeSet<>();
        for (int i = 0; i < size; i++) {
            items.add(randomAlphaOfLengthBetween(1, 20));
        }
        return MlFilter.builder(filterId).setDescription(description).setItems(items).build();
    }

    @Override
    protected Reader<MlFilter> instanceReader() {
        return MlFilter::new;
    }

    @Override
    protected MlFilter doParseInstance(XContentParser parser) {
        return MlFilter.STRICT_PARSER.apply(parser, null).build();
    }

    public void testNullId() {
        Exception ex = expectThrows(IllegalArgumentException.class, () -> MlFilter.builder(null).build());
        assertEquals("[filter_id] must not be null.", ex.getMessage());
    }

    public void testNullItems() {
        NullPointerException ex = expectThrows(NullPointerException.class,
                () -> MlFilter.builder(randomValidFilterId()).setItems((SortedSet<String>) null).build());
        assertEquals(MlFilter.ITEMS.getPreferredName() + " must not be null", ex.getMessage());
    }

    public void testDocumentId() {
        assertThat(MlFilter.documentId("foo"), equalTo("filter_foo"));
    }

    public void testStrictParser() throws IOException {
        String json = "{\"filter_id\":\"filter_1\", \"items\": [], \"foo\":\"bar\"}";
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, json)) {
            IllegalArgumentException e = expectThrows(IllegalArgumentException.class,
                    () -> MlFilter.STRICT_PARSER.apply(parser, null));

            assertThat(e.getMessage(), containsString("unknown field [foo]"));
        }
    }

    public void testLenientParser() throws IOException {
        String json = "{\"filter_id\":\"filter_1\", \"items\": [], \"foo\":\"bar\"}";
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, json)) {
            MlFilter.LENIENT_PARSER.apply(parser, null);
        }
    }

    public void testInvalidId() {
        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> MlFilter.builder("Invalid id").build());
        assertThat(e.getMessage(), startsWith("Invalid filter_id; 'Invalid id' can contain lowercase"));
    }

    public void testItemsAreSorted() {
        MlFilter filter = MlFilter.builder("foo").setItems("c", "b", "a").build();
        assertThat(filter.getItems(), contains("a", "b", "c"));
    }

    public void testGetItemsReturnsUnmodifiable() {
        MlFilter filter = MlFilter.builder("foo").setItems("c", "b", "a").build();
        expectThrows(UnsupportedOperationException.class, () -> filter.getItems().add("x"));
    }
}
