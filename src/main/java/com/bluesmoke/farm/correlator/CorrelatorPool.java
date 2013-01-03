package com.bluesmoke.farm.correlator;

import com.bluesmoke.farm.listener.FeedListener;
import com.bluesmoke.farm.model.tickdata.Tick;
import com.bluesmoke.farm.service.feed.FeedService;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CorrelatorPool extends ArrayList<GenericCorrelator> implements FeedListener {

    //TODO method to get best PnL correlator

    //TODO method to get correlator based on similarity

    private long id = 0;
    private int currentMaxGen = 0;

    private TreeMap<String, GenericCorrelator> correlators = new TreeMap<String, GenericCorrelator>();
    private ArrayList<GenericCorrelator> toKill = new ArrayList<GenericCorrelator>();

    private TreeMap<Integer, Set<GenericCorrelator>> generationPools = new TreeMap<Integer, Set<GenericCorrelator>>();

    private Tick currentTick;

    private FeedService feed;

    private boolean active = false;

    private int maxPopulation = 200;

    public CorrelatorPool(FeedService feed)
    {
        this.feed = feed;
        feed.subscribe(this);
    }

    public synchronized void addCorrelator(GenericCorrelator correlator)
    {
        add(correlator);
        correlators.put(correlator.getID(), correlator);
        if(!generationPools.containsKey(correlator.getGeneration()))
        {
            generationPools.put(correlator.getGeneration(), new HashSet<GenericCorrelator>());
        }
        generationPools.get(correlator.getGeneration()).add(correlator);
        currentMaxGen = 0;
        int gen = 0;
        for(Set generation : generationPools.values())
        {
            if(generation.size() > 2)
            {
                currentMaxGen = gen;
            }
            gen++;
        }

        System.out.println("Correlator " + correlator.getID() + " added in generation " + correlator.getGeneration());

        if(active)
        {
            correlator.start();
        }
        id++;
    }

    public synchronized void removeCorrelator(GenericCorrelator correlator)
    {
        remove(correlator);
        correlators.remove(correlator.getID());
    }

    public GenericCorrelator getCorrelatorForID(String id)
    {
        return correlators.get(id);
    }

    public String getNextID()
    {
        return "Correlator_" + id;
    }

    public void setMaxPopulation(int population)
    {
        maxPopulation = population;
    }

    public synchronized void onNewTick(Tick tick) {
        long tStart = System.currentTimeMillis();
        currentTick = tick;

        for(Map.Entry<Integer,Set<GenericCorrelator>> generation : generationPools.entrySet())
        {
            Set<GenericCorrelator> generationCorrelators = generation.getValue();
            for(GenericCorrelator correlator : generationCorrelators)
            {
                if(correlator.currentTick != currentTick)
                {
                    correlator.setReady();
                }
            }

            for(GenericCorrelator correlator : generationCorrelators)
            {
                while (correlator.getProcessingStage() != 'F')
                {
                    try {
                        Thread.sleep(0,10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for(GenericCorrelator child : correlator.children)
                {
                    int ready = 0;
                    if(child.getAggressiveParent() != null && child.getAggressiveParent().getProcessingStage() == 'F')
                    {
                        ready++;
                    }
                    else if(child.getAggressiveParent() == null) {
                        ready++;
                    }

                    if(child.getPassiveParent() != null && child.getPassiveParent().getProcessingStage() == 'F')
                    {
                        ready++;
                    }
                    else if(child.getPassiveParent() == null) {
                        ready++;
                    }

                    if(correlator.currentTick != currentTick)
                    {
                        ready++;
                    }

                    if(ready == 3)
                    {
                        child.setReady();
                    }
                }
                //System.out.println("Correlator " + correlator.getID() + " finished processing");
            }
            //System.out.println("Generation " + generation.getKey() + " finished processing");
        }
        removeKilled();
        long tEnd = System.currentTimeMillis();

        if(tEnd - tStart > 500)
        {
            System.out.println("Farm overloading: " + (tEnd - tStart) + "ms");
        }
    }

    public void causeMutationWave()
    {
        int maxPop = (maxPopulation / 10) * (generationPools.size());
        if(maxPop > maxPopulation)
        {
            maxPop = maxPopulation;
        }
        double threshold = Math.pow((double) size() / maxPop, 0.03375);
        List<GenericCorrelator> correlators = new ArrayList<GenericCorrelator>(this);
        for(GenericCorrelator correlator : correlators)
        {
            if(Math.random() > threshold)
            {
                correlator.createMutant();
                if(correlator.getGeneration() == currentMaxGen)
                {
                    correlator.createMutant();
                }
            }
        }
    }

    public void causeBreedingWave()
    {
        int maxPop = (maxPopulation / 10) * (generationPools.size());
        if(maxPop > maxPopulation)
        {
            maxPop = maxPopulation;
        }
        double threshold = Math.pow((double) size() / maxPop, 0.0675);
        List<GenericCorrelator> correlators = new ArrayList<GenericCorrelator>(this);
        for(GenericCorrelator correlator : correlators)
        {
            if(Math.random() > threshold)
            {
                if(correlator.getAge() > 2000)
                {
                    correlator.spawn();
                    if(correlator.getGeneration() == currentMaxGen)
                    {
                        correlator.spawn();
                    }
                }
            }
        }
    }

    public void causeDeathWave()
    {
        TreeMap<Double, GenericCorrelator> dieableCorrelators = new TreeMap<Double, GenericCorrelator>();
        List<GenericCorrelator> correlators = new ArrayList<GenericCorrelator>(this);

        double minPnL = 0;
        for(GenericCorrelator correlator : correlators)
        {
            if(correlator.getPnL() < minPnL)
            {
                minPnL = correlator.getPnL();
            }
        }
        for(GenericCorrelator correlator : correlators)
        {
            if(correlator.getAge() > 10000 && correlator.getCurrentStateValueData() == null)
            {
                correlator.killLineage();
            }
            else if(correlator.getAge() > 1000 && correlator.getPnL() == 0)
            {
                correlator.die();
            }
            else
            {
                double pnl = correlator.getPnL();
                if(correlator.children.size() == 0 && correlator.getAge() > 5000 && correlator.getPnL() != 0)
                {
                    dieableCorrelators.put(Math.log(pnl - minPnL + 1)*Math.random(), correlator);
                }
            }
        }

        int i = 0;
        for(GenericCorrelator correlator : dieableCorrelators.values())
        {
            i++;
            if(i > dieableCorrelators.size()/0.5)
            {
                break;
            }

            correlator.die();
        }

    }

    public Tick getCurrentTick()
    {
        return currentTick;
    }

    public void activate()
    {
        feed.resume();
        active = true;
        for(GenericCorrelator correlator : this)
        {
            correlator.start();
        }
    }

    public void reset()
    {
        for(GenericCorrelator correlator : this)
        {
            try
            {
                correlator.toInterrupt();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            correlator.reset();
        }
        feed.pause();
        feed.reset();
    }

    public synchronized void toKill(GenericCorrelator correlator)
    {
        toKill.add(correlator);
    }

    private void removeKilled()
    {
        for(GenericCorrelator correlator : toKill)
        {
            generationPools.get(correlator.getGeneration()).remove(correlator);
            removeCorrelator(correlator);
            correlator.interrupt();
        }
        toKill.clear();
    }

    public void pauseFeed()
    {
        feed.pause();
    }
    public void resumeFeed()
    {
        feed.resume();
    }

    public String getHandlesInfo()
    {
        String info = "";
        for(GenericCorrelator correlator : this)
        {
            info += correlator.getHandlesInfo();
        }
        info += "Population:" + size() + "\n";
        int genPop = 0;
        for(Set<GenericCorrelator> correlators : generationPools.values())
        {
            genPop += correlators.size();
        }
        info += "Population in GenPools:" + genPop + "\n";

        return info;
    }
}
