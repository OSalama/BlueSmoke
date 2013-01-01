package com.bluesmoke.farm.service.feed;

import com.bluesmoke.farm.enumeration.Pair;
import com.bluesmoke.farm.exception.InvalidTimeStampException;
import com.bluesmoke.farm.exception.TickEditingLockedException;
import com.bluesmoke.farm.model.tickdata.NewsData;
import com.bluesmoke.farm.model.tickdata.PairData;
import com.bluesmoke.farm.model.tickdata.Tick;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OHLCFeed extends FeedService {

    private Map<Pair, String> feedPaths = new TreeMap<Pair, String>();
    private String calendarFeedPath;

    private Map<Pair, BufferedReader> feeds = new TreeMap<Pair, BufferedReader>();
    private BufferedReader calendarFeed;

    private SimpleDateFormat fPair = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
    private SimpleDateFormat fCalendar = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
    private NewsData currentNewsData;
    private long currentNewsTimeStamp = 0;

    private String feedsPath;

    private long totalSize = 0;
    private long readSize = 0;

    public void setFeedsPath(String feedsPath)
    {
        this.feedsPath = feedsPath;
    }

    public void addPairFeed(Pair pair, String feedFileName)
    {
        feedPaths.put(pair, feedFileName);
        try
        {
            FileInputStream fstrm = new FileInputStream(feedsPath + "/" + feedFileName);
            DataInputStream instrm = new DataInputStream(fstrm);
            BufferedReader brstrm = new BufferedReader(new InputStreamReader(instrm));
            brstrm.readLine();

            System.out.println("Found feed: " + pair.name());
            feeds.put(pair, brstrm);
            totalSize += fstrm.available();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void addCalendarFeed(String feedFileName)
    {
        calendarFeedPath = feedFileName;
        try
        {
            FileInputStream fstrm = new FileInputStream(feedsPath + "/" + feedFileName);
            DataInputStream instrm = new DataInputStream(fstrm);
            BufferedReader brstrm = new BufferedReader(new InputStreamReader(instrm));
            brstrm.readLine();

            System.out.println("Found calendar feed: " + feedFileName);
            calendarFeed = brstrm;
            totalSize += fstrm.available();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public Tick getNextTick() throws InvalidTimeStampException {

        currentTick = null;
        long timeStamp = 0;
        try
        {
            for(Pair pair : feeds.keySet())
            {
                String line = feeds.get(pair).readLine();
                long time = fPair.parse(line.split(",")[0]).getTime();
                if(timeStamp == 0)
                {
                    timeStamp = time;
                    currentTick = new Tick(timeStamp);
                }
                else {
                    if(time != timeStamp)
                    {
                        throw new InvalidTimeStampException();
                    }
                }
                readSize += line.length() + 1;
                currentTick.addPairData(createData(pair, line));
            }

            while(currentNewsTimeStamp < timeStamp)
            {
                if(currentNewsData != null && currentNewsTimeStamp < timeStamp)
                {
                    currentTick.addNewsData(currentNewsData);
                }
                String line = calendarFeed.readLine();
                currentNewsTimeStamp = fCalendar.parse(line.split(",")[0]).getTime();
                currentNewsData = createNews(line);
            }
            currentTick.lock();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        catch (TickEditingLockedException e)
        {
            e.printStackTrace();
        }
        //System.out.println(currentTick.getTimeStamp());
        return currentTick;
    }

    public void reset()
    {
        feeds = new TreeMap<Pair, BufferedReader>();

        hasNext = true;
        totalSize = 0;
        readSize = 0;

        for(Pair pair : feedPaths.keySet())
        {
            addPairFeed(pair, feedPaths.get(pair));
        }
    }

    private PairData createData(Pair pair, String line) throws ParseException
    {
        String[] parts = line.split(",");
        Date t = fPair.parse(parts[0]);

        return new PairData(pair, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
    }

    private NewsData createNews(String line)
    {
        String[] parts = line.split(",");
        if(!parts[3].matches("[0-9]*"))
        {
            return null;
        }
        if(parts.length == 7)
        {
            double actual = 0;
            double previous = 0;
            double consensus = 0;
            if(parts.length > 4)
            {
                Pattern pattern = Pattern.compile("[-]*[0-9]*\\.[0-9]*");
                Matcher matcher = pattern.matcher(parts[4]);
                if(matcher.find())
                {
                    String str = matcher.group();
                    if(str.length() > 0)
                    {
                        actual = Double.parseDouble(str);
                    }
                }
                else {
                    Pattern patternInt = Pattern.compile("[-]*[0-9]*");
                    Matcher matcherInt = patternInt.matcher(parts[4]);
                    if(matcherInt.find())
                    {
                        String str = matcherInt.group();
                        if(str.length() > 0)
                        {
                            actual = Double.parseDouble(str);
                        }
                    }
                }
            }
            if(parts.length > 5)
            {
                Pattern pattern = Pattern.compile("[-]*[0-9]*\\.[0-9]*");
                Matcher matcher = pattern.matcher(parts[5]);
                if(matcher.find())
                {
                    String str = matcher.group();
                    if(str.length() > 0)
                    {
                        previous = Double.parseDouble(str);
                    }
                }
                else {
                    Pattern patternInt = Pattern.compile("[-]*[0-9]*");
                    Matcher matcherInt = patternInt.matcher(parts[5]);
                    if(matcherInt.find())
                    {
                        String str = matcherInt.group();
                        if(str.length() > 0)
                        {
                            previous = Double.parseDouble(str);
                        }
                    }
                }
            }

            if(parts.length > 6)
            {
                Pattern pattern = Pattern.compile("[-]*[0-9]*\\.[0-9]*");
                Matcher matcher = pattern.matcher(parts[6]);
                if(matcher.find())
                {
                    String str = matcher.group();
                    if(str.length() > 0)
                    {
                        consensus = Double.parseDouble(str);
                    }
                }
                else {
                    Pattern patternInt = Pattern.compile("[-]*[0-9]*");
                    Matcher matcherInt = patternInt.matcher(parts[6]);
                    if(matcherInt.find())
                    {
                        String str = matcherInt.group();
                        if(str.length() > 0)
                        {
                            consensus = Double.parseDouble(str);
                        }
                    }
                }
            }

            //System.out.println(actual + " " + previous + " " + consensus);

            return new NewsData(parts[1], parts[2], Integer.parseInt(parts[3]), previous, consensus, actual, "");
        }
        return null;
    }


    @Override
    public double getPercentageComplete() {
        return (double)((int)((readSize * 10000)/totalSize))/100.0;
    }
}
