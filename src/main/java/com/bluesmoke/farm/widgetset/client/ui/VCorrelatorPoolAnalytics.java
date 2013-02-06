package com.bluesmoke.farm.widgetset.client.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.UIDL;
import org.vaadin.contrib.processing.svg.gwt.client.ui.VProcessingSVG;
import org.vaadin.gwtgraphics.client.shape.Text;

import java.util.ArrayList;

public class VCorrelatorPoolAnalytics extends VProcessingSVG{

    public VCorrelatorPoolAnalytics canvas;

    private VCorrelatorPlacer placer;

    //private VSuccessGrid grid;
    private VDistSkew distSkew;

    private ApplicationConnection client;
    private String uidlId;
    private boolean setupComplete = false;

    private String log = "";

    public int frameRate = 30;

    private Text date;

    private ArrayList<Refreshable> refreshables = new ArrayList<Refreshable>();

    public VCorrelatorPoolAnalytics()
    {
        size(400,400);
        canvas = this;
        placer = new VCorrelatorPlacer(this);
    }

    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client)
    {
        if(setupComplete)
        {
            if(client.updateComponent(this, uidl, true))
            {
                return;
            }

            String msg = uidl.getStringAttribute("message");
            for(String message : msg.split("##"))
            {
                if(message != null)
                {
                    processMessage(message);
                }
            }

        }
        else {
            this.client = client;
            this.uidlId = uidl.getId();
            initCanvas();
        }
    }

    public String getCorrelatorInfo(String id, String info)
    {
        return placer.getCorrelatorInfo(id, info);
    }

    public void sendMessage(String message)
    {
        client.updateVariable(uidlId, "message", message, true);
    }

    public void setup()
    {
        doClear = false;
        frameRate(frameRate);
        setupComplete = true;
        distSkew = new VDistSkew(this);
        buildUI();
    }

    private void processMessage(String message)
    {
        logText(message);
        if(message.startsWith("RESIZE->"))
        {
            int w = Integer.parseInt(message.split("->")[1].split(",")[0]);
            int h = Integer.parseInt(message.split("->")[1].split(",")[1]);
            resize(w, h);
            return;
        }

        if(message.startsWith("CORRELATOR_DATA->"))
        {
            placer.addCorrelatorData(message.split("->")[1]);
            return;
        }

        if(message.startsWith("BATCH->"))
        {
            placer.batch();
            return;
        }

        if(message.startsWith("TICK->"))
        {
            date.setText("Date: " + message.split("->")[1]);
            return;
        }

        if(message.startsWith("DIST_SKEW->"))
        {
            distSkew.show(message.split("->")[1]);
            return;
        }
    }

    public void handleClick(String id)
    {
        if(id.equals("log"))
        {
            sendMessage(log);
            log = "";
        }
    }

    private void buildUI()
    {
        Text title = text("Refresh Analytic", 20, 20);
        title.setStrokeOpacity(0);
        title.setFillColor("white");
        title.getElement().getStyle().setCursor(Style.Cursor.POINTER);
        title.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                sendMessage("REQUEST_CHECK");
            }
        });

        Text getLog = text("Get Log", 20, 40);
        getLog.setStrokeOpacity(0);
        getLog.setFillColor("white");
        getLog.getElement().getStyle().setCursor(Style.Cursor.POINTER);
        getLog.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                sendMessage(log);
            }
        });

        date = text("", 20, 60);
        date.setStrokeOpacity(0);
        date.setFillColor("white");
    }

    public void addRefreshable(Refreshable refreshable)
    {
        refreshables.add(refreshable);
    }

    public void draw()
    {
        for(Refreshable refreshable : refreshables)
        {
            refreshable.refresh();
        }
    }

    public void resize(int w, int h)
    {
        width = w;
        height = h;

        size(width, height);
    }

    public void logText(String message)
    {
        log += message + "\n";
    }
}
