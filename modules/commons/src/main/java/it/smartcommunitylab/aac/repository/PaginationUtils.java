/**
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.repository;

import java.util.function.BiFunction;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    public static class PageOffset implements Pageable {

        long offset;
        int size;
        Sort sort;

        public PageOffset(long offset, int size) {
            this.offset = offset;
            this.size = size;
            this.sort = null;
        }

        public PageOffset(long offset, int size, Sort sort) {
            this.offset = offset;
            this.size = size;
            this.sort = sort;
        }

        @Override
        public int getPageNumber() {
            return 0;
        }

        @Override
        public int getPageSize() {
            return size;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            return new PageOffset(offset + size, size, sort);
        }

        @Override
        public Pageable previousOrFirst() {
            return this;
        }

        @Override
        public Pageable first() {
            return this;
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return this;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }
    }
}
