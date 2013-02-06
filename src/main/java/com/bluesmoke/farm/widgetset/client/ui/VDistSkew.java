package com.bluesmoke.farm.widgetset.client.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.vaadin.gwtgraphics.client.shape.Rectangle;
import org.vaadin.gwtgraphics.client.shape.Text;

import java.util.Map;
import java.util.TreeMap;

public class VDistSkew {

    private VCorrelatorPoolAnalytics canvas;
    private TreeMap<Integer, Rectangle> bars = new TreeMap<Integer, Rectangle>();

    private Rectangle screen;
    private Text close;
    private Text title;

    public VDistSkew(VCorrelatorPoolAnalytics canvas)
    {
        this.canvas = canvas;
        screen = canvas.rect(0, 0, canvas.width, canvas.height);
        screen.setFillColor("black");
        screen.setFillOpacity(0.8);
        screen.setStrokeOpacity(0);

        close = canvas.text("Close", 20,40);
        close.setStrokeOpacity(0);
        close.setFillColor("white");
        close.getElement().getStyle().setCursor(Style.Cursor.POINTER);
        close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        title = canvas.text("", 20,20);
        title.setStrokeOpacity(0);
        title.setFillColor("white");

        screen.setVisible(false);
        close.setVisible(false);
        title.setVisible(false);
    }

    public void hide()
    {
        for (Rectangle rect : bars.values())
        {
            rect.setVisible(false);
        }
        screen.setVisible(false);
        close.setVisible(false);
        title.setVisible(false);
    }

    public void show(String data)
    {
        TreeMap<Integer, Double> distMap = new TreeMap<Integer, Double>();
        String id = data.split(";")[0];
        title.setText(id + " Generation: " + canvas.getCorrelatorInfo(id, "GEN"));
        double maxFreq = 0;
        int span = 0;
        String[] values = data.split(";")[1].split(",");
        for(String value : values)
        {
            if(value != null)
            {
                int distClass = Integer.parseInt(value.split("=")[0]);
                if(Math.abs(distClass) > span)
                {
                    span = Math.abs(distClass);
                }
                double freq = Double.parseDouble(value.split("=")[1]);
                if(maxFreq < freq)
                {
                    maxFreq = freq;
                }
                distMap.put(distClass, freq);
            }
        }
        screen.setVisible(true);
        close.setVisible(true);
        canvas.getCanvas().bringToFront(screen);
        screen.setWidth(canvas.width);
        screen.setHeight(canvas.height);
        canvas.getCanvas().bringToFront(close);
        title.setVisible(true);
        canvas.getCanvas().bringToFront(title);

        int wIncrement = (canvas.width - 100)/(2 * span);

        for(Map.Entry<Integer, Double> entry : distMap.entrySet())
        {
            int h = (int) (((canvas.height - 100) * entry.getValue())/maxFreq);

            if(!bars.containsKey(entry.getKey()))
            {
                Rectangle r = canvas.rect(10, 10, 10, 10);
                r.setStrokeOpacity(0);
                r.setFillColor("blue");
                r.setFillOpacity(0.8);
                bars.put(entry.getKey(), r);
            }
            Rectangle r = bars.get(entry.getKey());
            r.setVisible(true);
            r.setX(canvas.width/2 + (entry.getKey() * wIncrement) - wIncrement/2);
            r.setWidth(wIncrement);
            r.setHeight(h);
            r.setY(canvas.height - 50 - h);
            canvas.getCanvas().bringToFront(r);
        }
    }
}
