package com.bluesmoke.farm.correlator;

import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;
import com.bluesmoke.farm.service.feed.FeedService;
import com.bluesmoke.farm.util.RuntimeJavaFileCompiler;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class CorrelatorFromCodeFactory {
   public static GenericCorrelator createCorrelator(String classPath, String className, String correlatorCode, CorrelatorBuilderManager correlatorBuilderManager, FeedService feed, CorrelatorPool correlatorPool, GenericCorrelator aParent, GenericCorrelator pParent, HashMap<String, Object> config)
   {
       GenericCorrelator correlator = null;
       try
       {
           RuntimeJavaFileCompiler.compile(classPath, className, correlatorCode);

           ClassLoader classLoader = feed.getClass().getClassLoader();

           Class correlatorClass = classLoader.loadClass(className);
           Constructor constructor = correlatorClass.getConstructor(String.class, CorrelatorBuilderManager.class, CorrelatorPool.class, FeedService.class, GenericCorrelator.class, GenericCorrelator.class, HashMap.class);
           correlator = (GenericCorrelator)constructor.newInstance(correlatorPool.getNextID(),correlatorBuilderManager, correlatorPool, feed, aParent, pParent, config);
       }
       catch (Exception e)
       {
           e.printStackTrace();
       }
       return correlator;
   }
}
