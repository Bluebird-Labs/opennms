/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.flows.classification.internal.decision;

/**
 * Tracks threshold values while constructing a decision tree.
 * <p>
 * Used to filter candidate thresholds and classification rules.
 */
public abstract class Bound<T extends Comparable<T>> {

    /** Restricts this bound such that it includes only values that are less than the given value. */
    public abstract Bound<T> lt(T value);

    /** Restricts this bound such that it includes only the given value. */
    public abstract Bound<T> eq(T value);

    /** Restricts this bound such that it includes only values that are greater than the given value. */
    public abstract Bound<T> gt(T value);

    /** Checks if this bound includes the given value. */
    public abstract boolean includes(T value);

    /** Checks if this bound can be restricted by the given value. */
    public abstract boolean canBeRestrictedBy(T value);

    /** Checks if this bound overlaps with the given range. Begin and end are inclusive. */
    public abstract boolean overlaps(T begin, T end);

    public static Any ANY = new Any();

    public static class Any<T extends Comparable<T>> extends Bound<T> {
        @Override
        public Bound<T> lt(T value) {
            return new Lt<>(value);
        }

        @Override
        public Bound<T> eq(T value) {
            return new Eq<>(value);
        }

        @Override
        public Bound<T> gt(T value) {
            return new Gt<>(value);
        }

        @Override
        public boolean includes(T value) {
            return true;
        }

        @Override
        public boolean canBeRestrictedBy(T value) {
            return true;
        }

        @Override
        public boolean overlaps(T begin, T end) {
            return true;
        }
    }

    public static class Lt<T extends Comparable<T>> extends Bound<T> {
        private final T lt;

        public Lt(T lt) {
            this.lt = lt;
        }

        @Override
        public Bound<T> lt(T value) {
            if (lt.compareTo(value) <= 0) {
                throw new IllegalArgumentException("restriction must be smaller than current value - current: " + lt + "; restriction: " + value);
            }
            return new Lt<>(value);
        }

        @Override
        public Bound<T> eq(T value) {
            if (lt.compareTo(value) < 0) {
                throw new IllegalArgumentException("illegal equality restriction - current: " + lt + "; restriction: " + value);
            }
            return new Eq<>(value);
        }

        @Override
        public Bound<T> gt(T value) {
            return new In<>(lt, value);
        }

        @Override
        public boolean includes(T value) {
            return value.compareTo(lt) < 0;
        }

        @Override
        public boolean canBeRestrictedBy(T value) {
            return value.compareTo(lt) < 0;
        }

        @Override
        public boolean overlaps(T begin, T end) {
            return begin.compareTo(lt) < 0;
        }
    }

    public static class Eq<T extends Comparable<T>> extends Bound<T> {
        private final T value;

        public Eq(T value) {
            this.value = value;
        }

        @Override
        public Bound<T> lt(T value) {
            throw new IllegalStateException("threshold is already restricted to value: " + this.value);
        }

        @Override
        public Bound<T> eq(T value) {
            throw new IllegalStateException("threshold is already restricted to value: " + this.value);
        }

        @Override
        public Bound<T> gt(T value) {
            throw new IllegalStateException("threshold is already restricted to value: " + this.value);
        }

        @Override
        public boolean includes(T value) {
            return value.compareTo(value) == 0;
        }

        @Override
        public boolean canBeRestrictedBy(T value) {
            return false;
        }

        @Override
        public boolean overlaps(T begin, T end) {
            return begin.compareTo(value) <= 0 && end.compareTo(value) >= 0;
        }
    }

    public static class Gt<T extends Comparable<T>> extends Bound<T> {
        private final T gt;

        public Gt(T gt) {
            this.gt = gt;
        }

        @Override
        public Bound<T> lt(T value) {
            return new In<>(value, gt);
        }

        @Override
        public Bound<T> eq(T value) {
            if (gt.compareTo(value) > 0) {
                throw new IllegalArgumentException("illegal equality restriction - current: " + gt + "; restriction: " + value);
            }
            return new Eq<>(value);
        }

        @Override
        public Bound<T> gt(T value) {
            if (gt.compareTo(value) >= 0) {
                throw new IllegalArgumentException("restriction must be greater than current value - current: " + gt + "; restriction: " + value);
            }
            return new Gt<>(value);
        }

        @Override
        public boolean includes(T value) {
            return value.compareTo(gt) > 0;
        }

        @Override
        public boolean canBeRestrictedBy(T value) {
            return value.compareTo(gt) > 0;
        }

        @Override
        public boolean overlaps(T begin, T end) {
            return end.compareTo(gt) > 0;
        }

    }

    public static class In<T extends Comparable<T>> extends Bound<T> {
        private final T lt, gt;

        public In(T lt, T gt) {
            this.lt = lt;
            this.gt = gt;
        }

        @Override
        public Bound<T> lt(T value) {
            if (lt.compareTo(value) <= 0) {
                throw new IllegalArgumentException("restriction must be smaller than current value - current: " + lt + "; restriction: " + value);
            }
            return new In<>(value, gt);
        }

        @Override
        public Bound<T> eq(T value) {
            if (lt.compareTo(value) < 0 || gt.compareTo(value) > 0) {
                throw new IllegalArgumentException("illegal equality restriction - current: (" + lt + "," + gt + "); restriction: " + value);
            }
            return new Eq<>(value);
        }

        @Override
        public Bound<T> gt(T value) {
            if (gt.compareTo(value) >= 0) {
                throw new IllegalArgumentException("restriction must be greater than current value - current: " + gt + "; restriction: " + value);
            }
            return new In<>(lt, value);
        }

        @Override
        public boolean includes(T value) {
            return value.compareTo(lt) < 0 && value.compareTo(gt) > 0;
        }

        @Override
        public boolean canBeRestrictedBy(T value) {
            return value.compareTo(lt) < 0 && value.compareTo(gt) > 0;
        }

        @Override
        public boolean overlaps(T begin, T end) {
            // the range overlaps this bound if "end > gt" and "begin < lt"
            return end.compareTo(gt) > 0 && begin.compareTo(lt) < 0;
        }

    }
}
