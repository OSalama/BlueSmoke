package com.bluesmoke.farm.correlator.builder;

import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.DescendantCorrelator;
import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.enumeration.descendant.config.PassiveParentConfig;
import com.bluesmoke.farm.service.feed.FeedService;
import com.bluesmoke.farm.util.ListElement;

import java.util.*;

public class DescendantCorrelatorBuilder implements CorrelatorBuilder {

    private CorrelatorPool pool;
    private FeedService feed;
    private CorrelatorBuilderManager correlatorBuilderManager;

    public DescendantCorrelatorBuilder(CorrelatorPool pool, FeedService feed, CorrelatorBuilderManager correlatorBuilderManager)
    {
        this.pool = pool;
        this.feed = feed;
        this.correlatorBuilderManager = correlatorBuilderManager;
    }

    public void build(GenericCorrelator aggressiveParent, GenericCorrelator passiveParent) {
        int maxADimensions = 7;
        int maxPDimensions = 3;

        if(aggressiveParent.getCurrentStateValueData() == null || passiveParent.getCurrentStateValueData() == null)
        {
            return;
        }

        int aPComponentsNum = aggressiveParent.getStateComponentsNumber();
        List<Integer> aParentStateComponents = new ArrayList<Integer>();
        TreeMap<Double, Object> randMap = new TreeMap<Double, Object>();
        for(int i = 0; i < aPComponentsNum; i++)
        {
            randMap.put(Math.random(), i);
        }

        for(int i = 0; i < randMap.size() && i < maxADimensions; i++)
        {
            aParentStateComponents.add((Integer)randMap.values().toArray()[i]);
        }
        randMap.clear();

        List<Map.Entry<PassiveParentConfig, Integer>> passiveParentConfig = new ArrayList<Map.Entry<PassiveParentConfig, Integer>>();
        for(PassiveParentConfig descendantConfigValue : PassiveParentConfig.values())
        {
            randMap.put(Math.random(), descendantConfigValue);
        }
        Random rand = new Random();
        for(int i = 0; i < randMap.size() && i < maxPDimensions; i++)
        {
            passiveParentConfig.add(new AbstractMap.SimpleEntry<PassiveParentConfig, Integer>((PassiveParentConfig)randMap.values().toArray()[i], rand.nextInt(4)));
        }

        HashMap<String, Object> config = new HashMap<String, Object>();
        config.put("aParentStateComponents", aParentStateComponents);
        config.put("passiveParentConfig", passiveParentConfig);
        new DescendantCorrelator(pool.getNextID(), correlatorBuilderManager, pool, feed, aggressiveParent, passiveParent, config);
    }

    public void build(GenericCorrelator parent) {
        if(parent.isStateLess())
        {
            return;
        }

        double score = Double.NEGATIVE_INFINITY;
        double minOrthogonality = Double.POSITIVE_INFINITY;
        GenericCorrelator mate = null;
        for(GenericCorrelator candidate : pool)
        {
            if(candidate != parent
                    && candidate.getAge() > 1000
                    && !candidate.isStateLess()
                    && parent.stackPnL.getHead() != null
                    && candidate.stackPnL.getHead() != null
                    && candidate.getNumberOfChildren() < 3)
            {
                double rand = 1 + Math.random()/2;
                if(rand*candidate.getPnL() > score)
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
                    orthogonality = (Math.random() * Math.abs(orthogonality));
                    if(orthogonality < minOrthogonality)
                    {
                        minOrthogonality = orthogonality;
                        score = rand*candidate.getPnL();
                        mate = candidate;
                    }
                }
            }
        }
        if(mate != null)
        {
            GenericCorrelator aggressiveParent = parent;
            GenericCorrelator passiveParent = mate;

            build(aggressiveParent, passiveParent);
        }
    }
}
