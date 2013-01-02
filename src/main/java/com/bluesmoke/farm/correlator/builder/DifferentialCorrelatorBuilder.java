package com.bluesmoke.farm.correlator.builder;

import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.DifferentialCorrelator;
import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.service.feed.FeedService;

import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

public class DifferentialCorrelatorBuilder implements CorrelatorBuilder {

    private CorrelatorPool pool;
    private FeedService feed;
    private CorrelatorBuilderManager correlatorBuilderManager;

    public DifferentialCorrelatorBuilder(CorrelatorPool pool, FeedService feed, CorrelatorBuilderManager correlatorBuilderManager)
    {
        this.pool = pool;
        this.feed = feed;
        this.correlatorBuilderManager = correlatorBuilderManager;
    }

    public void build(GenericCorrelator parent, GenericCorrelator passiveParent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void build(GenericCorrelator parent) {
        Random rand = new Random();
        int timeSpan = rand.nextInt(4);
        timeSpan = (int) Math.pow(10, timeSpan);

        HashMap<String, Object> config = new HashMap<String, Object>();
        config.put("timeSpan" , timeSpan);

        boolean valid = false;

        double score = Double.NEGATIVE_INFINITY;
        GenericCorrelator passiveParent = null;
        for(GenericCorrelator candidate : pool)
        {
            if(candidate != parent && candidate.getAge() > 1000)
            {
                double random = Math.random();
                if(random*candidate.getPnL() > score)
                {
                    score = random*candidate.getPnL();
                    passiveParent = candidate;
                }
            }
        }

        if(Math.random() > 0.5 || passiveParent == null)
        {
            if(parent.getCurrentStateValueData() == null)
            {
                return;
            }
            if(Math.random() > 0.8)
            {
                config.put("timeSpan" , 0);
            }
            config.put("type" , "Single");
            TreeMap<Double, String> randMap = new TreeMap<Double, String>();
            if(parent.getCurrentUnderlyingComponents() != null)
            {
                for(String underlying : parent.getCurrentUnderlyingComponents().keySet())
                {
                    if(parent.getCurrentUnderlyingComponents().get(underlying).getClass() == Double.class)
                    {
                        randMap.put(Math.random(), underlying);
                        valid = true;
                    }
                }
            }
            if(valid)
            {
                config.put("aggressiveUnderlying", randMap.firstEntry().getValue());
                new DifferentialCorrelator(pool.getNextID(), correlatorBuilderManager, pool, feed, parent, null, config);
            }
        }
        else {
            if(parent.getCurrentStateValueData() == null || passiveParent.getCurrentStateValueData() == null)
            {
                return;
            }
            config.put("type" , "Double");
            TreeMap<Double, String> randMap = new TreeMap<Double, String>();
            if(parent.getCurrentUnderlyingComponents() != null)
            {
                for(String underlying : parent.getCurrentUnderlyingComponents().keySet())
                {
                    if(parent.getCurrentUnderlyingComponents().get(underlying).getClass() == Double.class)
                    {
                        randMap.put(Math.random(), underlying);
                        valid = true;
                    }
                }
            }

            if(valid)
            {
                valid = false;
                config.put("aggressiveUnderlying", randMap.firstEntry().getValue());
                randMap.clear();
                for(String underlying : passiveParent.getCurrentUnderlyingComponents().keySet())
                {
                    if(passiveParent.getCurrentUnderlyingComponents().get(underlying).getClass() == Double.class)
                    {
                        randMap.put(Math.random(), underlying);
                        valid = true;
                    }
                }

                if(valid)
                {
                    config.put("passiveUnderlying", randMap.firstEntry().getValue());
                    new DifferentialCorrelator(pool.getNextID(), correlatorBuilderManager, pool, feed, parent, passiveParent, config);
                }
            }
        }
    }
}
