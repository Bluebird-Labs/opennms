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
package org.opennms.netmgt.telemetry.protocols.sflow.parser.proto.flows;

import org.bson.BsonWriter;
import org.opennms.netmgt.telemetry.listeners.utils.BufferUtils;
import org.opennms.netmgt.telemetry.protocols.sflow.parser.SampleDatagramEnrichment;
import org.opennms.netmgt.telemetry.protocols.sflow.parser.InvalidPacketException;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedLong;

import io.netty.buffer.ByteBuf;

// struct memcache_counters {
//   unsigned int cmd_set;
//   unsigned int cmd_touch;
//   unsigned int cmd_flush;
//   unsigned int get_hits;
//   unsigned int get_misses;
//   unsigned int delete_hits;
//   unsigned int delete_misses;
//   unsigned int incr_hits;
//   unsigned int incr_misses;
//   unsigned int decr_hits;
//   unsigned int decr_misses;
//   unsigned int cas_hits;
//   unsigned int cas_misses;
//   unsigned int cas_badval;
//   unsigned int auth_cmds;
//   unsigned int auth_errors;
//   unsigned int threads;
//   unsigned int conn_yields;
//   unsigned int listen_disabled_num;
//   unsigned int curr_connections;
//   unsigned int rejected_connections;
//   unsigned int total_connections;
//   unsigned int connection_structures;
//   unsigned int evictions;
//   unsigned int reclaimed;
//   unsigned int curr_items;
//   unsigned int total_items;
//   unsigned hyper bytes_read;
//   unsigned hyper bytes_written;
//   unsigned hyper bytes;
//   unsigned hyper limit_maxbytes;
// };

public class MemcacheCounters implements CounterData {
    public final long cmd_set;
    public final long cmd_touch;
    public final long cmd_flush;
    public final long get_hits;
    public final long get_misses;
    public final long delete_hits;
    public final long delete_misses;
    public final long incr_hits;
    public final long incr_misses;
    public final long decr_hits;
    public final long decr_misses;
    public final long cas_hits;
    public final long cas_misses;
    public final long cas_badval;
    public final long auth_cmds;
    public final long auth_errors;
    public final long threads;
    public final long conn_yields;
    public final long listen_disabled_num;
    public final long curr_connections;
    public final long rejected_connections;
    public final long total_connections;
    public final long connection_structures;
    public final long evictions;
    public final long reclaimed;
    public final long curr_items;
    public final long total_items;
    public final UnsignedLong bytes_read;
    public final UnsignedLong bytes_written;
    public final UnsignedLong bytes;
    public final UnsignedLong limit_maxbytes;

    public MemcacheCounters(final ByteBuf buffer) throws InvalidPacketException {
        this.cmd_set = BufferUtils.uint32(buffer);
        this.cmd_touch = BufferUtils.uint32(buffer);
        this.cmd_flush = BufferUtils.uint32(buffer);
        this.get_hits = BufferUtils.uint32(buffer);
        this.get_misses = BufferUtils.uint32(buffer);
        this.delete_hits = BufferUtils.uint32(buffer);
        this.delete_misses = BufferUtils.uint32(buffer);
        this.incr_hits = BufferUtils.uint32(buffer);
        this.incr_misses = BufferUtils.uint32(buffer);
        this.decr_hits = BufferUtils.uint32(buffer);
        this.decr_misses = BufferUtils.uint32(buffer);
        this.cas_hits = BufferUtils.uint32(buffer);
        this.cas_misses = BufferUtils.uint32(buffer);
        this.cas_badval = BufferUtils.uint32(buffer);
        this.auth_cmds = BufferUtils.uint32(buffer);
        this.auth_errors = BufferUtils.uint32(buffer);
        this.threads = BufferUtils.uint32(buffer);
        this.conn_yields = BufferUtils.uint32(buffer);
        this.listen_disabled_num = BufferUtils.uint32(buffer);
        this.curr_connections = BufferUtils.uint32(buffer);
        this.rejected_connections = BufferUtils.uint32(buffer);
        this.total_connections = BufferUtils.uint32(buffer);
        this.connection_structures = BufferUtils.uint32(buffer);
        this.evictions = BufferUtils.uint32(buffer);
        this.reclaimed = BufferUtils.uint32(buffer);
        this.curr_items = BufferUtils.uint32(buffer);
        this.total_items = BufferUtils.uint32(buffer);
        this.bytes_read = BufferUtils.uint64(buffer);
        this.bytes_written = BufferUtils.uint64(buffer);
        this.bytes = BufferUtils.uint64(buffer);
        this.limit_maxbytes = BufferUtils.uint64(buffer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cmd_set", this.cmd_set)
                .add("cmd_touch", this.cmd_touch)
                .add("cmd_flush", this.cmd_flush)
                .add("get_hits", this.get_hits)
                .add("get_misses", this.get_misses)
                .add("delete_hits", this.delete_hits)
                .add("delete_misses", this.delete_misses)
                .add("incr_hits", this.incr_hits)
                .add("incr_misses", this.incr_misses)
                .add("decr_hits", this.decr_hits)
                .add("decr_misses", this.decr_misses)
                .add("cas_hits", this.cas_hits)
                .add("cas_misses", this.cas_misses)
                .add("cas_badval", this.cas_badval)
                .add("auth_cmds", this.auth_cmds)
                .add("auth_errors", this.auth_errors)
                .add("threads", this.threads)
                .add("conn_yields", this.conn_yields)
                .add("listen_disabled_num", this.listen_disabled_num)
                .add("curr_connections", this.curr_connections)
                .add("rejected_connections", this.rejected_connections)
                .add("total_connections", this.total_connections)
                .add("connection_structures", this.connection_structures)
                .add("evictions", this.evictions)
                .add("reclaimed", this.reclaimed)
                .add("curr_items", this.curr_items)
                .add("total_items", this.total_items)
                .add("bytes_read", this.bytes_read)
                .add("bytes_written", this.bytes_written)
                .add("bytes", this.bytes)
                .add("limit_maxbytes", this.limit_maxbytes)
                .toString();
    }

    @Override
    public void writeBson(final BsonWriter bsonWriter, final SampleDatagramEnrichment enr) {
        bsonWriter.writeStartDocument();
        bsonWriter.writeInt64("cmd_set", this.cmd_set);
        bsonWriter.writeInt64("cmd_touch", this.cmd_touch);
        bsonWriter.writeInt64("cmd_flush", this.cmd_flush);
        bsonWriter.writeInt64("get_hits", this.get_hits);
        bsonWriter.writeInt64("get_misses", this.get_misses);
        bsonWriter.writeInt64("delete_hits", this.delete_hits);
        bsonWriter.writeInt64("delete_misses", this.delete_misses);
        bsonWriter.writeInt64("incr_hits", this.incr_hits);
        bsonWriter.writeInt64("incr_misses", this.incr_misses);
        bsonWriter.writeInt64("decr_hits", this.decr_hits);
        bsonWriter.writeInt64("decr_misses", this.decr_misses);
        bsonWriter.writeInt64("cas_hits", this.cas_hits);
        bsonWriter.writeInt64("cas_misses", this.cas_misses);
        bsonWriter.writeInt64("cas_badval", this.cas_badval);
        bsonWriter.writeInt64("auth_cmds", this.auth_cmds);
        bsonWriter.writeInt64("auth_errors", this.auth_errors);
        bsonWriter.writeInt64("threads", this.threads);
        bsonWriter.writeInt64("conn_yields", this.conn_yields);
        bsonWriter.writeInt64("listen_disabled_num", this.listen_disabled_num);
        bsonWriter.writeInt64("curr_connections", this.curr_connections);
        bsonWriter.writeInt64("rejected_connections", this.rejected_connections);
        bsonWriter.writeInt64("total_connections", this.total_connections);
        bsonWriter.writeInt64("connection_structures", this.connection_structures);
        bsonWriter.writeInt64("evictions", this.evictions);
        bsonWriter.writeInt64("reclaimed", this.reclaimed);
        bsonWriter.writeInt64("curr_items", this.curr_items);
        bsonWriter.writeInt64("total_items", this.total_items);
        bsonWriter.writeInt64("bytes_read", this.bytes_read.longValue());
        bsonWriter.writeInt64("bytes_written", this.bytes_written.longValue());
        bsonWriter.writeInt64("bytes", this.bytes.longValue());
        bsonWriter.writeInt64("limit_maxbytes", this.limit_maxbytes.longValue());
        bsonWriter.writeEndDocument();
    }
}
