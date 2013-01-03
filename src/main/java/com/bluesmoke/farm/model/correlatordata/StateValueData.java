package com.bluesmoke.farm.model.correlatordata;

import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.exception.IllegalStateValueDataModificationException;
import com.bluesmoke.farm.model.pnl.OpenOrder;

import java.util.*;

public class StateValueData {

    //TODO ordered list of state component type

    final private String state;
    final private double res;
    final private String passCode;
    final private GenericCorrelator correlator;

    final private List<String> stateComponents;
    final private int stateComponentNumber;

    private double sum = 0;
    private double sum2 = 0;
    private long count = 0;

    private double average = 0;
    private double sdev = 0;
    private double sharpe = 0;

    private double pnl = 0;
    private double sum2PnL = 0;
    private long countClosed = 0;

    private double sumWeights = 0;

    private TreeMap<Long, OpenOrder> openOrders = new TreeMap<Long, OpenOrder>();
    private TreeMap<Integer, Double> collapsedDist = new TreeMap<Integer, Double>();
    private TreeMap<Integer, TreeMap<Integer, Double>> dist = new TreeMap<Integer, TreeMap<Integer, Double>>();

    public StateValueData(GenericCorrelator correlator, String state, double res, String passCode)
    {
        this.correlator = correlator;
        this.state = state;
        stateComponents = Arrays.asList(state.split(","));
        stateComponentNumber = stateComponents.size();
        this.res = res;
        this.passCode = passCode;
    }

    public void addObservedResult(double result, int timeElapsed, String passCode) throws IllegalStateValueDataModificationException {
        if(!this.passCode.equals(passCode))
        {
            throw new IllegalStateValueDataModificationException();
        }
        sum += result;
        sum2 += (result*result);

        count ++;

        average = sum/count;
        double variance = Math.abs(sum2/count - average*average);
        sdev = Math.sqrt(variance);
        sharpe = average*average / variance;

        Integer timeClass = (int)(Math.log(timeElapsed)/ Math.log(2));
        Integer distClass = (int)(result/res);
        if(!dist.containsKey(timeClass))
        {
            dist.put(timeClass, new TreeMap<Integer, Double>());
        }
        if(!dist.get(timeClass).containsKey(distClass))
        {
            dist.get(timeClass).put(distClass, 0.0);
        }

        dist.get(timeClass).put(distClass, dist.get(timeClass).get(distClass) + 1);

        if(!collapsedDist.containsKey(distClass))
        {
            collapsedDist.put(distClass, 0.0);
        }

        collapsedDist.put(distClass, collapsedDist.get(distClass) + 1);
    }

    public void addObservedResult(double min, double max, long currentTime, String passCode) throws IllegalStateValueDataModificationException {
        if(!this.passCode.equals(passCode))
        {
            throw new IllegalStateValueDataModificationException();
        }

        int priority = 0;
        Set<Long> closedOrders = new HashSet<Long>();
        for(Map.Entry<Long, OpenOrder> entry : openOrders.entrySet())
        {
            long orderNumber = entry.getKey();
            if(orderNumber != currentTime)
            {
                OpenOrder order = entry.getValue();
                double weight = Math.exp(-priority);
                Integer timeClass = (int)(Math.log(currentTime - orderNumber)/ Math.log(2));
                int maxClass = (int)((max - order.getPrice())/res);
                int minClass = (int)((min - order.getPrice())/res);
                if(!dist.containsKey(timeClass))
                {
                    dist.put(timeClass, new TreeMap<Integer, Double>());
                }
                for(int i = minClass; i <= maxClass; i++)
                {
                    if(!dist.get(timeClass).containsKey(i))
                    {
                        dist.get(timeClass).put(i, 0.0);
                    }

                    if(!collapsedDist.containsKey(i))
                    {
                        collapsedDist.put(i, 0.0);
                    }
                }
                for(int i = minClass; i <= maxClass; i++)
                {
                    dist.get(timeClass).put(i, dist.get(timeClass).get(i) + weight);
                    collapsedDist.put(i, collapsedDist.get(i) + weight);
                }
                sumWeights += weight;
                boolean closed = false;
                if(order.getPosition() == 'L')
                {
                    closed = order.newPrice(min);
                    if(!closed)
                    {
                        closed = order.newPrice(max);
                    }
                }
                else {
                    closed = order.newPrice(max);
                    if(!closed)
                    {
                        closed = order.newPrice(min);
                    }
                }

                if(closed)
                {
                    closedOrders.add(orderNumber);
                    double profit = weight * (order.getPnL()/res);
                    //System.out.println("Order Closed from " + state + ": " + orderNumber + ": Profit: " + profit);

                    pnl += profit;
                    sum = pnl;
                    sum2PnL += (profit*profit);
                    sum2 = sum2PnL;
                    correlator.pnl -= average;
                    countClosed += weight;

                    average = pnl/countClosed;
                    correlator.pnl += average;

                    double variance = Math.abs(sum2/countClosed - average);

                    sdev = Math.sqrt(variance);
                    double average2PnL = average*average;
                    sharpe = Math.sqrt(average2PnL / variance);
                }

                priority++;
            }
        }
        for(long orderNumber : closedOrders)
        {
            openOrders.remove(orderNumber);
        }
        if(openOrders.isEmpty())
        {
            correlator.removeLiveState(this);
        }
    }

    public void newOrder(long orderNumber, double price, String passCode) throws IllegalStateValueDataModificationException
    {
        if(!this.passCode.equals(passCode))
        {
            throw new IllegalStateValueDataModificationException();
        }
        correlator.addLiveState(this);

        TreeMap<Integer, Double> neg = new TreeMap<Integer, Double>(collapsedDist.headMap(0));
        SortedMap<Integer, Double> pos = collapsedDist.tailMap(0);

        int shortProfit = 0;
        int shortLoss = 0;

        //System.out.println("Distribution: ");
        for(int returnClass : neg.descendingKeySet())
        {
            double weight = neg.get(returnClass);
            if(weight/sumWeights > correlator.confidenceProfit)
            {
                shortProfit = returnClass;
            }
            if(weight/sumWeights > correlator.confidenceLoss)
            {
                shortLoss = returnClass;
            }
            //System.out.println(returnClass + " : " + weight);
        }

        int longProfit = 0;
        int longLoss = 0;
        for(int returnClass : pos.keySet())
        {
            double weight = pos.get(returnClass);
            if(weight/sumWeights > correlator.confidenceProfit)
            {
                longProfit = returnClass;
            }
            if(weight/sumWeights > correlator.confidenceLoss)
            {
                longLoss = returnClass;
            }
            //System.out.println(returnClass + " : " + weight);
        }

        double takeProfit = price + (longProfit*res);
        double stopLoss = price - (shortLoss*res);
        if(Math.abs(shortProfit) > longProfit)
        {
            takeProfit = price - (shortProfit*res);
            stopLoss = price + (longLoss*res);
        }
        //System.out.println("New Order from " + state + ": " + orderNumber + ": " + price + " " + takeProfit + " " + stopLoss);
        openOrders.put(orderNumber, new OpenOrder("" + orderNumber, price, takeProfit, stopLoss));
    }

    public String getState() {
        return state;
    }

    public double getRes() {
        return res;
    }

    public double getCount() {
        return count;
    }

    public double getAverage() {
        return average;
    }

    public double getSDev() {
        return sdev;
    }

    public double getSharpe() {
        return sharpe;
    }

    public double getPnl()
    {
        return pnl;
    }

    public String getStateComponent(int index)
    {
        return stateComponents.get(index);
    }

    public int getStateComponentNumber()
    {
        return stateComponentNumber;
    }

    public TreeMap<Integer, TreeMap<Integer, Double>> getDist() {
        return dist;
    }
}
