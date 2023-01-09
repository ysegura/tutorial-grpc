package es.ysegura.grpctutorial.client;

import es.ysegura.grpctutorial.RouteGuideUtil;
import es.ysegura.grpctutorial.protobuff.Feature;
import es.ysegura.grpctutorial.protobuff.Point;
import es.ysegura.grpctutorial.protobuff.RouteNote;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RouteGuideClientRunner {

    private static final Random RANDOM = new Random();
    private static List<Feature> features;
    private static RouteGuideClient client;

    private static final String SERVER = "localhost:8980";

    public static void main(String[] args) throws InterruptedException {

        // Cargamos la lista de features para poder probar con algún punto conocido
        features = getDefaultFeatures();

        // Definimos la conexión con el servidor gRPC
        ManagedChannel channel = Grpc.newChannelBuilder(SERVER, InsecureChannelCredentials.create()).build();

        try {
            client = new RouteGuideClient(channel);

            // Invocaciones a los métodos gRPC
            testGetValidFeature();
            testGetInvalidFeature();
            testGetFeatureListFromArea();
            testRecordRoute();
            testRouteChat();
        } finally {
            // Aseguramos cerrar de forma ordenada la conexión
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void testGetValidFeature() {
        client.getFeature(409146138, -746188906);
    }

    private static void testGetInvalidFeature() {
        client.getFeature(0, 0);
    }

    private static void testGetFeatureListFromArea() {
        client.listFeatures(400000000, -750000000, 420000000, -730000000);
    }

    private static void testRecordRoute() throws InterruptedException {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            points.add(getRandomPoint(features, RANDOM));
        }

        client.recordRoute(points);
    }

    private static Point getRandomPoint(List<Feature> features, Random random) {
        int index = random.nextInt(features.size());
        return features.get(index).getLocation();
    }

    private static void testRouteChat() throws InterruptedException {
        var requests = List.of(
                newNote("First message", 0, 0),
                newNote("Second message", 0, 10_000_000),
                newNote("Third message", 10_000_000, 0),
                newNote("Fourth message", 10_000_000, 10_000_000)
        );
        CountDownLatch finishLatch = client.routeChat(requests);

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            log.warn("routeChat couldn't finish within 1 minutes");
        }
    }

    private static List<Feature> getDefaultFeatures(){
        try {
            return RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        } catch (IOException ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message)
                .setLocation(
                        Point.newBuilder()
                                .setLatitude(lat)
                                .setLongitude(lon)
                                .build())
                .build();
    }

}