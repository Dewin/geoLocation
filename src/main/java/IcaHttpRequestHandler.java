import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;

/**
 * Created by Simon on 2017-12-25. :)
 */
public class IcaHttpRequestHandler extends HttpRequestHandler {

    private Map<String, Pair<String, String>> foundStoresWithLink;

    public IcaHttpRequestHandler(String city) {
        List<Element> cards = findAllStoresInCity(city);
        foundStoresWithLink = extractNameAndAddressWithLink(cards);
    }

    protected String findStoreWebsiteAddress(Store store) {
        Map<String, String> storeNameAndAddress = new HashMap<String, String>();
        for (Map.Entry<String, Pair<String, String>> e: foundStoresWithLink.entrySet())
            storeNameAndAddress.put(e.getKey(), e.getValue().getKey());

        for (Map.Entry<String, String> e : storeNameAndAddress.entrySet()) {
            if (store.getAddress().toLowerCase().contains(e.getValue().toLowerCase()))
                return removeEntryAndReturn(e.getKey());
        }
        return null;
    }

    protected String findStoreWebsiteNameExact(Store store) {
        Set<String> foundStoreNames = foundStoresWithLink.keySet();
        for (String foundStore : foundStoreNames) {
            if (store.getName().equals(foundStore))
                return removeEntryAndReturn(foundStore);
        }
        return null;
    }

    protected String findStoreWebsiteNameNotSoExact(Store store) {
        Set<String> foundStoreNames = foundStoresWithLink.keySet();
        for (String foundStore : foundStoreNames) {
            int lengthSearch = (store.getName().length() > 15 && foundStore.length() > 15) ? 15 : (foundStore.length() < store.getName().length()) ? foundStore.length() : store.getName().length();
            boolean attempt = false;

            if (store.getName().split(" ").length > 2)
                attempt = store.getName().split(" ")[2].contains(foundStore);
            if (attempt || store.getName().substring(0, lengthSearch).equals(foundStore.substring(0, lengthSearch))) {
                return removeEntryAndReturn(foundStore);
            }
        }
        return null;
    }

    protected String findStoreWebsiteNameDistance(Store store) {
        Set<String> foundStoreNames = foundStoresWithLink.keySet();
        double bestScore = store.getName().length();
        String currentWinner = "";
        for (String foundStore : foundStoreNames) {
            double distanceScore = distanceScore(foundStore, store.getName());
            if (distanceScore < bestScore) {
                bestScore = distanceScore;
                currentWinner = foundStore;
            }
        }

        if (bestScore < store.getName().length())
            return removeEntryAndReturn(currentWinner);
        return null;
    }

    public void addAllDiscountedItems(Store store) {
        List<Element> sliderElements = findAllSliderElements(store.getDiscountLink());
        String name;
        String info;
        String imageLink;
        double price;
        int amount;
        Unit unit;
        if (sliderElements != null) {
            for (Element e : sliderElements) {
                name = extractName(e);
                info = extractInfo(e);
                imageLink = extractImageLink(e);
                price = extractPrice(e);
                amount = extractAmount(e);
                unit = extractUnit(e);
                Item item = new Item(name, info, imageLink, price, amount, unit);
                item.setCondition(extractCondition(e));
                store.addItem(item);
            }
        }
        //TODO lägga till fler rabbater än bara slider-elementen
    }

    private static List<Element> findAllSliderElements(String discountLink) {
        String requestURL = "https://www.ica.se" + discountLink;
        try {
            Document doc = Jsoup.connect(requestURL).maxBodySize(0).get();
            return doc.select("div.hit-item");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String extractName(Element element) {
        return element.getElementsByClass("item-name-product").text();
    }

    private static String extractInfo(Element element) {
        return element.getElementsByClass("item-info").text();
    }

    private static String extractImageLink(Element element) {
        return element.getElementsByClass("item-image").attr("src");
    }

    private static double extractPrice(Element element) {
        String valueString = element.getElementsByClass("product-price__price-value").text().replaceAll("[:\\-%]", "");
        int value = Integer.parseInt(valueString);
        String decimalString = element.getElementsByClass("product-price__decimal").text();
        if (!decimalString.equals("")) {
            int decimal = Integer.parseInt(decimalString);
            return Double.parseDouble(value + "." + decimal);
        }
        return Double.parseDouble(value + "");
    }

    private static int extractAmount(Element element) {
        String amountString = element.getElementsByClass("product-price__amount").text().replaceAll(" för", "");
        int amount = 1;
        if (!amountString.equals("")) {
            amount = Integer.parseInt(amountString);
        }
        return amount;
    }

    private static Unit extractUnit(Element element) {
        String unitString = element.getElementsByClass("product-price__unit-item").text().toUpperCase().replace("/", "").replace(" ", "");
        if (unitString.equals("KG")) {
            return Unit.KG;
        } else if (unitString.equals("ST")) {
            return Unit.ST;
        } else if (unitString.equals("RABATT")) {
            return Unit.RABATT;
        } else if (unitString.equals("")) {
            return Unit.EMPTY;
        } else {
            return null;
        }
    }

    private Condition extractCondition(Element element) {
        String elementString = element.getElementsByClass("product-price__unit-item").text().toUpperCase().replace("/", "").replace(" ", "");
        Condition condition = new Condition();
        if (elementString.contains("OMDUHANDLARFÖR")) {
            condition.setType(ConditionType.BUY_FOR_ATLEAST);
            int test = Integer.parseInt(elementString.replaceAll("[^0-9]", ""));
            condition.setAmount(test);
        }

        if (condition.getType() != null)
            return condition;
        return null;
    }

    private String removeEntryAndReturn(String foundStore) {
        String correctLink = foundStoresWithLink.get(foundStore).getValue();
        foundStoresWithLink.remove(foundStore);
        return correctLink;
    }

    private List<Element> findAllStoresInCity(String city) {
        String errName = city;
        city = city.toLowerCase().replaceAll("[åä]", "a").replaceAll("ö", "o").replaceAll(" ", "-");
        if (city.equals("skogas"))
            city = "huddinge";
        String requestURL = "https://www.ica.se/butiker/" + city;
        try {
            Document doc = Jsoup.connect(requestURL).maxBodySize(0).get();
            return doc.select("store-card-list-item:not(.compact)");
        } catch (IOException e) {
            //TODO take care of when ica doesnt have that webpage. Maybe search for address or name from google
            System.err.print("Seems like ICA made a slip, city " + errName + " is missing it's webpage. ");
            e.printStackTrace();
        }
        return null;
    }

    private Map<String, Pair<String, String>> extractNameAndAddressWithLink(List<Element> storeElements) {
        Map<String, Pair<String, String>> legitStores = new HashMap<String, Pair<String, String>>();
        for (Element e : storeElements) {
            String storeName = e.getElementsByClass("card-heading").text().toLowerCase().replaceAll("(maxi ica)", "ica maxi");
            String address = e.getElementsByClass("street-name").text();
            String city = e.getElementsByClass("store-address").select("span").get(1).text();
            String discountLink = e.getElementsByClass("store-card-store-link").last().attr("href");
            legitStores.put(storeName, new Pair<String, String>(address + ", " + city, discountLink));
        }
        return legitStores;
    }
}
