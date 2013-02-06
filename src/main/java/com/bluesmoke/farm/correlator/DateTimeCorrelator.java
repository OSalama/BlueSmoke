package com.bluesmoke.farm.correlator;

import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;
import com.bluesmoke.farm.service.feed.FeedService;

import java.util.Date;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Oblene
 * Date: 07/01/13
 * Time: 07:16
 * To change this template use File | Settings | File Templates.
 */
public class DateTimeCorrelator extends GenericCorrelator {


    public DateTimeCorrelator(String id, CorrelatorBuilderManager correlatorBuilderManager, CorrelatorPool pool, FeedService feed, GenericCorrelator aggressiveParent, GenericCorrelator passiveParent, HashMap<String, Object> config)
    {
        super("DateTime_" + id, correlatorBuilderManager, pool, feed, aggressiveParent, passiveParent, config);
    }

    @Override
    public void createMutant() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String createState() {

        Date timeStamp = currentTick.getTimeStamp();
        String dateType = config.get("DateType").toString();

        String state = null;
        if(dateType.equals("DAY"))
        {
            state += timeStamp.getDay();
            currentUnderlyingComponents.put("DateComponent", timeStamp.getDay());
        }
        else if(dateType.equals("HOUR"))
        {
            state += timeStamp.getHours();
            currentUnderlyingComponents.put("DateComponent", timeStamp.getHours());
        }
        else if(dateType.equals("MIN"))
        {
            state += timeStamp.getMinutes();
            currentUnderlyingComponents.put("DateComponent", timeStamp.getMinutes());
        }
        else if(dateType.equals("MONTH"))
        {
            state += timeStamp.getMonth();
            currentUnderlyingComponents.put("DateComponent", timeStamp.getMonth());
        }
        else if(dateType.equals("DATE"))
        {
            state += timeStamp.getDate();
            currentUnderlyingComponents.put("DateComponent", timeStamp.getDate());
        }

        return state;
    }
}
