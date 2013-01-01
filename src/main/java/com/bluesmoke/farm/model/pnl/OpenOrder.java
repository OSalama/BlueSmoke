package com.bluesmoke.farm.model.pnl;

public class OpenOrder {

    private final String stateIDOnOpen;
    private final double open;
    private final double takeProfit;
    private final double stopLoss;

    private double current;

    private boolean closed = false;
    private double pnl = 0;

    private final char position;

    private double[] successData;


    public OpenOrder(String stateIDOnOpen, double open, double takeProfit, double stopLoss)
    {
        this.open = open;
        this.takeProfit = takeProfit;
        this.stopLoss = stopLoss;
        this.stateIDOnOpen = stateIDOnOpen;
        this.current = open;

        if(takeProfit > open)
        {
            position = 'L';
        }
        else {
            position = 'S';
        }

    }

    public boolean newPrice(double price)
    {
        current = price;

        if(position == 'L')
        {
            if(current > takeProfit)
            {
                closed = true;
                pnl = current - open;
            }
            else if(current < stopLoss)
            {
                closed = true;
                pnl = current - open;
            }
        }
        else {
            if(current < takeProfit)
            {
                closed = true;
                pnl = open - current;
            }
            else if(current > stopLoss)
            {
                closed = true;
                pnl = open - current;
            }
        }

        successData = new double[]{takeProfit, open - current};
        return closed;
    }

    public double getPnL()
    {
        return pnl;
    }

    public double getPrice()
    {
        return open;
    }

    public double[] getSuccessData()
    {
        return successData;
    }

    public char getPosition()
    {
        return position;
    }

}
