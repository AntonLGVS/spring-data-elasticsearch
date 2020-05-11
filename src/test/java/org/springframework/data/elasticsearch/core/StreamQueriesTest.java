/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.core;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/**
 * @author Sascha Woo
 * @author Peter-Josef Meisch
 */
public class StreamQueriesTest {

	@Test // DATAES-764
	public void shouldCallClearScrollOnIteratorClose() {

		// given
		List<SearchHit<String>> hits = new ArrayList<>();
		hits.add(new SearchHit<String>(null, 0, null, null, "one"));

		SearchScrollHits<String> searchHits = newSearchScrollHits(hits, "1234");

		AtomicBoolean clearScrollCalled = new AtomicBoolean(false);

		// when
		SearchHitsIterator<String> iterator = StreamQueries.streamResults( //
				searchHits, //
				scrollId -> newSearchScrollHits(Collections.emptyList(), scrollId), //
				scrollIds -> clearScrollCalled.set(true));

		while (iterator.hasNext()) {
			iterator.next();
		}
		iterator.close();

		// then
		assertThat(clearScrollCalled).isTrue();

	}

	@Test // DATAES-766
	public void shouldReturnTotalHits() {

		// given
		List<SearchHit<String>> hits = new ArrayList<>();
		hits.add(new SearchHit<String>(null, 0, null, null, "one"));

		SearchScrollHits<String> searchHits = newSearchScrollHits(hits, "1234");

		// when
		SearchHitsIterator<String> iterator = StreamQueries.streamResults( //
				searchHits, //
				scrollId -> newSearchScrollHits(Collections.emptyList(), scrollId), //
				scrollId -> {});

		// then
		assertThat(iterator.getTotalHits()).isEqualTo(1);

	}

	@Test // DATAES-817
	void shouldClearAllScrollIds() {

		SearchScrollHits<String> searchHits1 = newSearchScrollHits(
				Collections.singletonList(new SearchHit<String>(null, 0, null, null, "one")), "s-1");
		SearchScrollHits<String> searchHits2 = newSearchScrollHits(
				Collections.singletonList(new SearchHit<String>(null, 0, null, null, "one")), "s-2");
		SearchScrollHits<String> searchHits3 = newSearchScrollHits(
				Collections.singletonList(new SearchHit<String>(null, 0, null, null, "one")), "s-2");
		SearchScrollHits<String> searchHits4 = newSearchScrollHits(Collections.emptyList(), "s-3");

		Iterator<SearchScrollHits<String>> searchScrollHitsIterator = Arrays.asList(searchHits1, searchHits2, searchHits3,searchHits4).iterator();

		List<String> clearedScrollIds = new ArrayList<>();
		SearchHitsIterator<String> iterator = StreamQueries.streamResults( //
				searchScrollHitsIterator.next(), //
				scrollId -> searchScrollHitsIterator.next(), //
				scrollIds -> clearedScrollIds.addAll(scrollIds));

		while (iterator.hasNext()) {
			iterator.next();
		}
		iterator.close();

		assertThat(clearedScrollIds).isEqualTo(Arrays.asList("s-1", "s-2", "s-3"));
	}

	private SearchScrollHits<String> newSearchScrollHits(List<SearchHit<String>> hits, String scrollId) {
		return new SearchHitsImpl<String>(hits.size(), TotalHitsRelation.EQUAL_TO, 0, scrollId, hits, null);
	}
}