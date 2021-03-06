import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import location.GeoIPv4;
import location.GeoLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;

/**
 * Created by Simon on 2017-06-24.
 */
public class StoreScanner {

    private GeoLocation location;
    private static List<StoreType> ALLOWED_STORES = Arrays.asList(StoreType.values());

    public StoreScanner() {
        this.location = GeoIPv4.getLocation(findMyIpAddress());
    }

    public StoreScanner(String ipAddress) {
        this.location = GeoIPv4.getLocation(ipAddress);
    }

    public GeoLocation getLocation() {
        return location;
    }

    public List<Store> getStoresInRadius(int radius) throws Exception {
        String googleApiString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + location.getLatitude() + "," + location.getLongitude() + "&radius=" + radius + "&type=grocery_or_supermarket&key=AIzaSyCSBYhOCNcjRBDp_Qzfg_8wMejojcvNdbs";
        URL googleApiUrl = new URL(googleApiString);
        HttpURLConnection request = (HttpURLConnection) googleApiUrl.openConnection();
        request.connect();

        JsonParser jp = new JsonParser();
        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
        JsonArray result = (JsonArray) root.getAsJsonObject().get("results");
        request.disconnect();

        Map<Integer, StoreType> acceptedStoresIndexAndType = new HashMap<Integer, StoreType>();
        for (int i = 0; i < result.size(); i++) {
            String storeName = ((JsonObject) result.get(i)).get("name").getAsString();
            for (StoreType type : ALLOWED_STORES) {
                //TODO have a nono-list (as APOTEK below)
                if (storeName.toUpperCase().contains(type.name()) && !storeName.toUpperCase().contains("APOTEK")) {
                    acceptedStoresIndexAndType.put(i, type);
                    break;
                }
            }
        }
        List<Store> storeList = new ArrayList<Store>();
        for (Map.Entry<Integer, StoreType> entry : acceptedStoresIndexAndType.entrySet()) {
            int index = entry.getKey();
            JsonObject tempRoot = ((JsonObject) result.get(index));
            float latitude = tempRoot.get("geometry").getAsJsonObject().get("location").getAsJsonObject().get("lat").getAsFloat();
            float longitude = tempRoot.get("geometry").getAsJsonObject().get("location").getAsJsonObject().get("lng").getAsFloat();
            String name = tempRoot.get("name").getAsString().toLowerCase().replaceAll("(maxi ica)", "ica maxi");
            String address = tempRoot.get("vicinity").getAsString();
            StoreType type = entry.getValue();

            Store store = new Store(latitude, longitude, location.distance(latitude, longitude), name, address, type);
            storeList.add(store);
        }
        Collections.sort(storeList);
        HttpRequestHandler.resetHandlers();
        HttpRequestHandler.linkStoresWithWebsite(storeList, location.getLatitude(), location.getLongitude());
        for (Store s : storeList) {
            HttpRequestHandler.addStoreDiscountedItems(s, location.getLatitude(), location.getLongitude());
        }
        return storeList;
    }

    //TODO upgrade way of finding, using mobile should become way better
    private String findMyIpAddress() {
        URL url;
        BufferedReader in;
        String ipAddress = "";
        try {
            url = new URL("http://bot.whatismyipaddress.com");
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            ipAddress = in.readLine().trim();
             /*IF not connected to internet, then
             * the above code will return one empty
             * String, we can check it's length and
             * if length is not greater than zero,
             * then we can go for LAN IP or Local IP
             * or PRIVATE IP
             */
            if (!(ipAddress.length() > 0)) {
                try {
                    return findMyIpAddressBackup();
                } catch (Exception exp) {
                    ipAddress = "ERROR";
                }
            }
        } catch (Exception ex) {
            // This try will give the Private IP of the Host.
            try {
                return findMyIpAddressBackup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ipAddress;
    }

    private String findMyIpAddressBackup() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            System.out.println((ip.getHostAddress()).trim());
            return (ip.getHostAddress()).trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
