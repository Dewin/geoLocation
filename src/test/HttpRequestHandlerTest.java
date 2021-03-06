import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by Simon on 2017-07-02. :)
 */
public class HttpRequestHandlerTest {

    @Test
    public void getStoreNamesByCityFindCorrectLink() throws Exception {
        StoreScanner storeScanner = new StoreScanner("78.40.41.15");
        List<Store> stores = storeScanner.getStoresInRadius(2900);
        assertEquals("/butiker/maxi/sodertalje/maxi-ica-stormarknad-sodertalje-11690/erbjudanden/", stores.get(4).getDiscountLink());
    }

    @Test
    public void getStoreNamesByCityFindCorrectLink2() throws Exception {
        StoreScanner storeScanner = new StoreScanner("78.40.41.15");
        List<Store> stores = storeScanner.getStoresInRadius(2900);
        assertEquals("/butiker/nara/sodertalje/ica-nara-orren-1356/butikserbjudanden/", stores.get(0).getDiscountLink());
    }

}