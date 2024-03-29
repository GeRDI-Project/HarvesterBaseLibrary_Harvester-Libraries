/**
 * Copyright © 2017 Robin Weiss (http://www.gerdi-project.de)
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
package de.gerdiproject.harvest.utils;


import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;


/**
 * An extension of {@linkplain CompletableFuture} with the option to cancel a running task.
 *
 * @author http://stackoverflow.com/users/4618331/sam
 * @param <T> the return type of the asynchronous task
 */
public class CancelableFuture<T> extends CompletableFuture<T>
{
    private final Future<?> inner;


    /**
     * Creates a new CancelableFuture which will be completed by calling the
     * given {@link Callable} via the provided {@link ExecutorService}.
     *
     * @param task
     *            the task to be executed asynchronously
     * @param executor
     *            the thread pool that handles the asynchronous task
     */
    public CancelableFuture(final Callable<T> task, final ExecutorService executor)
    {
        super();
        this.inner = executor.submit(() -> complete(task));
    }


    /**
     * Creates a new CancelableFuture which will be completed by calling the
     * given {@link Callable} via the provided {@link ExecutorService}.
     *
     * @param task
     *            the task to be executed asynchronously
     */
    public CancelableFuture(final Callable<T> task)
    {
        super();
        final ExecutorService executor = ForkJoinPool.commonPool();
        this.inner = executor.submit(() -> complete(task));
    }


    /**
     * Completes this future by executing a {@link Callable}. If the call throws
     * an exception, the future will complete with that exception. Otherwise,
     * the future will complete with the value returned from the callable.
     */
    private void complete(final Callable<T> callable)
    {
        try {
            final T result = callable.call();
            complete(result);

        } catch (final Exception e) { // NOPMD required
            completeExceptionally(e);
        }
    }


    @Override
    public boolean cancel(final boolean mayInterrupt)
    {
        return inner.cancel(mayInterrupt) && super.cancel(true);
    }
}
