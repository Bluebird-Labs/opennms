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
package org.opennms.core.ipc.sink.offheap;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opennms.core.ipc.sink.api.DispatchQueue;
import org.opennms.core.ipc.sink.api.WriteFailedException;

import org.awaitility.core.ConditionTimeoutException;

public class QueueFileOffHeapDispatchQueueTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void canQueueAndDequeue() throws IOException, WriteFailedException, InterruptedException {
        DispatchQueue<String> queue = new QueueFileOffHeapDispatchQueue<>(String::getBytes, String::new,
                "canQueueAndDequeue", Paths.get(folder.newFolder().toURI()), 1, 1, 10000);

        // Since size is 1, the first entry should be in-memory and the second entry should be on disk
        String payload1 = "msg1";
        queue.enqueue(payload1, "key1");

        String payload2 = "msg2";
        queue.enqueue(payload2, "key2");

        assertThat(queue.getSize(), equalTo(2));

        assertThat(queue.dequeue().getValue(), equalTo(payload1));
        assertThat(queue.dequeue().getValue(), equalTo(payload2));
    }

    @Test
    public void canQueueAndDequeueInParallel() throws IOException {
        DispatchQueue<String> queue = new QueueFileOffHeapDispatchQueue<>(String::getBytes, String::new,
                "canQueueAndDequeueInParallel", Paths.get(folder.newFolder().toURI()), 20, 5, 100_000_000);

        int numEntries = 11_111;
        List<String> toQueue = IntStream.range(0, numEntries)
                .boxed()
                .map(Object::toString)
                .collect(Collectors.toList());

        AtomicInteger count = new AtomicInteger(0);
        CompletableFuture.runAsync(() -> {
            while(count.get() < numEntries) {
                try {
                    queue.enqueue(toQueue.get(count.getAndIncrement()), "key");
                } catch (WriteFailedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        List<String> dequeued = new CopyOnWriteArrayList<>();
        CompletableFuture.runAsync(() -> {
            await().pollDelay(Duration.ofMillis(10)).pollInterval(Duration.ofMillis(10)).until(() -> queue.getSize() > 0);
            while(true) {
                try {
                    dequeued.add(queue.dequeue().getValue());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        await().atMost(1, TimeUnit.MINUTES).until(() -> dequeued, equalTo(toQueue));
    }

    @Test
    public void dequeuesInOrder() throws IOException, WriteFailedException, InterruptedException {
        DispatchQueue<String> queue = new QueueFileOffHeapDispatchQueue<>(String::getBytes, String::new,
                "dequeuesInOrder", Paths.get(folder.newFolder().toURI()), 1000, 100, 10_000_000);

        int numEntries = 10020;
        List<String> toQueue = IntStream.range(0, numEntries)
                .boxed()
                .map(Object::toString)
                .collect(Collectors.toList());

        
        for (String s : toQueue) {
            queue.enqueue(s, "key" + s);
        }

        assertThat(queue.getSize(), equalTo(numEntries));

        List<String> dequeued = new ArrayList<>();
        
        while(queue.getSize() > 0) {
            dequeued.add(queue.dequeue().getValue());
        }
        
        assertThat(dequeued, equalTo(toQueue));
    }

    @Test
    public void blocksWhenEmpty() throws IOException, WriteFailedException {
        DispatchQueue<String> queue = new QueueFileOffHeapDispatchQueue<>(String::getBytes, String::new,
                "blocksWhenEmpty", Paths.get(folder.newFolder().toURI()), 1, 1, 10000);

        AtomicReference<String> atomicString = new AtomicReference<>(null);
        AtomicBoolean receivedValue = new AtomicBoolean(false);

        CompletableFuture.runAsync(() -> {
            try {
                atomicString.set(queue.dequeue().getValue());
                receivedValue.set(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(atomicString.get(), is(nullValue()));

        String payload = "a";

        try {
            await().pollDelay(10, TimeUnit.MILLISECONDS)
                    .atMost(100, TimeUnit.MILLISECONDS)
                    .until(receivedValue::get);
            fail("Dequeue did not block");
        } catch (ConditionTimeoutException expected) {
        }

        queue.enqueue(payload, "key");

        await().pollDelay(10, TimeUnit.MILLISECONDS)
                .atMost(1, TimeUnit.SECONDS)
                .until(() -> Objects.equals(payload, atomicString.get()));
    }

    @Test
    public void blocksWhenFull() throws WriteFailedException, IOException, InterruptedException {
        QueueFileOffHeapDispatchQueue<byte[]> queue = new QueueFileOffHeapDispatchQueue<>(b -> b, b -> b,
                "blocksWhenFull", Paths.get(folder.newFolder().toURI()), 1, 1, 300);

        // Fill the in-memory queue and the file and force a file check
        queue.enqueue(new byte[0], "key1");
        queue.enqueue(new byte[10], "key2");
        queue.checkFileSize();

        AtomicBoolean didQueue = new AtomicBoolean(false);

        CompletableFuture.runAsync(() -> {
            // Now try to queue again and verify that we block
            try {
                queue.enqueue(new byte[1], "key3");
                didQueue.set(true);
            } catch (WriteFailedException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            await().pollDelay(10, TimeUnit.MILLISECONDS)
                    .atMost(100, TimeUnit.MILLISECONDS)
                    .until(didQueue::get);
            fail("Dequeue happened but we should have been blocking");
        } catch (ConditionTimeoutException expected) {
        }

        // Now dequeue which should free up space in the file
        queue.dequeue();
        queue.dequeue();
        queue.checkFileSize();

        await().atMost(1, TimeUnit.SECONDS).until(didQueue::get);
    }

    @Test
    public void canDequeueOffHeapAfterRestart() throws IOException, WriteFailedException, InterruptedException {
        Path path = Paths.get(folder.newFolder().toURI());
        String moduleName = "canQueueAfterRestart";
        DispatchQueue<String> queue = new QueueFileOffHeapDispatchQueue<>(String::getBytes, String::new,
                moduleName, path, 1, 1, 10000);

        // Should be immediate since it should be queued in-memory
        String payload1 = "msg1";
        assertThat(queue.enqueue(payload1, "key1"), equalTo(DispatchQueue.EnqueueResult.IMMEDIATE));

        // Should get deferred since it will be written off-heap
        String payload2 = "msg2";
        assertThat(queue.enqueue(payload2, "key2"), equalTo(DispatchQueue.EnqueueResult.DEFERRED));;

        // Reinitialize to simulate coming back up after restart
        queue = new QueueFileOffHeapDispatchQueue<>(String::getBytes, String::new,
                moduleName, path, 1, 1, 10000);

        // We will have lost the in-memory portion of the queue
        assertThat(queue.getSize(), equalTo(1));
        assertThat(queue.dequeue().getValue(), equalTo(payload2));
    }

    @Test
    public void recoversFromCorruptFile() throws IOException, WriteFailedException, InterruptedException {
        String moduleName = "recoversFromCorruptFile";
        File tmpFolder = folder.newFolder();
        File corruptfile = Paths.get(tmpFolder.getAbsolutePath(), moduleName + ".fifo").toFile();
        Files.write(corruptfile.toPath(), "corrupt!".getBytes());
        QueueFileOffHeapDispatchQueue<String> queue = new QueueFileOffHeapDispatchQueue<>(String::getBytes, String::new,
                moduleName, Paths.get(tmpFolder.toURI()), 1, 1, 300);

        String payload1 = "msg1";
        queue.enqueue(payload1, "key1");

        String received = queue.dequeue().getValue();
        assertThat(received, equalTo(payload1));
    }

}
