package es.ysegura.grpctutorial.service;

import es.ysegura.grpctutorial.protobuff.Point;
import es.ysegura.grpctutorial.protobuff.RouteNote;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class RouteNoteStreamObserver implements StreamObserver<RouteNote> {

    private final ConcurrentMap<Point, List<RouteNote>> routeNotes;
    private final StreamObserver<RouteNote> responseObserver;

    public RouteNoteStreamObserver(StreamObserver<RouteNote> responseObserver) {
        this.responseObserver = responseObserver;
        this.routeNotes = new ConcurrentHashMap<>();
    }

    @Override
    public void onNext(RouteNote note) {
        List<RouteNote> notes = getOrCreateNotes(note.getLocation());
        // Respond with all previous notes at this location.
        routeNotes.forEach((k, v) -> v.forEach(responseObserver::onNext));
        notes.add(note);
    }

    @Override
    public void onError(Throwable throwable) {
        log.warn("routeChat cancelled due to " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        routeNotes.values().stream()
                .flatMap(Collection::stream)
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    private List<RouteNote> getOrCreateNotes(Point location){
        List<RouteNote> notes = Collections.synchronizedList(new ArrayList<>());
        List<RouteNote> prevNotes = routeNotes.putIfAbsent(location, notes);
        return prevNotes != null ? prevNotes : notes;
    }
}
