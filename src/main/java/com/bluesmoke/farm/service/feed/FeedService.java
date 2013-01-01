package com.bluesmoke.farm.service.feed;

import com.bluesmoke.farm.exception.InvalidTimeStampException;
import com.bluesmoke.farm.listener.FeedListener;
import com.bluesmoke.farm.model.tickdata.Tick;
import com.bluesmoke.farm.service.GenericService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class FeedService implements GenericService {

    private ConcurrentLinkedQueue<FeedListener> listeners = new ConcurrentLinkedQueue<FeedListener>();
    protected Tick currentTick;
    protected boolean hasNext = true;

    private boolean paused = false;

    public abstract Tick getNextTick() throws InvalidTimeStampException;

    public abstract void reset();

    public abstract double getPercentageComplete();

    public void subscribe(FeedListener listener)
    {
        listeners.add(listener);
    }

    public void unSubscribe(FeedListener listener)
    {
        listeners.remove(listener);
    }

    public void broadcastTick()
    {
        for(FeedListener listener : listeners)
        {
            listener.onNewTick(currentTick);
        }
    }

    public synchronized boolean getAndBroadcastNextTick()
    {
        try{
            getNextTick();
        }
        catch (InvalidTimeStampException e)
        {
            e.printStackTrace();
        }
        broadcastTick();
        return hasNext;
    }

    public boolean isPaused()
    {
        return paused;
    }

    public void pause()
    {
        paused = true;
    }

    public void resume()
    {
        paused = false;
    }
}
