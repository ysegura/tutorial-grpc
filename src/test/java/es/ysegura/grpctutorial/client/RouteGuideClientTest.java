package es.ysegura.grpctutorial.client;

import es.ysegura.grpctutorial.RouteGuideUtil;
import es.ysegura.grpctutorial.protobuff.Feature;
import es.ysegura.grpctutorial.protobuff.Point;
import es.ysegura.grpctutorial.protobuff.RouteNote;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteGuideClientTest {

    private final ManagedChannel channel = Grpc.newChannelBuilder("localhost:8980", InsecureChannelCredentials.create()).build();

    private final RouteGuideClient client = new RouteGuideClient(channel);

    @Test
    void test_GetValidFeature() {
        Feature feature = client.getFeature(409146138, -746188906);
        assertEquals("Berkshire Valley Management Area Trail, Jefferson, NJ, USA", feature.getName());
        assertEquals(409146138, feature.getLocation().getLatitude());
        assertEquals(-746188906, feature.getLocation().getLongitude());
    }

    @Test
    void testGetInvalidFeature() {
        Feature feature = client.getFeature(0, 0);
        assertEquals("", feature.getName());
        assertEquals(0, feature.getLocation().getLatitude());
        assertEquals(0, feature.getLocation().getLongitude());
    }

    @Test
    void testGetFeatureListFromArea() {
        Iterator<Feature> features = client.listFeatures(400000000, -750000000, 420000000, -730000000);
        Assertions.assertNotNull(features);
        List<Feature> defaultFeatures = getDefaultFeatures();
        features.forEachRemaining(feature -> assertTrue(defaultFeatures.contains(feature)));
    }

    @Test
    void testRecordRoute() throws InterruptedException {
        Random random = new Random();
        List<Feature> features = getDefaultFeatures();
        List<Point> points = new ArrayList<>();

        for (int i = 0; i < 10; ++i) {
            int index = random.nextInt(features.size());
            points.add(features.get(index).getLocation());
        }

        client.recordRoute(points);
    }
    @Test
    void test_RouteChat() throws InterruptedException {
        List<RouteNote> routeNotes = List.of(
                newNote("First message", 0, 0),
                newNote("Second message", 0, 10_000_000),
                newNote("Third message", 10_000_000, 0),
                newNote("Fourth message", 10_000_000, 10_000_000)
        );
        CountDownLatch finishLatch = client.routeChat(routeNotes);

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            System.err.println("routeChat couldn't finish within 1 minutes");
        }
    }

    private List<Feature> getDefaultFeatures(){
        try {
            return RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    private RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message)
                .setLocation(
                        Point.newBuilder()
                                .setLatitude(lat)
                                .setLongitude(lon)
                                .build())
                .build();
    }
}