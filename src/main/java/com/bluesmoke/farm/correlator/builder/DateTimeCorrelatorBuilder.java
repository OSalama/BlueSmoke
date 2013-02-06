package com.bluesmoke.farm.correlator.builder;

import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.DateTimeCorrelator;
import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.service.feed.FeedService;

import java.util.HashMap;
import java.util.Random;

public class DateTimeCorrelatorBuilder implements CorrelatorBuilder
{
    private CorrelatorPool pool;
    private FeedService feed;
    private CorrelatorBuilderManager correlatorBuilderManager;

    public DateTimeCorrelatorBuilder(CorrelatorPool pool, FeedService feed, CorrelatorBuilderManager correlatorBuilderManager)
    {
        this.pool = pool;
        this.feed = feed;
        this.correlatorBuilderManager = correlatorBuilderManager;
    }

    public void build(GenericCorrelator aggressiveParent, GenericCorrelator passiveParent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void build(GenericCorrelator parent) {

        HashMap<String, Object> config = new HashMap<String, Object>();

        Random rand = new Random();
        String dateType = "";

        switch (rand.nextInt(5))
        {
            case 0:
                dateType = "DAY";
                break;
            case 1:
                dateType = "DATE";
                break;
            case 2:
                dateType = "MONTH";
                break;
            case 3:
                dateType = "HOUR";
                break;
            case 4:
                dateType = "MIN";
                break;
        }
        config.put("DateType" , dateType);

        int count = 0;
        for(GenericCorrelator correlator : pool)
        {
            if(correlator.getID().startsWith("DateTime_"))
            {
                count++;
            }
        }
        if(count < 5)
        {
            new DateTimeCorrelator(pool.getNextID(), correlatorBuilderManager, pool, feed, null, null, config);
        }
    }
}
