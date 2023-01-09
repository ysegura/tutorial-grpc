package es.ysegura.grpctutorial.service;

import es.ysegura.grpctutorial.RouteGuideUtil;
import es.ysegura.grpctutorial.protobuff.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Slf4j
public class RouteGuideService extends RouteGuideGrpc.RouteGuideImplBase {
    private final Collection<Feature> features;


    public RouteGuideService(Collection<Feature> features) {
        this.features = features;
    }

    @Override
    public void getFeature(Point request, StreamObserver<Feature> responseObserver) {
        responseObserver.onNext(checkFeature(request));
        responseObserver.onCompleted();
    }

    @Override
    public void listFeatures(Rectangle area, StreamObserver<Feature> responseObserver) {
        int left = getLeft(area);
        int right = getRight(area);
        int top = getTop(area);
        int bottom = getBottom(area);

        features.stream()
                .filter(RouteGuideUtil::exists)
                .filter(feature -> featureWithinLimits(left, right, top, bottom, feature))
                .forEach(responseObserver::onNext);

        responseObserver.onCompleted();
    }

    private static boolean featureWithinLimits(int left, int right, int top, int bottom, Feature feature) {
        int lat = feature.getLocation().getLatitude();
        int lon = feature.getLocation().getLongitude();
        return lon >= left && lon <= right && lat >= bottom && lat <= top;
    }



    private static int getBottom(Rectangle area) {
        return min(area.getLo().getLatitude(), area.getHi().getLatitude());
    }

    private static int getTop(Rectangle area) {
        return max(area.getLo().getLatitude(), area.getHi().getLatitude());
    }

    private static int getRight(Rectangle area) {
        return max(area.getLo().getLongitude(), area.getHi().getLongitude());
    }

    private static int getLeft(Rectangle area) {
        return min(area.getLo().getLongitude(), area.getHi().getLongitude());
    }

    @Override
    public StreamObserver<Point> recordRoute(StreamObserver<RouteSummary> responseObserver) {
        return new PointStreamObserver(responseObserver, features);
    }

    @Override
    public StreamObserver<RouteNote> routeChat(StreamObserver<RouteNote> responseObserver) {
        return new RouteNoteStreamObserver(responseObserver);

    }

    private Feature checkFeature(Point location) {
        return features.stream()
                .filter(feature -> feature.getLocation().getLongitude() == location.getLongitude())
                .filter(feature -> feature.getLocation().getLatitude() == location.getLatitude())
                .findFirst()
                .orElse(Feature.newBuilder().setName("").setLocation(location).build());
    }
}