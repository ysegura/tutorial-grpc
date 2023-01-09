package es.ysegura.grpctutorial.client;

import es.ysegura.grpctutorial.RouteGuideUtil;
import es.ysegura.grpctutorial.protobuff.*;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RouteGuideClient {

    // Define las llamadas bloqueantes, que nos aparecerán como métodos del blockingStub
    private final RouteGuideGrpc.RouteGuideBlockingStub blockingStub;

    // Define las llamadas que utilizan stream (asíncronas). Se invocan como métodos del asyncStub
    private final RouteGuideGrpc.RouteGuideStub asyncStub;
    private final Random random = new Random();

    public RouteGuideClient(Channel channel) {
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
    }

    /**
     * Blocking unary call example.  Calls getFeature and prints the response.
     */
    public Feature getFeature(int lat, int lon) {
        log.info(String.format("*** GetFeature: lat=%d lon=%d", lat, lon));

        Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();

        try {
            Feature feature = blockingStub.getFeature(request);
            printFeatureInformation(feature);
            return feature;
        } catch (StatusRuntimeException e) {
            log.warn(String.format("RPC failed: %s", e.getStatus()));
            return null;
        }
    }

    private void printFeatureInformation(Feature feature) {
        Point location = feature.getLocation();
        if (RouteGuideUtil.exists(feature)) {
            log.info(String.format("Found feature called \"%s\" at %f, %f",
                    feature.getName(),
                    RouteGuideUtil.getLatitude(location),
                    RouteGuideUtil.getLongitude(location)));
        } else {
            log.info(String.format("Found no feature at %f, %f",
                    RouteGuideUtil.getLatitude(location),
                    RouteGuideUtil.getLongitude(location)));
        }
    }

    /**
     * Blocking server-streaming example. Calls listFeatures with a rectangle of interest. Prints each
     * response feature as it arrives.
     */
    public Iterator<Feature> listFeatures(int lowLat, int lowLon, int hiLat, int hiLon) {
        log.info(String.format("*** ListFeatures: lowLat=%d lowLon=%d hiLat=%d hiLon=%d", lowLat, lowLon, hiLat, hiLon));

        Rectangle rectangle =
                Rectangle.newBuilder()
                        .setLo(Point.newBuilder().setLatitude(lowLat).setLongitude(lowLon).build())
                        .setHi(Point.newBuilder().setLatitude(hiLat).setLongitude(hiLon).build())
                        .build();
        try {
            // Mostramos los resultados obtenidos
            Iterator<Feature> features = blockingStub.listFeatures(rectangle);
            features.forEachRemaining(f -> log.info(String.format("Feature found:%n%s", f)));
            return features;
        } catch (StatusRuntimeException e) {
            log.warn(String.format("RPC failed: %s", e.getStatus()));
            return null;
        }
    }

    /**
     * Async client-streaming example. Sends {@code numPoints} randomly chosen points from {@code
     * features} with a variable delay in between. Prints the statistics when they are sent from the
     * server.
     */
    public void recordRoute(List<Point> points) throws InterruptedException {
        log.info("*** RecordRoute");
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<RouteSummary> responseObserver = new RouteSummaryStreamObserver(finishLatch);
        StreamObserver<Point> requestObserver = asyncStub.recordRoute(responseObserver);

        try {
            points.forEach(point -> {
                if (finishLatch.getCount() != 0) {
                    log.info(String.format("Visiting point %f, %f",
                            RouteGuideUtil.getLatitude(point),
                            RouteGuideUtil.getLongitude(point)));
                    requestObserver.onNext(point);

                    // Simulamos que el cliente envía la información poco a poco (comportamiento humano)
                    // Esto, evidentemente, no puede pasar a código productivo.
                    try {
                        Thread.sleep(random.nextInt(1000) + 500L);
                    } catch (InterruptedException e) {
                        log.error("Error sending request: " + e.getMessage());
                        requestObserver.onError(e);
                    }
                }
            });
            requestObserver.onCompleted();

            // Como la recepción es asíncrona, controlamos el timeout con finishLatch
            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                log.warn("recordRoute can not finish within 1 minutes");
            }
        } catch (InterruptedException e) {
            requestObserver.onError(e);
            throw e;
        }
    }

    /**
     * Bidirectional example, which can only be asynchronous. Send some chat messages, and print any
     * chat messages that are sent from the server.
     */
    public CountDownLatch routeChat(List<RouteNote> routeNotes) {
        log.info("*** RouteChat");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<RouteNote> requestObserver =
                asyncStub.routeChat(new StreamObserver<>() {
                    @Override
                    public void onNext(RouteNote note) {
                        log.info(String.format("Got message \"%s\" at %d, %d",
                                note.getMessage(),
                                note.getLocation().getLatitude(),
                                note.getLocation().getLongitude()));
                    }

                    @Override
                    public void onError(Throwable t) {
                        log.warn(String.format("RouteChat Failed: %s", Status.fromThrowable(t)));
                        finishLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        log.info("Finished RouteChat");
                        finishLatch.countDown();
                    }
                });

        try {
            routeNotes.forEach(request -> {
                log.info(String.format("Sending message \"%s\" at %d, %d",
                        request.getMessage(),
                        request.getLocation().getLatitude(),
                        request.getLocation().getLongitude()));
                requestObserver.onNext(request);
            });
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

        // return the latch while receiving happens asynchronously
        return finishLatch;
    }


}
