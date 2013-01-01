package com.bluesmoke.farm.enumeration;

import java.util.TreeMap;

public class PairResolution {

    public static Double getResolution(Pair pair)
    {
        TreeMap<Pair, Double> pairs = new TreeMap<Pair, Double>();

        pairs.put(Pair.EURUSD, 0.0001);
        pairs.put(Pair.GBPUSD, 0.0001);
        pairs.put(Pair.USDCHF, 0.0001);
        pairs.put(Pair.NZDUSD, 0.0001);
        pairs.put(Pair.AUDUSD, 0.0001);
        //pairs.put(Pair.USDJPY, 0.01);

        return pairs.get(pair);
    }
}
