package com.bluesmoke.farm.worker;

import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.service.feed.FeedService;

public class PassageOfTimeEmulationWorker implements Runnable
{
    private FeedService feed;
    private CorrelatorPool pool;
    private boolean feedHasNext = true;
    private Thread thread;
    private long tLastGC = 0;

    private long initialFreeMemory = Runtime.getRuntime().freeMemory();

    public PassageOfTimeEmulationWorker(FeedService feed, CorrelatorPool pool)
    {
        this.feed = feed;
        this.pool = pool;
    }

    public void startEmulation()
    {
        die();
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        pool.activate();
        while (feedHasNext)
        {
            if(feed.isPaused())
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                feedHasNext = feed.getAndBroadcastNextTick();

                long freeMem = Runtime.getRuntime().freeMemory();
                long usedMem = initialFreeMemory - freeMem;

                if((double)usedMem / (double)initialFreeMemory > 0.5)
                {
                    pool.causeDeathWave();
                    if(System.currentTimeMillis() - tLastGC > 20000)
                    {
                        tLastGC = System.currentTimeMillis();
                        System.gc();
                    }
                }
                else if(Math.random() > 0.99)
                {
                    pool.causeMutationWave();
                    pool.causeBreedingWave();
                    pool.causeDeathWave();
                }
            }
        }
        pool.reset();
        feedHasNext = true;
        System.out.println("Run complete!");
    }

    public void stop()
    {
        feedHasNext = false;
    }

    public void die()
    {
        if(thread != null)
        {
            feedHasNext = false;
            pool.reset();
        }
    }
}
