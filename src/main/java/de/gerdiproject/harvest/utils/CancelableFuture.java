/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.gerdiproject.harvest.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * An extension of CompletableFuture with the option to cancel a running task.
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
     * @param task the task to be executed asynchronously
     * @param executor the thread pool that handles the asynchronous task
     */
    public CancelableFuture( Callable<T> task, ExecutorService executor )
    {
        this.inner = executor.submit( () -> complete( task ) );
    }


    /**
     * Creates a new CancelableFuture which will be completed by calling the
     * given {@link Callable} via the provided {@link ExecutorService}.
     *
     * @param task the task to be executed asynchronously
     */
    public CancelableFuture( Callable<T> task )
    {
        ExecutorService executor = ForkJoinPool.commonPool();
        this.inner = executor.submit( () -> complete( task ) );
    }


    /**
     * Completes this future by executing a {@link Callable}. If the call throws
     * an exception, the future will complete with that exception. Otherwise,
     * the future will complete with the value returned from the callable.
     */
    private void complete( Callable<T> callable )
    {
        try
        {
            T result = callable.call();
            complete( result );
        }
        catch (Exception e)
        {
            completeExceptionally( e );
        }
    }


    @Override
    public boolean cancel( boolean mayInterrupt )
    {
        return inner.cancel( mayInterrupt ) && super.cancel( true );
    }
}
