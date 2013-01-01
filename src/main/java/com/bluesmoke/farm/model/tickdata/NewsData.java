package com.bluesmoke.farm.model.tickdata;


public class NewsData
{
    public final String title;
    public final String news;
    public final String country;
    public final int volatility;
    public final double previous;
    public final double consensus;
    public final double actual;

    public NewsData(String title, String country, int volatility, double previous, double consensus, double actual, String news)
    {
        this.title = title;
        this.news = news;
        this.volatility = volatility;
        this.country = country;
        this.previous = previous;
        this.consensus = consensus;
        this.actual = actual;
    }
}
