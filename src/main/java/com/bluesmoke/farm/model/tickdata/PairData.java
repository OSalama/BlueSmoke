package com.bluesmoke.farm.model.tickdata;

import com.bluesmoke.farm.enumeration.Pair;

public class PairData {
    private Pair pair;
    private double bid;
    private double ask;
    private double mid;
    private double askVol;
    private double bidVol;

    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;


    public PairData(Pair pair, double ask, double bid, double askVol, double bidVol)
    {
        this.pair = pair;
        this.bid = bid;
        this.ask = ask;
        this.mid = (bid + ask)/2;

        this.askVol = askVol;
        this.bidVol = bidVol;
    }

    public PairData(Pair pair, double open, double high, double low, double close, double vol)
    {
        this.pair = pair;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;

        this.volume = vol;
    }

    public String getPair() {
        return pair.name();
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public double getMid() {
        return mid;
    }

    public double getAskVol() {
        return askVol;
    }

    public double getBidVol() {
        return bidVol;
    }

    public double getVolume() {
        return volume;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public String toString()
    {
        return      "{pair=" + pair + ", "
                +   "ask=" + ask + ", "
                +   "bid=" + bid + ", "
                +   "open=" + open + ", "
                +   "high=" + high + ", "
                +   "low=" + low + ", "
                +   "close=" + close + ", "
                +   "askVol=" + askVol + ", "
                +   "bidVol=" + bidVol + "}";
    }
}
