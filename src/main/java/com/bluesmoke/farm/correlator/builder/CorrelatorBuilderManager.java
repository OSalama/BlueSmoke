package com.bluesmoke.farm.correlator.builder;

import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.service.feed.FeedService;

import java.util.HashMap;
import java.util.TreeMap;

public class CorrelatorBuilderManager {
    private HashMap<CorrelatorBuilder, Double> builders = new HashMap<CorrelatorBuilder, Double>();
    private CorrelatorPool pool;
    private FeedService feed;

    public CorrelatorBuilderManager(CorrelatorPool pool, FeedService feed)
    {
        this.pool = pool;
        this.feed = feed;

        builders.put(new DescendantCorrelatorBuilder(pool, feed, this), 0.5);
        builders.put(new DifferentialCorrelatorBuilder(pool, feed, this), 0.5);
        builders.put(new CrossOverCorrelatorBuilder(pool, feed, this), 0.5);
        builders.put(new DateTimeCorrelatorBuilder(pool, feed, this), 0.1);
    }

    public void addBuilder(CorrelatorBuilder builder, double aggressivity)
    {
        builders.put(builder, aggressivity);
    }

    public void build(GenericCorrelator aggressiveParent, GenericCorrelator passiveParent)
    {
        TreeMap<Double, CorrelatorBuilder> randMap = new TreeMap<Double, CorrelatorBuilder>();
        for(CorrelatorBuilder builder : builders.keySet())
        {
            randMap.put(Math.random() * builders.get(builder), builder);
        }

        randMap.firstEntry().getValue().build(aggressiveParent, passiveParent);
    }

    public void build(GenericCorrelator parent)
    {
        TreeMap<Double, CorrelatorBuilder> randMap = new TreeMap<Double, CorrelatorBuilder>();
        for(CorrelatorBuilder builder : builders.keySet())
        {
            randMap.put(Math.random() * builders.get(builder), builder);
        }

        randMap.firstEntry().getValue().build(parent);
    }
}
