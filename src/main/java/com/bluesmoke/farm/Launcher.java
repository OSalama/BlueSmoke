package com.bluesmoke.farm;

import com.bluesmoke.farm.correlator.CorrelatorFromCodeFactory;
import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderFromCodeFactory;
import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;
import com.bluesmoke.farm.enumeration.Pair;
import com.bluesmoke.farm.service.feed.CSVFeed;
import com.bluesmoke.farm.service.feed.OHLCFeed;
import com.bluesmoke.farm.util.RuntimeJavaFileCompiler;
import com.bluesmoke.farm.util.SpringContextHelper;
import com.bluesmoke.farm.widgetset.CorrelatorPoolAnalytics;
import com.bluesmoke.farm.worker.PassageOfTimeEmulationWorker;
import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.*;
import org.vaadin.artur.icepush.ICEPush;

import java.io.File;
import java.util.HashMap;

public class Launcher extends Application {

    private Window mainWindow = new Window("BlueSmoke");
    private TabSheet mainLayout;
    private SpringContextHelper helper;

    private CorrelatorPool correlatorPool;
    private CorrelatorBuilderManager correlatorBuilderManager;

    private OHLCFeed feed;
    private String feedsPath;

    private Panel feedControl = new Panel();
    private Select feedSelect = new Select("Select Feed");

    private CorrelatorPoolAnalytics analytics;

    private ICEPush pusher = new ICEPush();

    private Panel workSpace = new Panel();

    private PassageOfTimeEmulationWorker emulator;

    @Override
    public void init() {
        setMainWindow(mainWindow);
        setTheme("bluesmoke");

        helper = new SpringContextHelper(this);
        feed = (OHLCFeed) helper.getBean("feed");
        correlatorPool = (CorrelatorPool) helper.getBean("correlatorPool");
        correlatorBuilderManager = (CorrelatorBuilderManager) helper.getBean("correlatorBuilderManager");
        emulator = (PassageOfTimeEmulationWorker) helper.getBean("emulator");

        buildMainLayout();
        mainWindow.setContent(mainLayout);
        mainLayout.setSizeFull();
        mainWindow.addListener(new Window.CloseListener() {
            public void windowClose(Window.CloseEvent e) {
                analytics.terminate();
            }
        });
        mainWindow.addListener(new Window.ResizeListener() {
            public void windowResized(Window.ResizeEvent e) {
                analytics.resize();
            }
        });
        mainWindow.addComponent(pusher);
    }

    private void buildMainLayout()
    {
        mainLayout = new TabSheet();
        buildAnalytics();
        buildFeedControl();
        buildWorkSpace();
    }

    private void buildAnalytics()
    {
        analytics = new CorrelatorPoolAnalytics(this, correlatorPool);
        mainLayout.addTab(analytics, "Analytics");
        analytics.setHeight("400px");
        analytics.setWidth("400px");
    }

    private void buildWorkSpace()
    {
        mainLayout.addTab(workSpace,"Work Space");
        workSpace.setSizeFull();

        final TextField className = new TextField("Correlator Class Name");
        final TextArea classCode = new TextArea("Correlator Code");

        final TextField builderClassName = new TextField("Builder Class Name");
        final TextArea builderClassCode = new TextArea("Builder Code");

        final CheckBox withBuilder = new CheckBox("Correlator comes with builder?");
        withBuilder.setImmediate(true);
        withBuilder.addListener(new Property.ValueChangeListener() {
            public void valueChange(Property.ValueChangeEvent event) {
                if(withBuilder.booleanValue())
                {
                    builderClassName.setVisible(true);
                    builderClassCode.setVisible(true);
                }
                else {
                    builderClassName.setVisible(false);
                    builderClassCode.setVisible(false);
                }

            }
        });

        final Button compile = new Button("Add Correlator");
        compile.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {

                if(!withBuilder.booleanValue())
                {
                    CorrelatorFromCodeFactory.createCorrelator(
                            (String) helper.getBean("classPath"),
                            className.getValue().toString(),
                            classCode.getValue().toString(),
                            correlatorBuilderManager,
                            feed, correlatorPool, null, null, null);
                }
                else {
                    RuntimeJavaFileCompiler.compile((String) helper.getBean("classPath"), className.getValue().toString(), classCode.getValue().toString());
                    CorrelatorBuilderFromCodeFactory.createCorrelatorBuilder((String) helper.getBean("classPath"),
                                                                            builderClassName.getValue().toString(),
                                                                            builderClassCode.getValue().toString(),
                                                                            correlatorBuilderManager, feed, correlatorPool);
                }
            }
        });

        workSpace.addComponent(className);
        workSpace.addComponent(classCode);
        workSpace.addComponent(withBuilder);
        workSpace.addComponent(builderClassName);
        workSpace.addComponent(builderClassCode);
        builderClassName.setVisible(false);
        builderClassCode.setVisible(false);
        workSpace.addComponent(compile);

        classCode.setWidth("100%");
        classCode.setHeight("500px");
        builderClassCode.setWidth("100%");
        builderClassCode.setHeight("500px");
    }

    private void buildFeedControl()
    {
        mainLayout.addTab(feedControl, "Feed Management");
        feedControl.setSizeFull();

        HorizontalLayout h1 = new HorizontalLayout();
        final TextField feedPath = new TextField("Feeds Folder:");
        h1.addComponent(feedPath);
        final Button updateFeedPath = new Button("Update");
        h1.addComponent(updateFeedPath);
        h1.setComponentAlignment(updateFeedPath, Alignment.BOTTOM_RIGHT);
        feedControl.addComponent(h1);

        updateFeedPath.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                feed.reset();
                feedsPath = feedPath.getValue().toString();
                feed.setFeedsPath(feedsPath);
                buildFeedSelect();
            }
        });

        HorizontalLayout h2 = new HorizontalLayout();
        final Select pairSelect = buildPairSelect();
        h2.addComponent(pairSelect);
        h2.addComponent(feedSelect);
        final Button addPairFeed = new Button("Add");
        h2.addComponent(addPairFeed);
        h2.setComponentAlignment(addPairFeed, Alignment.BOTTOM_RIGHT);
        feedControl.addComponent(h2);

        addPairFeed.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                feed.addPairFeed((Pair)pairSelect.getValue(), feedSelect.getValue().toString());
            }
        });

        HorizontalLayout h3 = new HorizontalLayout();
        final TextField maxPopulation = new TextField("Maximum Population:");
        maxPopulation.setValue("200");
        h3.addComponent(maxPopulation);
        final Button updateMaxPop = new Button("Update");
        h3.addComponent(updateMaxPop);
        h3.setComponentAlignment(updateMaxPop, Alignment.BOTTOM_RIGHT);
        feedControl.addComponent(h3);

        updateMaxPop.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                correlatorPool.setMaxPopulation(Integer.parseInt(maxPopulation.getValue().toString()));
            }
        });


        final Button startSim = new Button("Start Learning");

        feedControl.addComponent(startSim);

        startSim.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                try
                {
                    emulator.startEmulation();
                    correlatorPool.activate();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        final Button endSim = new Button("End Learning");

        feedControl.addComponent(endSim);

        endSim.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                try
                {
                    emulator.die();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        final Button pauseFeed = new Button("Pause Feed");

        feedControl.addComponent(pauseFeed);

        pauseFeed.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                feed.pause();
            }
        });

        final Button resumeFeed = new Button("Resume Feed");

        feedControl.addComponent(resumeFeed);

        resumeFeed.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                feed.resume();
            }
        });

        final Button testInit = new Button("Use Test Init");

        feedControl.addComponent(testInit);

        testInit.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                feed.reset();
                //feedsPath = "C:\\Users\\Oblene\\Desktop\\Sandbox\\Data";
                feedsPath = "/app/testing/hans/WebApps/Data";
                feed.setFeedsPath(feedsPath);
                buildFeedSelect();

                feed.addPairFeed(Pair.EURUSD, "EURUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv");
                feed.addPairFeed(Pair.GBPUSD, "GBPUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv");
                feed.addPairFeed(Pair.USDCHF, "USDCHF_5 Mins_Bid_2008.01.01_2012.12.31.csv");
                //feed.addPairFeed(Pair.USDJPY, "USDJPY_5 Mins_Bid_2008.01.01_2012.12.31.csv");
                feed.addPairFeed(Pair.AUDUSD, "AUDUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv");
                feed.addPairFeed(Pair.NZDUSD, "NZDUSD_5 Mins_Bid_2008.01.01_2012.12.31.csv");
                feed.addCalendarFeed("Calendar_2008.csv");

                CorrelatorFromCodeFactory.createCorrelator(
                        (String) helper.getBean("classPath"),
                        "MovingAverageCorrelator",
                        "import com.bluesmoke.farm.correlator.CorrelatorPool;\n" +
                                "import com.bluesmoke.farm.correlator.GenericCorrelator;\n" +
                                "import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;\n" +
                                "import com.bluesmoke.farm.model.tickdata.Tick;\n" +
                                "import com.bluesmoke.farm.service.feed.FeedService;\n" +
                                "\n" +
                                "import java.util.HashMap;\n" +
                                "import java.util.Random;\n" +
                                "\n" +
                                "public class MovingAverageCorrelator extends GenericCorrelator{\n" +
                                "\n" +
                                "    private int refresh = 0;\n" +
                                "    private double ma;\n" +
                                "    private double sum = 0;\n" +
                                "\n" +
                                "    public MovingAverageCorrelator(String id, CorrelatorBuilderManager correlatorBuilderManager, CorrelatorPool pool, FeedService feed, GenericCorrelator aggressiveParent, GenericCorrelator passiveParent, HashMap<String, Object> config)\n" +
                                "    {\n" +
                                "        super(\"MovingAverage_\" + pool.getNextID(), correlatorBuilderManager, pool, feed, aggressiveParent, passiveParent, config);\n" +
                                "        Random rand = new Random();\n" +
                                "        this.config.put(\"price_type\", rand.nextInt(4));\n"+
                                "    }\n" +
                                "\n" +
                                "\n" +
                                "    @Override\n" +
                                "    public void createMutant() {\n" +
                                "        new MovingAverageCorrelator(\"MovingAverage_\" + pool.getNextID(), correlatorBuilderManager, pool, feed, null, null, config);\n" +
                                "    }\n" +
                                "\n" +
                                "    @Override\n" +
                                "    public String createState() {\n" +
                                "\n" +
                                "        if(refresh == 0)\n" +
                                "        {\n" +
                                "            refresh = 1000;\n" +
                                "            sum = 0;\n" +
                                "            boolean passedFirst = false;\n" +
                                "            for(Tick tick : ticks)\n" +
                                "            {\n" +
                                "                   double price = 0;\n" +
                                "                   switch ((Integer)config.get(\"price_type\"))\n" +
                                "                   {\n" +
                                "                       case 0:\n" +
                                "                           price = tick.getPairData(pair.name()).getOpen();\n" +
                                "                           break;\n" +
                                "                       case 1:\n" +
                                "                           price = tick.getPairData(pair.name()).getClose();\n" +
                                "                           break;\n" +
                                "                       case 2:\n" +
                                "                           price = tick.getPairData(pair.name()).getHigh();\n" +
                                "                           break;\n" +
                                "                       case 3:\n" +
                                "                           price = tick.getPairData(pair.name()).getLow();\n" +
                                "                           break;\n" +
                                "                   }" +

                                "                if(passedFirst)\n" +
                                "                {\n" +
                                "                    sum += price;\n" +
                                "                }\n" +
                                "                else {\n" +
                                "                    passedFirst = true;\n" +
                                "                }\n" +
                                "            }\n" +
                                "            ma = sum/(ticks.size() - 1);\n" +
                                "        }\n" +
                                "        else {\n" +
                                "           double priceRemove = 0;" +
                                "           double priceAdd = 0;" +
                                "           switch ((Integer)config.get(\"price_type\"))\n" +
                        "                   {\n" +
                        "                       case 0:\n" +
                        "                           priceRemove = ticks.get(0).getPairData(pair.name()).getOpen();\n" +
                        "                           priceAdd = currentTick.getPairData(pair.name()).getOpen();\n" +
                        "                           break;\n" +
                        "                       case 1:\n" +
                        "                           priceRemove = ticks.get(0).getPairData(pair.name()).getClose();\n" +
                        "                           priceAdd = currentTick.getPairData(pair.name()).getClose();\n" +
                        "                           break;\n" +
                        "                       case 2:\n" +
                        "                           priceRemove = ticks.get(0).getPairData(pair.name()).getHigh();\n" +
                        "                           priceAdd = currentTick.getPairData(pair.name()).getHigh();\n" +
                        "                           break;\n" +
                        "                       case 3:\n" +
                        "                           priceRemove = ticks.get(0).getPairData(pair.name()).getLow();\n" +
                        "                           priceAdd = currentTick.getPairData(pair.name()).getLow();\n" +
                        "                           break;\n" +
                        "                   }" +
                                "            sum -= priceRemove;\n" +
                                "            sum += priceAdd;\n" +
                                "            ma = sum/(ticks.size() - 1);\n" +
                                "        }\n" +
                                "        currentUnderlyingComponents.put(\"MA\", ma);\n" +
                                "        refresh--;\n" +
                                "        return \"\" + (int)(ma/(100 * resolution));\n" +
                                "    }\n" +
                                "}\n",
                        correlatorBuilderManager,
                        feed, correlatorPool, null, null, null);

                CorrelatorFromCodeFactory.createCorrelator(
                        (String) helper.getBean("classPath"),
                        "PriceCorrelator",
                        "import com.bluesmoke.farm.correlator.CorrelatorPool;\n" +
                                "import com.bluesmoke.farm.correlator.GenericCorrelator;\n" +
                                "import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;\n" +
                                "import com.bluesmoke.farm.model.tickdata.Tick;\n" +
                                "import com.bluesmoke.farm.service.feed.FeedService;\n" +
                                "\n" +
                                "import java.util.HashMap;\n" +
                                "import java.util.Random;\n" +
                                "\n" +
                                "public class PriceCorrelator extends GenericCorrelator{\n" +
                                "\n" +
                                "    public PriceCorrelator(String id, CorrelatorBuilderManager correlatorBuilderManager, CorrelatorPool pool, FeedService feed, GenericCorrelator aggressiveParent, GenericCorrelator passiveParent, HashMap<String, Object> config)\n" +
                                "    {\n" +
                                "        super(\"Price_\" + pool.getNextID(), correlatorBuilderManager, pool, feed, aggressiveParent, passiveParent, config);\n" +
                                "        Random rand = new Random();\n" +
                                "        this.config.put(\"price_type\", rand.nextInt(4));\n" +
                                "    }\n" +
                                "\n" +
                                "\n" +
                                "    @Override\n" +
                                "    public void createMutant() {\n" +
                                "        new PriceCorrelator(\"Price_\" + pool.getNextID(), correlatorBuilderManager, pool, feed, null, null, config);\n" +
                                "    }\n" +
                                "\n" +
                                "    @Override\n" +
                                "    public String createState() {\n" +
                                "\n" +
                                "        double price = 0;\n" +
                                "        switch ((Integer)config.get(\"price_type\"))\n" +
                                "        {\n" +
                                "            case 0:\n" +
                                "                price = currentTick.getPairData(pair.name()).getOpen();\n" +
                                "                break;\n" +
                                "            case 1:\n" +
                                "                price = currentTick.getPairData(pair.name()).getClose();\n" +
                                "                break;\n" +
                                "            case 2:\n" +
                                "                price = currentTick.getPairData(pair.name()).getHigh();\n" +
                                "                break;\n" +
                                "            case 3:\n" +
                                "                price = currentTick.getPairData(pair.name()).getLow();\n" +
                                "                break;\n" +
                                "        }\n" +
                                "        currentUnderlyingComponents.put(\"price\", price);\n" +
                                "        return \"\" + (int)(price/(10 * resolution));\n" +
                                "    }\n" +
                                "}\n",
                        correlatorBuilderManager,
                        feed, correlatorPool, null, null, null);
            }
        });



        final Button checkPool = new Button("Check Pool");

        feedControl.addComponent(checkPool);

        checkPool.addListener(new Button.ClickListener() {
            public void buttonClick(Button.ClickEvent event) {
                System.out.println(correlatorPool.getHandlesInfo());
            }
        });
    }

    private Select buildPairSelect()
    {
        IndexedContainer pairsCon = new IndexedContainer();
        for (Pair pair : Pair.values())
        {
            pairsCon.addItem(pair);
        }
        Select pairSelect = new Select("Select Pair");
        pairSelect.setContainerDataSource(pairsCon);
        return pairSelect;
    }

    private void buildFeedSelect()
    {
        IndexedContainer feedsCon = new IndexedContainer();
        File feedFolder = new File(feedsPath);
        for(String feed : feedFolder.list())
        {
            feedsCon.addItem(feed);
        }
        feedSelect.setContainerDataSource(feedsCon);
    }

    public ICEPush getPusher()
    {
        return pusher;
    }
}
