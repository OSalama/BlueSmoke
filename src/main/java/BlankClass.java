//This is just a template file for a correlator

import com.bluesmoke.farm.correlator.CorrelatorPool;
import com.bluesmoke.farm.correlator.GenericCorrelator;
import com.bluesmoke.farm.correlator.builder.CorrelatorBuilderManager;
import com.bluesmoke.farm.model.tickdata.Tick;
import com.bluesmoke.farm.service.feed.FeedService;

import java.util.HashMap;
import java.util.Random;

public class BlankClass extends GenericCorrelator{

    public BlankClass(String id, CorrelatorBuilderManager correlatorBuilderManager, CorrelatorPool pool, FeedService feed, GenericCorrelator aggressiveParent, GenericCorrelator passiveParent, HashMap<String, Object> config)
    {
        super("Price_" + pool.getNextID(), correlatorBuilderManager, pool, feed, aggressiveParent, passiveParent, config);

        Random rand = new Random();
        config.put("price_type", rand.nextInt(4));
    }


    @Override
    public void createMutant() {
        new BlankClass("Price_" + pool.getNextID(), correlatorBuilderManager, pool, feed, null, null, config);
    }

    @Override
    public String createState() {

        double price = 0;
        switch ((Integer)config.get("price_type"))
        {
            case 0:
                price = currentTick.getPairData(pair.name()).getOpen();
                break;
            case 1:
                price = currentTick.getPairData(pair.name()).getClose();
                break;
            case 2:
                price = currentTick.getPairData(pair.name()).getHigh();
                break;
            case 3:
                price = currentTick.getPairData(pair.name()).getLow();
                break;
        }

        currentUnderlyingComponents.put("price", price);

        return "" + (int)(price/(10 * resolution));
    }
}
