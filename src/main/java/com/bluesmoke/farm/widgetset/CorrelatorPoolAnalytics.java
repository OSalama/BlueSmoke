package com.bluesmoke.farm.widgetset;

import com.bluesmoke.farm.Launcher;
import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.model.correlatordata.StateValueData;
import com.bluesmoke.farm.widgetset.client.ui.VCorrelatorPoolAnalytics;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.ClientWidget;
import org.vaadin.contrib.component.svg.processing.Processing;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

@ClientWidget(VCorrelatorPoolAnalytics.class)
public class CorrelatorPoolAnalytics extends Processing {

    private String message = "";
    private int height;
    private int width;

    private int initialMessages = 5;

    private Timer timer = new Timer();
    private CorrelatorPool pool;
    private Launcher app;

    private Queue<String[]> messages = new ArrayBlockingQueue<String[]>(10000);

    public CorrelatorPoolAnalytics(final Launcher app, final CorrelatorPool pool)
    {
        this.app = app;
        this.pool = pool;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try
                {
                    if(initialMessages > 0)
                    {
                        resize();
                        initialMessages--;
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }, 2000);
    }

    public void terminate()
    {
        timer.cancel();
    }


    @Override
    public void paintContent(PaintTarget target) throws PaintException
    {
        super.paintContent(target);
        target.addAttribute("message", message);
        this.message = "";
    }

    @Override
    public void changeVariables(Object source, Map<String, Object> variables)
    {
        super.changeVariables(source, variables);

        if(variables.containsKey("message"))
        {
            processMessage(variables.get("message").toString());
        }
    }

    private void processMessage(String message)
    {
        System.out.println(message);

        if(message.equals("REQUEST_RESIZE"))
        {
            resize();
        }
        else if(message.equals("REQUEST_CHECK"))
        {
            checkPool();
        }
        else if(message.startsWith("CORRELATOR"))
        {
            pool.pauseFeed();
            String id = message.split(":")[1];
            String dist = "";
            if(pool.getCorrelatorForID(id) != null)
            {
                GenericCorrelator correlator = pool.getCorrelatorForID(id);
                dist = correlator.getDistSkew();
                GenericCorrelator aggressiveParent = correlator.getAggressiveParent();
                GenericCorrelator passiveParent = correlator.getPassiveParent();
                if(aggressiveParent == null || !pool.contains(aggressiveParent) || passiveParent == null || !pool.contains(passiveParent))
                {
                    correlator.killLineage();
                }
            }
            pool.resumeFeed();
            sendMessage("DIST_SKEW", id + ";" + dist);
        }
    }

    private void checkPool()
    {
        //pool.pauseFeed();
        System.out.println("Population: " + pool.size());
        try
        {
            List<GenericCorrelator> correlators = new ArrayList<GenericCorrelator>(pool);
            for(GenericCorrelator correlator : correlators)
            {
                String message = "";

                message += "ID=" + correlator.getID() + ";";
                        //"AGE=" + correlator.getAge() + ";" +
                message += "GEN=" + correlator.getGeneration() + ";";

                double  sharpe = correlator.getSharpe();
                if(("" + sharpe).equals("NaN"))
                {
                    sharpe = 0;
                }

                if(sharpe > 10)
                {
                    sharpe = 10;
                }

                message += "SHARPE=" + sharpe + ";";

                message += "PNL=" + correlator.getPnL() + ";";

                if(correlator.getAggressiveParent() != null)
                {
                    message += "AP=" + correlator.getAggressiveParent().getID() + ";";
                }
                else {
                    message += "AP=XXX;";
                }
                if(correlator.getPassiveParent() != null)
                {
                    message += "PP=" + correlator.getPassiveParent().getID();
                }
                else {
                    message += "PP=XXX";
                }
                //message += "CONF=" + correlator.getConfig() + ";" + "";
                        //"PNL=" + correlator.getPnL() + ";" +
                        //"SHARPE=" + correlator.getSharpe() + ";" +
                        //"PAIR=" + correlator.getPair() + ";" +
                        //"RES=" + correlator.getResolution() + ";";
                /*StateValueData stateValueData = correlator.getCurrentStateValueData();

                if(stateValueData != null)
                {
                    message += "AV=" + stateValueData.getAverage() + ";" +
                        "SD=" + stateValueData.getSDev() + ";" +
                        "C=" + stateValueData.getCount() + ";" +
                        "CSHARPE=" + stateValueData.getSharpe() + ";";

                    String dist = stateValueData.getDist().toString().replaceAll("\\}\\}|[ ]", "");
                    dist = dist.replaceAll("=\\{", ">");
                    dist = dist.replaceAll("\\},", "<");
                    dist = dist.replaceAll("=", "@");
                    dist = dist.substring(1);

                    message += "DIST=" + dist;
                }*/
                scheduleMessage("CORRELATOR_DATA" , message);
            }
            scheduleMessage("BATCH" , "END");
            scheduleMessage("TICK" , pool.getCurrentTick().getTimeStamp().toString());
            sendScheduledMessages();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        //pool.resumeFeed();
    }

    private void sendMessage(String property, String value)
    {
        this.message = property + "->" + value;
        requestRepaint();
        app.getPusher().push();
    }

    private void sendMessage(String message)
    {
        //System.out.println(message);
        this.message = message;
        requestRepaint();
        app.getPusher().push();
    }

    public void resize()
    {
        app.getMainWindow().setHeightUnits(UNITS_PIXELS);
        app.getMainWindow().setWidthUnits(UNITS_PIXELS);

        this.height = (int)app.getMainWindow().getHeight() - 50;
        this.width = (int)app.getMainWindow().getWidth() - 3;

        setWidth(width + "px");
        setHeight(height + "px");

        scheduleMessage("RESIZE", width + "," + height);
        app.getPusher().push();
    }

    private void scheduleMessage(String property, String message)
    {
        messages.add(new String[]{property, message});
    }

    private void sendScheduledMessages()
    {
        String message = "";
        while(!messages.isEmpty())
        {
            String[] messageParts = messages.poll();
            message += messageParts[0] + "->" + messageParts[1] + "##";
        }
        if(message.length() > 0)
        {
            sendMessage(message);
        }
    }
}
