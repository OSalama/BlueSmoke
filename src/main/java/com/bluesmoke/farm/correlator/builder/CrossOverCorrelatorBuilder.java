package com.bluesmoke.farm.correlator.builder;

import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.CrossOverCorrelator;
import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.service.feed.FeedService;
import com.bluesmoke.farm.util.ListElement;

import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

public class CrossOverCorrelatorBuilder implements CorrelatorBuilder {

    private CorrelatorPool pool;
    private FeedService feed;
    private CorrelatorBuilderManager correlatorBuilderManager;

    public CrossOverCorrelatorBuilder(CorrelatorPool pool, FeedService feed, CorrelatorBuilderManager correlatorBuilderManager)
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
        double minOrthogonality = Double.POSITIVE_INFINITY;
        GenericCorrelator passiveParent = null;
        for(GenericCorrelator candidate : pool)
        {
            if(candidate != parent
                    && candidate.getAge() > 1000
                    && !candidate.isStateLess()
                    && !parent.isStateLess()
                    && parent.stackPnL.getHead() != null
                    && candidate.stackPnL.getHead() != null
                    && candidate.getNumberOfChildren() < 3)
            {
                double random = 1 + Math.random()/2;
                if(random*candidate.getPnL() > score)
                {
                    ListElement<Double> aP = parent.stackPnL.getHead();
                    ListElement<Double> pP = candidate.stackPnL.getHead();
                    double orthogonality = 0;
                    while(true)
                    {
                        orthogonality += aP.getData() * pP.getData();
                        if(aP.getPrevious() == null || pP.getPrevious() == null)
                        {
                            break;
                        }
                        aP = aP.getPrevious();
                        pP = pP.getPrevious();
                    }
                    orthogonality = Math.abs(orthogonality);
                    if(orthogonality < minOrthogonality)
                    {
                        minOrthogonality = orthogonality;
                        score = random*candidate.getPnL();
                        passiveParent = candidate;
                    }
                }
            }
            else if(candidate != parent && candidate.getAge() > 1000)
            {
                if(Math.random() > 0.9 && candidate.getNumberOfChildren() < 3)
                {
                    passiveParent = candidate;
                }
            }
        }

        if(passiveParent != null)
        {
            if(parent.getCurrentStateValueData() == null || passiveParent.getCurrentStateValueData() == null)
            {
                return;
            }
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

                if(valid && randMap.firstEntry() != null)
                {
                    config.put("passiveUnderlying", randMap.firstEntry().getValue());
                    new CrossOverCorrelator(pool.getNextID(), correlatorBuilderManager, pool, feed, parent, passiveParent, config);
                }
            }
        }
    }
}
