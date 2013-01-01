package com.bluesmoke.farm.widgetset.client.ui;

import com.bluesmoke.farm.model.correlatordata.StateValueData;
import com.google.gwt.event.dom.client.*;
import org.vaadin.gwtgraphics.client.Line;
import org.vaadin.gwtgraphics.client.shape.Circle;
import org.vaadin.gwtgraphics.client.shape.Text;

import java.util.HashMap;
import java.util.TreeMap;

public class VCorrelator implements Refreshable{

    private Circle body;
    private VCorrelatorPoolAnalytics canvas;
    //private VHintPopUp popUp;

    public String id;

    public TreeMap<String, Object> information = new TreeMap<String, Object>();
    private TreeMap<String, Text> labels = new TreeMap<String, Text>();

    private TreeMap<Integer, TreeMap<Integer, Long>> distMap = new TreeMap<Integer, TreeMap<Integer, Long>>();

    private VCorrelator aParent;
    private VCorrelator pParent;

    private Line aLine;
    private Line pLine;

    private HashMap<String, VCorrelator> correlators;

    private int destX = 10;
    private int destY = 10;

    private boolean mouseOver = false;

    public VCorrelator(final VCorrelatorPoolAnalytics canvas, HashMap<String, VCorrelator> correlators)
    {
        this.canvas = canvas;
        canvas.addRefreshable(this);
        this.correlators = correlators;
        body = canvas.circle(10, 10, 3);
        body.setFillColor("orange");
        body.setFillOpacity(0.8);
        body.setStrokeOpacity(0);

        /*body.addMouseOverHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                mouseOver = true;
                body.setStrokeOpacity(0.5);
            }
        });

        body.addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                mouseOver = false;
                body.setStrokeOpacity(0);
            }
        });*/

        body.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                canvas.sendMessage("CORRELATOR:" + id);
            }
        });

        //popUp = new VHintPopUp(canvas, 10, 10);

        setDestX((int) canvas.random(canvas.width));
        setDestY((int) canvas.random(canvas.height));
    }

    public void setData(String data)
    {
        for(String info : data.split(";"))
        {
            String property = info.split("=")[0];
            String value = info.split("=")[1];

            if(property.equals("ID"))
            {
                id = value;

                addToPopUp(property, value);
            }
            else if(property.equals("AGE"))
            {
                information.put(property, Long.parseLong(value));
                addToPopUp(property, value);
            }
            else if(property.equals("GEN"))
            {
                information.put(property, Integer.parseInt(value));
                addToPopUp(property, value);
            }
            else if(property.equals("AP"))
            {
                aParent = correlators.get(value);
                addToPopUp(property, value);
            }
            else if(property.equals("PP"))
            {
                pParent = correlators.get(value);
                addToPopUp(property, value);
            }
            else if(property.equals("CONF"))
            {
                information.put(property, value);
                addToPopUp(property, value);
            }
            else if(property.equals("PNL"))
            {
                information.put(property, Double.parseDouble(value));
                addToPopUp(property, value);
            }
            else if(property.equals("SHARPE"))
            {
                double sharpe = 0;
                if(!value.equals("Infinity"))
                {
                    sharpe = Double.parseDouble(value);
                }
                information.put(property, sharpe);
                addToPopUp(property, value);
            }
            else if(property.equals("PAIR"))
            {
                //information.put(property, value);
                addToPopUp(property, value);
            }
            else if(property.equals("RES"))
            {
                //information.put(property, Double.parseDouble(value));
                addToPopUp(property, value);
            }
            else if(property.equals("AV"))
            {
                //information.put(property, Double.parseDouble(value));
                addToPopUp(property, value);
            }
            else if(property.equals("SD"))
            {
                //information.put(property, Double.parseDouble(value));
                addToPopUp(property, value);
            }
            else if(property.equals("C"))
            {
                //information.put(property, Long.parseLong(value));
                addToPopUp(property, value);
            }
            else if(property.equals("CSHARPE"))
            {
                //information.put(property, Double.parseDouble(value));
                addToPopUp(property, value);
            }
            else if(property.equals("DIST"))
            {
                String[] timeClasses = value.split("<");
                for(String timeClass : timeClasses)
                {
                    int time = Integer.parseInt(timeClass.split(">")[0]);
                    String[] distClasses = timeClass.split(">")[1].split(",");
                    for(String distClass : distClasses)
                    {
                        int dist = Integer.parseInt(distClass.split("@")[0]);
                        long freq = Long.parseLong(distClass.split("@")[1]);

                        if(!distMap.containsKey(time))
                        {
                            distMap.put(time, new TreeMap<Integer, Long>());
                        }

                        distMap.get(time).put(dist, freq);
                    }
                }
            }
        }
        live();
    }

    public void addToPopUp(String property, String value)
    {
        /*if(!labels.containsKey(property))
        {
            Text tProperty = canvas.text(property, 10, 10);
            tProperty.setStrokeOpacity(0);
            tProperty.setFillColor("white");
            int y = popUp.addComponent(tProperty, 10, "Text");

            Text tValue = canvas.text(value, 10, 10);
            tValue.setStrokeOpacity(0);
            tValue.setFillColor("white");
            popUp.addComponent(tValue, 150, y, "Text");
            popUp.structure();

            labels.put(property, tValue);
        }
        else {
            labels.get(property).setText(value);
            popUp.structure();
        }*/
    }

    public void die()
    {
        body.setFillColor("gray");
        body.setFillOpacity(0.1);
        if(aLine != null)
        {
            aLine.setStrokeOpacity(0.05);
        }

        if(pLine != null)
        {
            pLine.setStrokeOpacity(0.05);
        }
    }

    public void live()
    {
        body.setFillColor("orange");
        body.setFillOpacity(0.8);
        if(aLine != null)
        {
            aLine.setStrokeOpacity(0.25);
        }

        if(pLine != null)
        {
            pLine.setStrokeOpacity(0.25);
        }
    }

    public void refresh() {
        /*if(mouseOver)
        {
            if(!popUp.isVisible())
            {
                popUp.show();
            }
            else {
                popUp.maintainVisibility();
            }

            popUp.move(body.getX() + 5, body.getY());
        }
        else
        {
            popUp.hide();
        }*/

        if(Math.abs(body.getX() - destX) > 8)
        {
            body.setX(body.getX() + (destX - body.getX())/8);
        }

        if(Math.abs(body.getY() - destY) > 8)
        {
            body.setY(body.getY() + (destY - body.getY())/8);
        }

        if(aParent != null)
        {
            if(aLine == null)
            {
                aLine = canvas.line(10, 10, 20, 20);
            }
            aLine.setX1(aParent.body.getX());
            aLine.setX2(body.getX());
            aLine.setY1(aParent.body.getY());
            aLine.setY2(body.getY());
            aLine.setStrokeColor("red");
            aLine.setStrokeOpacity(0.25);
        }

        if(pParent != null)
        {
            if(pLine == null)
            {
                pLine = canvas.line(10, 10, 20, 20);
            }
            pLine.setX1(pParent.body.getX());
            pLine.setX2(body.getX());
            pLine.setY1(pParent.body.getY());
            pLine.setY2(body.getY());
            pLine.setStrokeColor("green");
            pLine.setStrokeOpacity(0.25);
        }
    }

    public void setDestX(int x)
    {
        destX = x;
    }

    public void setDestY(int y)
    {
        destY = y;
    }

    public void setR(int r)
    {
        body.setRadius(r);
    }
}
