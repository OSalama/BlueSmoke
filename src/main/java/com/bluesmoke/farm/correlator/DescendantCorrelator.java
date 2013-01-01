package com.bluesmoke.farm.correlator;

import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;
import com.bluesmoke.farm.enumeration.descendant.config.PassiveParentConfig;
import com.bluesmoke.farm.model.correlatordata.StateValueData;
import com.bluesmoke.farm.service.feed.FeedService;

import java.util.*;

public class DescendantCorrelator extends GenericCorrelator {

    public DescendantCorrelator(String id, CorrelatorBuilderManager correlatorBuilderManager, CorrelatorPool pool, FeedService feed, GenericCorrelator aggressiveParent, GenericCorrelator passiveParent, HashMap<String, Object> config)
    {
        super("Descendant_" + id, correlatorBuilderManager, pool, feed, aggressiveParent, passiveParent, config);
    }

    @Override
    public void createMutant() {

        GenericCorrelator aggressiveParent = this.aggressiveParent;
        GenericCorrelator passiveParent = this.passiveParent;

        if(Math.random() > 0.5)
        {
            aggressiveParent = this.passiveParent;
            passiveParent = this.aggressiveParent;
        }

        createDescendantFromParents(aggressiveParent, passiveParent);
    }

    @Override
    public String createState() {
        String state = null;
        try
        {
            StateValueData aParentStateValueData = aggressiveParent.getCurrentStateValueData();
            StateValueData pParentStateValueData = passiveParent.getCurrentStateValueData();

            currentUnderlyingComponents.clear();
            if(aParentStateValueData != null && pParentStateValueData != null)
            {
                state = "";
                List<Integer> aParentStateComponents = (List<Integer>) config.get("aParentStateComponents");
                for(int i : aParentStateComponents)
                {
                    state += aParentStateValueData.getStateComponent(i) + ",";
                    currentUnderlyingComponents.put("aParentStateComponents:" + i, aParentStateValueData.getStateComponent(i));
                }

                List<Map.Entry<PassiveParentConfig, Integer>> passiveParentConfig = (List<Map.Entry<PassiveParentConfig, Integer>>) config.get("passiveParentConfig");

                for(Map.Entry component : passiveParentConfig)
                {
                    if(component.getKey().equals(PassiveParentConfig.AVERAGE))
                    {
                        state += (int)(pParentStateValueData.getAverage()/ pParentStateValueData.getRes()) + ",";
                        currentUnderlyingComponents.put("pParentStateComponents:" + PassiveParentConfig.AVERAGE, pParentStateValueData.getAverage());
                    }
                    else if(component.getKey().equals(PassiveParentConfig.SDEV))
                    {
                        state += (int)(pParentStateValueData.getSDev()/ pParentStateValueData.getRes()) + ",";
                        currentUnderlyingComponents.put("pParentStateComponents:" + PassiveParentConfig.SDEV, pParentStateValueData.getSDev());
                    }
                    else if(component.getKey().equals(PassiveParentConfig.SHARPE))
                    {
                        state += (int)(10 * pParentStateValueData.getSharpe()) + ",";
                        currentUnderlyingComponents.put("pParentStateComponents:" + PassiveParentConfig.SDEV, pParentStateValueData.getSharpe());
                    }
                    else if(component.getKey().equals(PassiveParentConfig.MODE))
                    {
                        int mode = 0;
                        double freq = 0;
                        TreeMap<Integer, Double> dist = pParentStateValueData.getDist().get(component.getValue());
                        if(dist != null)
                        {
                            for(int actionClass : dist.keySet())
                            {
                                if(dist.get(actionClass) > freq)
                                {
                                    freq = dist.get(actionClass);
                                    mode = actionClass;
                                }
                            }
                        }
                        state += mode + ",";
                        currentUnderlyingComponents.put("pParentStateComponents:" + PassiveParentConfig.MODE, mode);
                    }
                    else if(component.getKey().equals(PassiveParentConfig.MODE_P))
                    {
                        int mode = 0;
                        double freq = 0;
                        TreeMap<Integer, Double> dist = pParentStateValueData.getDist().get(component.getValue());
                        if(dist != null)
                        {
                            for(int actionClass : dist.keySet())
                            {
                                if(actionClass > 0)
                                {
                                    if(dist.get(actionClass) > freq)
                                    {
                                        freq = dist.get(actionClass);
                                        mode = actionClass;
                                    }
                                }
                            }
                        }
                        state += mode + ",";
                        currentUnderlyingComponents.put("pParentStateComponents:" + PassiveParentConfig.MODE_P, mode);

                    }
                    else if(component.getKey().equals(PassiveParentConfig.MODE_N))
                    {
                        int mode = 0;
                        double freq = 0;
                        TreeMap<Integer, Double> dist = pParentStateValueData.getDist().get(component.getValue());
                        if(dist != null)
                        {
                            for(int actionClass : dist.keySet())
                            {
                                if(actionClass < 0)
                                {
                                    if(dist.get(actionClass) > freq)
                                    {
                                        freq = dist.get(actionClass);
                                        mode = actionClass;
                                    }
                                }
                            }
                        }
                        state += mode + ",";
                        currentUnderlyingComponents.put("pParentStateComponents:" + PassiveParentConfig.MODE_N, mode);
                    }
                }

                state = state.substring(0, state.length() - 1);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //feed.pause();
            killLineage();
            return null;
        }
        return state;
    }

    private void createDescendantFromParents(GenericCorrelator aggressiveParent, GenericCorrelator passiveParent)
    {
        int maxADimensions = 7;
        int maxPDimensions = 3;

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
}
