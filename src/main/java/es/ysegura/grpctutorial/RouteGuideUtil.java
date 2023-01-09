package es.ysegura.grpctutorial;

import com.google.protobuf.util.JsonFormat;
import es.ysegura.grpctutorial.protobuff.Feature;
import es.ysegura.grpctutorial.protobuff.FeatureDatabase;
import es.ysegura.grpctutorial.protobuff.Point;
import es.ysegura.grpctutorial.server.RouteGuideServerRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.Math.*;

public class RouteGuideUtil {
    private static final double COORD_FACTOR = 1e7;

    private RouteGuideUtil() {
    }

    public static double getLatitude(Point location){
        return location.getLatitude() / COORD_FACTOR;
    }

    public static double getLongitude(Point location){
        return location.getLongitude() / COORD_FACTOR;
    }

    public static URL getDefaultFeaturesFile(){
        return RouteGuideServerRunner.class.getResource("route_guide_db.json");
    }

    public static List<Feature> parseFeatures(URL file) throws IOException {
        InputStream input = file.openStream();

        try (input) {
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            try (reader) {
                FeatureDatabase.Builder database = FeatureDatabase.newBuilder();
                JsonFormat.parser().merge(reader, database);
                return database.getFeatureList();
            }
        }
    }

    public static boolean exists(Feature feature){
        return feature != null && !feature.getName().isEmpty();
    }

    public static int calcDistance(Point start, Point end){
        int r = 6371000; // Earth radius in meters
        double lat1 = toRadians(RouteGuideUtil.getLatitude(start));
        double lat2 = toRadians(RouteGuideUtil.getLatitude(end));
        double lon1 = toRadians(RouteGuideUtil.getLongitude(start));
        double lon2 = toRadians(RouteGuideUtil.getLongitude(end));

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a = getArc(lat1, lat2, deltaLat, deltaLon);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));

        return (int) (r * c);
    }

    private static double getArc(double lat1, double lat2, double deltaLat, double deltaLon) {
        return sin(deltaLat / 2) * sin(deltaLat / 2)
                + cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2);
    }
}
