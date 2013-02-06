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

    public boolean usesParentStates = false;

    private boolean lineageKilled = false;

    private long breedingAge;

    public double pnl = 0;
    private boolean toInterrupt = false;

    private int stateComponentsNumber = -1;

    protected String currentStateValueID;
    protected TreeMap<String, Object> currentUnderlyingComponents;

    protected Pair pair = Pair.EURUSD;

    protected GenericCorrelator aggressiveParent;
    protected GenericCorrelator passiveParent;
    protected HashMap<String, Object> config = new HashMap<String, Object>();
    protected ArrayList<GenericCorrelator> children = new ArrayList<GenericCorrelator>();

    protected int numTicksObserved;
    protected double resolution;
    public double confidenceProfit = 0.5;
    public double confidenceLoss = confidenceProfit;

    protected ArrayList<Tick> ticks = new ArrayList<Tick>();
    protected Tick currentTick;

    private char processingStage = 'F';
    private boolean alive = true;

    private boolean stateLess = false;

    protected ConcurrentHashMap<String, Object> correlatorData = new ConcurrentHashMap<String, Object>();
    protected TreeMap<String, StateValueData> memory = new TreeMap<String, StateValueData>();
    protected FixedSizeStackArrayList<StateValueData> stackStateValueData = new FixedSizeStackArrayList<StateValueData>(5000);
    protected FixedSizeStackArrayList<TreeMap<String, Object>> stackUnderlyingComponents = new FixedSizeStackArrayList<TreeMap<String, Object>>(5000);
    public FixedSizeStackArrayList<Double> stackPnL = new FixedSizeStackArrayList<Double>(5000);

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

        setBreedingAge(10000);
        Random rand = new Random();
        setNumberTicksObserved((int) Math.pow(2, rand.nextInt(7) + 1));

        if(aggressiveParent != null)
        {
            this.setBreedingAge(aggressiveParent.getBreedingAge());
            this.setNumberTicksObserved(aggressiveParent.getNumTicksObserved());
        }

        if(this.config.containsKey("cloning_config:numTicksObserved"))
        {
            setNumberTicksObserved((Integer) config.get("cloning_config:numTicksObserved"));
            this.config.remove("cloning_config:numTicksObserved");
        }

        if(this.config.containsKey("cloning_config:stateLess"))
        {
            stateLess = ((Boolean) config.get("cloning_config:stateLess"));
            this.config.remove("cloning_config:stateLess");
        }

        setTrackedPair(Pair.EURUSD);

        setConfidenceLevels();

        pool.addCorrelator(this);
    }

    public synchronized void toInterrupt()
    {
        toInterrupt = true;
    }

    public synchronized void reset()
    {
        correlatorData.clear();
        stackStateValueData.clear();
        stackUnderlyingComponents.clear();
        memory.clear();
        processingStage = 'F';
        age = 0;

        killLineage();
    }

    public void addChild(GenericCorrelator child)
    {
        children.add(child);
    }

    public void setTrackedPair(Pair pair)
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
        /*if(config.containsKey("confidenceProfit"))
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
        }*/

        config.put("confidenceProfit", confidenceProfit);
        config.put("confidenceLoss", confidenceLoss);
    }

    public synchronized StateValueData getCurrentStateValueData()
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
        while (alive && !toInterrupt)
        {
            while (processingStage != 'R' && !toInterrupt)
            {
                try {
                    Thread.sleep(0,1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(toInterrupt)
            {
                break;
            }
            processingStage = 'P';
            Tick tick = pool.getCurrentTick();
            age++;

            if(memory.size() > 100)
            {
                if(Math.log(memory.size())/5 > stateComponentsNumber + 1)
                {
                    setStateLess();
                }
            }
            else if(tick != null)
            {
                currentTick = tick;
                ticks.add(tick);
                if(ticks.size() > numTicksObserved)
                {
                    if(!stateLess)
                    {
                        refreshAliveStates();
                        for(StateValueData stateValueData : aliveStates)
                        {
                            try
                            {
                                stateValueData.addObservedResult(tick.getPairData(pair.name()).getLow(), tick.getPairData(pair.name()).getHigh(), tick.getPairData(pair.name()).getClose(), age, passCode);
                            }
                            catch (IllegalStateValueDataModificationException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        refreshAliveStates();
                    }
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

                    if(!stateLess)
                    {
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
                    }
                }
            }
            processingStage = 'F';
        }
        if(toInterrupt)
        {
            toInterrupt = false;
            interrupt();
            if(alive)
            {
                reset();
            }
        }
        System.out.println("Correlator " + id + " ended...");
    }

    public synchronized void addLiveState(StateValueData stateValueData)
    {
        aliveStatesToAdd.add(stateValueData);
    }
    public synchronized void removeLiveState(StateValueData stateValueData)
    {
        aliveStatesToKill.add(stateValueData);
    }

    private synchronized void refreshAliveStates()
    {
        aliveStates.addAll(aliveStatesToAdd);
        aliveStates.removeAll(aliveStatesToKill);
        aliveStatesToAdd.clear();
        aliveStatesToKill.clear();
    }

    public synchronized void setReady()
    {
        processingStage = 'R';
    }

    public synchronized void die()
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

    public synchronized void forcedie()
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

    public synchronized void killLineage()
    {
        if(!lineageKilled)
        {
            lineageKilled = true;
            for(GenericCorrelator child : children)
            {
                child.killLineage();
            }
            forcedie();
        }
    }

    public synchronized void setStateLess()
    {
        memory.clear();
        stackStateValueData.clear();
        stackPnL.clear();
        for(GenericCorrelator child : children)
        {
            /*if(this == child.passiveParent)
            {
                child.killLineage();
            }*/
            if(child.usesParentStates)
            {
                child.killLineage();
            }
        }
        pnl = 0;
    }

    public synchronized boolean isStateLess()
    {
        return stateLess;
    }

    public synchronized int getNumberOfChildren()
    {
        return children.size();
    }

    public synchronized void childDeath(GenericCorrelator child)
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

    public synchronized Object getUnderlyingComponent(Object name)
    {
        if(currentUnderlyingComponents == null)
        {
            return null;
        }
        return currentUnderlyingComponents.get(name);
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

    public synchronized char getProcessingStage()
    {
        return processingStage;
    }

    public int getStateComponentsNumber()
    {
        return stateComponentsNumber;
    }

    public synchronized double getSharpe()
    {
        double sharpe = 0;
        for(StateValueData stateValueData : memory.values())
        {
            if(stateValueData != null)
            {
                sharpe += (stateValueData.getSharpe()/memory.size());
            }
        }
        return sharpe;
    }

    public synchronized Map<String, Object> getCurrentUnderlyingComponents()
    {
        return currentUnderlyingComponents;
    }

    public synchronized Set<String> getUnderlyingComponentNames()
    {
        return currentUnderlyingComponents.keySet();
    }

    public synchronized Map<String, Object> getConfig()
    {
        return new TreeMap<String, Object>(config);
    }

    public synchronized double getPnL()
    {
        pnl = 0;
        for(StateValueData stateValueData : memory.values())
        {
            if(stateValueData != null)
            {
                pnl += (stateValueData.getAverage()/memory.size());
            }
        }
        return pnl;
    }

    public synchronized TreeMap<Integer, TreeMap<Integer, Long>> getSuccssGrid()
    {
        return successGrid;
    }

    public synchronized String getDistSkew()
    {
        String dist = "";
        TreeMap<Integer, Double> distSkew = new TreeMap<Integer, Double>();

        for(StateValueData stateValueData : memory.values())
        {
            TreeMap<Integer, Double> neg = new TreeMap<Integer, Double>(stateValueData.getCollapsedDist().headMap(0));
            SortedMap<Integer, Double> pos = stateValueData.getCollapsedDist().tailMap(0);

            int shortProfit = 0;

            for(int returnClass : neg.descendingKeySet())
            {
                double weight = neg.get(returnClass);
                if(weight/stateValueData.sumWeights > confidenceProfit)
                {
                    shortProfit = returnClass;
                }
            }

            int longProfit = 0;
            for(int returnClass : pos.keySet())
            {
                double weight = pos.get(returnClass);
                if(weight/stateValueData.sumWeights > confidenceProfit)
                {
                    longProfit = returnClass;
                }
            }

            if(longProfit > shortProfit)
            {
                for(Map.Entry<Integer, Double> entry : stateValueData.getCollapsedDist().entrySet())
                {
                    if(!distSkew.containsKey(entry.getKey()))
                    {
                        distSkew.put(entry.getKey(), 0.0);
                    }
                    distSkew.put(entry.getKey(), distSkew.get(entry.getKey()) + entry.getValue());
                }
            }
            else {
                for(Map.Entry<Integer, Double> entry : stateValueData.getCollapsedDist().entrySet())
                {
                    if(!distSkew.containsKey(-entry.getKey()))
                    {
                        distSkew.put(-entry.getKey(), 0.0);
                    }
                    distSkew.put(-entry.getKey(), distSkew.get(-entry.getKey()) + entry.getValue());
                }
            }
        }

        for(Map.Entry<Integer, Double> entry : distSkew.entrySet())
        {
            dist += entry.getKey() + "=" + entry.getValue() + ",";
        }

        return dist;
    }

    public synchronized String getHandlesInfo()
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
