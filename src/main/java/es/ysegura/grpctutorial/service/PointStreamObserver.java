package es.ysegura.grpctutorial.service;

import es.ysegura.grpctutorial.RouteGuideUtil;
import es.ysegura.grpctutorial.protobuff.Feature;
import es.ysegura.grpctutorial.protobuff.Point;
import es.ysegura.grpctutorial.protobuff.RouteSummary;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

@Slf4j
public class PointStreamObserver implements StreamObserver<Point> {

    private int pointCount;
    private int featureCount;
    private int distance;
    private Point previous;
    private final long startTime = System.nanoTime();

    private final StreamObserver<RouteSummary> responseObserver;

    private final Collection<Feature> features;
    public PointStreamObserver(StreamObserver<RouteSummary> responseObserver, Collection<Feature> features) {
        this.responseObserver = responseObserver;
        this.features = features;
    }

    @Override
    public void onNext(Point point) {
        pointCount++;
        if (RouteGuideUtil.exists(checkFeature(point))) {
            featureCount++;
        }
        if (previous != null) {
            distance += RouteGuideUtil.calcDistance(previous, point);
        }
        previous = point;
    }

    @Override
    public void onError(Throwable throwable) {
        log.warn("recordRoute cancelled");
    }

    @Override
    public void onCompleted() {
        long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
        responseObserver.onNext(
                RouteSummary.newBuilder()
                        .setPointCount(pointCount)
                        .setFeatureCount(featureCount)
                        .setDistance(distance)
                        .setElapsedTime((int) seconds)
                        .build()
        );
        responseObserver.onCompleted();
    }

    private Feature checkFeature(Point location){
        return features.stream()
                .filter(feature -> feature.getLocation().getLongitude() == location.getLongitude())
                .filter(feature -> feature.getLocation().getLatitude() == location.getLatitude())
                .findFirst()
                .orElse(Feature.newBuilder().setName("").setLocation(location).build());
    }
}
