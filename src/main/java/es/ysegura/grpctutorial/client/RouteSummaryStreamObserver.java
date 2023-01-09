package es.ysegura.grpctutorial.client;

import es.ysegura.grpctutorial.protobuff.RouteSummary;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
public class RouteSummaryStreamObserver implements StreamObserver<RouteSummary> {

    private final CountDownLatch finishLatch;

    public RouteSummaryStreamObserver(CountDownLatch finishLatch) {
        this.finishLatch = finishLatch;
    }

    @Override
    public void onNext(RouteSummary routeSummary) {
        log.info(String.format("Finished trip with %d points. Passed %d features. Travelled %d meters. It took %d seconds.",
                routeSummary.getPointCount(),
                routeSummary.getFeatureCount(),
                routeSummary.getDistance(),
                routeSummary.getElapsedTime()));
    }

    @Override
    public void onError(Throwable t) {
        log.warn(String.format("RecordRoute Failed: %s", Status.fromThrowable(t)));
        finishLatch.countDown();
    }

    @Override
    public void onCompleted() {
        log.info("Finished RecordRoute");
        finishLatch.countDown();
    }
}
