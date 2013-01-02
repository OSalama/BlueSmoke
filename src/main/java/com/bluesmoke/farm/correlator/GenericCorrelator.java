package com.bluesmoke.farm.correlator;

import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;
import com.bluesmoke.farm.enumeration.PairResolution;
import com.bluesmoke.farm.enumeration.descendant.config.PassiveParentConfig;
import com.bluesmoke.farm.enumeration.Pair;
import com.bluesmoke.farm.exception.IllegalStateValueDataModificationException;
import com.bluesmoke.farm.listener.FeedListener;
import com.bluesmoke.farm.model.correlatordata.StateValueData;
import com.bluesmoke.farm.model.pnl.OpenOrder;
import com.bluesmoke.farm.model.tickdata.Tick;
import com.bluesmoke.farm.service.feed.FeedService;
import com.bluesmoke.farm.util.FixedSizeStackArrayList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GenericCorrelator extends Thread{

    private String id;
    private int generation = 0;
    private String passCode;
    private long age = 0;

    private long breedingAge;

    public double pnl = 0;
    private boolean toInterrupt = false;

    private int stateComponentsNumber = -1;

    protected String currentStateValueID;
    protected TreeMap<String, Object> currentUnderlyingComponents;

    protected Pair pair;

    protected GenericCorrelator aggressiveParent;
    protected GenericCorrelator passiveParent;
    protected HashMap<String, Object> config = new HashMap<String, Object>();
    protected ArrayList<GenericCorrelator> children = new ArrayList<GenericCorrelator>();

    protected int numTicksObserved;
    protected double resolution;
    public double confidenceProfit = 0.6;
    public double confidenceLoss = 0.1;

    protected ArrayList<Tick> ticks = new ArrayList<Tick>();
    protected Tick currentTick;

    private char processingStage = 'F';
    private boolean alive = true;

    protected ConcurrentHashMap<String, Object> correlatorData = new ConcurrentHashMap<String, Object>();
    protected TreeMap<String, StateValueData> memory = new TreeMap<String, StateValueData>();
    protected FixedSizeStackArrayList<StateValueData> stackStateValueData = new FixedSizeStackArrayList<StateValueData>(5000);
    protected FixedSizeStackArrayList<TreeMap<String, Object>> stackUnderlyingComponents = new FixedSizeStackArrayList<TreeMap<String, Object>>(5000);

    protected HashSet<StateValueData> aliveStates = new HashSet<StateValueData>();
    protected HashSet<StateValueData> aliveStatesToAdd = new HashSet<StateValueData>();
    protected HashSet<StateValueData> aliveStatesToKill = new HashSet<StateValueData>();

    private TreeMap<Integer, TreeMap<Integer, Long>> successGrid = new TreeMap<Integer, TreeMap<Integer, Long>>();

    protected CorrelatorPool pool;
    protected FeedService feed;
    protected CorrelatorBuilderManager correlatorBuilderManager;

    public GenericCorrelator(String id, CorrelatorBuilderManager correlatorBuilderManager, CorrelatorPool pool, FeedService feed, GenericCorrelator aggressiveParent, GenericCorrelator passiveParent, HashMap<String, Object> config)
    {
        this.id = id;
        this.pool = pool;
        this.feed = feed;
        this.correlatorBuilderManager = correlatorBuilderManager;

        passCode = "" + Math.random();

        this.aggressiveParent = aggressiveParent;
        this.passiveParent = passiveParent;
        if(aggressiveParent != null)
        {
            generation = aggressiveParent.getGeneration() + 1;
            aggressiveParent.addChild(this);
        }
        if(passiveParent != null)
        {
            if(passiveParent.getGeneration() >= generation)
            {
                generation = passiveParent.getGeneration() + 1;
            }
            passiveParent.addChild(this);
        }
        if(config != null)
        {
            this.config = config;
        }

        if(aggressiveParent != null)
        {
            this.setBreedingAge(aggressiveParent.getBreedingAge());
            this.setNumberTicksObserved(aggressiveParent.getNumTicksObserved());
        }
        else {
            Random rand = new Random();
            setNumberTicksObserved((int) Math.pow(2, rand.nextInt(7) + 1));
            setBreedingAge(10000);
        }

        TreeMap<Double, Pair> randMap = new TreeMap<Double, Pair>();
        for(Pair pair : Pair.values())
        {
            randMap.put(Math.random(), pair);
        }

        Pair trackedPair = randMap.firstEntry().getValue();

        setTrackedPairAndMetric(trackedPair);

        pool.addCorrelator(this);
    }

    public void toInterrupt()
    {
        toInterrupt = true;
    }

    public void reset()
    {
        correlatorData.clear();
        stackStateValueData.clear();
        stackUnderlyingComponents.clear();
        memory.clear();
        processingStage = 'F';
        age = 0;
    }

    public void addChild(GenericCorrelator child)
    {
        children.add(child);
    }

    public void setTrackedPairAndMetric(Pair pair)
    {
        this.pair = pair;
        config.put("pair", pair);

        this.resolution = PairResolution.getResolution(pair);
        config.put("resolution", resolution);
    }

    public void setNumberTicksObserved(int num)
    {
        this.numTicksObserved = num;
        config.put("numTicksObserved", num);
    }

    public void setBreedingAge(long breedingAge)
    {
        this.breedingAge = breedingAge;
        config.put("breedingAge", breedingAge);
    }

    public void setConfidenceLevels()
    {
        if(config.containsKey("confidenceProfit"))
        {
            confidenceProfit = (Double)config.get("confidenceProfit") + Math.random()/10 - 0.05;
        }
        else {
            confidenceProfit = Math.random()/2 + 0.5;
        }
        if(config.containsKey("confidenceLoss"))
        {
            confidenceLoss = (Double)config.get("confidenceLoss") + Math.random()/10 - 0.05;
        }
        else {
            confidenceLoss = Math.random()/2;
        }
        config.put("confidenceProfit", confidenceProfit);
        config.put("confidenceLoss", confidenceLoss);

    }

    public StateValueData getCurrentStateValueData()
    {
        if(stackStateValueData.size() > 0)
        {
            return stackStateValueData.getHeadData();
        }
        return null;
    }

    public ConcurrentHashMap<String, Object> getCorrelatorData()
    {
        return correlatorData;
    }

    public String getID()
    {
        return id;
    }

    public int getGeneration()
    {
        return generation;
    }

    public void spawn() {
        if(Math.random() > 0.5)
        {
            correlatorBuilderManager.build(this);
        }
    }

    public abstract void createMutant();

    public abstract String createState();

    @Override
    public void run()
    {
        System.out.println(id + " started...");
        while (alive)
        {
            while (processingStage != 'R')
            {
                try {
                    Thread.sleep(0,10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(toInterrupt)
                {
                    toInterrupt = false;
                    interrupt();
                }
            }
            processingStage = 'P';
            Tick tick = pool.getCurrentTick();
            age++;

            if(memory.size() > 100 && generation > 0)
            {
                if(Math.log(memory.size())/5 > stateComponentsNumber + 1)
                {
                    killLineage();
                }
            }
            else if(tick != null)
            {
                currentTick = tick;
                ticks.add(tick);
                if(ticks.size() > numTicksObserved)
                {
                    ticks.remove(0);

                    //TODO
                    currentUnderlyingComponents = new TreeMap<String, Object>();
                    currentStateValueID = createState();
                    if(stateComponentsNumber < currentUnderlyingComponents.size())
                    {
                        stateComponentsNumber = currentUnderlyingComponents.size();
                    }
                    //System.out.println(id + " " + currentStateValueID);

                    stackUnderlyingComponents.addToStack(currentUnderlyingComponents);

                    if(currentStateValueID != null)
                    {
                        if(!memory.containsKey(currentStateValueID))
                        {
                            memory.put(currentStateValueID, new StateValueData(this, currentStateValueID, resolution, passCode));
                        }

                        StateValueData currentStateValueData = memory.get(currentStateValueID);
                        try
                        {
                            currentStateValueData.newOrder(age, tick.getPairData(pair.name()).getClose(), passCode);
                        }
                        catch (IllegalStateValueDataModificationException e)
                        {
                            e.printStackTrace();
                        }
                        if(stateComponentsNumber == -1)
                        {
                            stateComponentsNumber = currentStateValueData.getStateComponentNumber();
                        }
                        stackStateValueData.addToStack(currentStateValueData);

                    }
                    else {
                        stackStateValueData.addToStack(null);
                    }

                    for(StateValueData stateValueData : aliveStates)
                    {
                        try
                        {
                            stateValueData.addObservedResult(tick.getPairData(pair.name()).getLow(), tick.getPairData(pair.name()).getHigh(), age, passCode);
                        }
                        catch (IllegalStateValueDataModificationException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    refreshAliveStates();
                }
            }
            processingStage = 'F';
            if(toInterrupt)
            {
                toInterrupt = false;
                interrupt();
            }
        }
        if(toInterrupt)
        {
            toInterrupt = false;
            interrupt();
        }
        System.out.println("Correlator " + id + " ended...");
    }

    public void addLiveState(StateValueData stateValueData)
    {
        aliveStatesToAdd.add(stateValueData);
    }
    public void removeLiveState(StateValueData stateValueData)
    {
        aliveStatesToKill.add(stateValueData);
    }

    private void refreshAliveStates()
    {
        aliveStates.addAll(aliveStatesToAdd);
        aliveStates.removeAll(aliveStatesToKill);
        aliveStatesToAdd.clear();
        aliveStatesToKill.clear();
    }

    public void setReady()
    {
        processingStage = 'R';
    }

    public void die()
    {
        if(pool.size() > 10 && Math.random()*age > 1000)
        {
            if(children.size() == 0)
            {
                pool.toKill(this);

                if(aggressiveParent != null)
                {
                    aggressiveParent.childDeath(this);
                }
                if(passiveParent != null)
                {
                    passiveParent.childDeath(this);
                }
                processingStage = 'F';
                alive = false;
                System.out.println("Correlator " + id + " dies");
            }
        }
    }

    public void forcedie()
    {
        if(children.size() == 0)
        {
            pool.toKill(this);

            if(aggressiveParent != null)
            {
                aggressiveParent.childDeath(this);
            }
            if(passiveParent != null)
            {
                passiveParent.childDeath(this);
            }
            processingStage = 'F';
            alive = false;
            System.out.println("Correlator " + id + " dies");
        }
    }

    public void killLineage()
    {
        for(GenericCorrelator child : children)
        {
            child.killLineage();
        }
        forcedie();
    }

    public void childDeath(GenericCorrelator child)
    {
        children.remove(child);
    }

    public int getNumTicksObserved() {
        return numTicksObserved;
    }

    public long getBreedingAge()
    {
        return breedingAge;
    }

    public long getAge()
    {
        return age;
    }

    public GenericCorrelator getAggressiveParent()
    {
        return aggressiveParent;
    }

    public GenericCorrelator getPassiveParent()
    {
        return passiveParent;
    }

    public Pair getPair()
    {
        return pair;
    }

    public double getResolution()
    {
        return resolution;
    }

    public char getProcessingStage()
    {
        return processingStage;
    }

    public int getStateComponentsNumber()
    {
        return stateComponentsNumber;
    }

    public double getSharpe()
    {
        double sharpe = 0;
        int i = 0;
        for(StateValueData stateValueData : memory.values())
        {
            if(stateValueData != null)
            {
                sharpe += stateValueData.getSharpe();
                i++;
            }
        }
        sharpe = sharpe/i;
        return sharpe;
    }

    public Map<String, Object> getCurrentUnderlyingComponents()
    {
        return currentUnderlyingComponents;
    }

    public Set<String> getUnderlyingComponentNames()
    {
        return currentUnderlyingComponents.keySet();
    }

    public Map<String, Object> getConfig()
    {
        return new TreeMap<String, Object>(config);
    }

    public double getPnL()
    {
        return pnl;
    }

    public TreeMap<Integer, TreeMap<Integer, Long>> getSuccssGrid()
    {
        return successGrid;
    }

    public String getHandlesInfo()
    {
        String info = "Correlator: " + id + "\n";
        info += "Children: " + children.size() + "\n";
        info += "States: " + memory.size() + "\n";
        //info += "History: " + historyStatValueData.size() + "\n";
        info += "Stack: " + stackStateValueData.size() + "\n";
        info += "Stack Underlying: " + stackUnderlyingComponents.size() + "\n";
        //info += "Open Orders: " + openOrders.size() + "\n";
        info += "Success Grid: " + successGrid.size() + "\n";
        info += "Ticks: " + ticks.size() + "\n\n";

        return info;
    }
}
