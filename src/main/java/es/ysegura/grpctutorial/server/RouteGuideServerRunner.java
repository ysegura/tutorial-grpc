package es.ysegura.grpctutorial.server;

import es.ysegura.grpctutorial.RouteGuideUtil;
import es.ysegura.grpctutorial.protobuff.Feature;
import es.ysegura.grpctutorial.service.RouteGuideService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RouteGuideServerRunner {

    private final int port;
    private final Server server;

    public RouteGuideServerRunner(int port) throws IOException {
        this(port, RouteGuideUtil.getDefaultFeaturesFile());
    }

    public RouteGuideServerRunner(int port, URL featuresFile) throws IOException{
        this(Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create()), port, RouteGuideUtil.parseFeatures(featuresFile));
    }

    public RouteGuideServerRunner(ServerBuilder<?> serverBuilder, int port, Collection<Feature> features){
        this.port = port;
        server = serverBuilder
                .addService(new RouteGuideService(features))
                .build();
    }

    public void start() throws IOException{
        server.start();
        log.info("Server started, listening on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.error("*** Shutting down gRPC server since JVM is shutting down");
            try {
                RouteGuideServerRunner.this.stop();
            } catch (InterruptedException exception) {
                exception.printStackTrace(System.err);
            }
            log.error("*** gRPC server shut down");
        }));
    }
    public void stop() throws InterruptedException{
        if (server != null){
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
    public void blockUntilShutdown() throws InterruptedException{
        if (server != null){
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception{
        RouteGuideServerRunner server = new RouteGuideServerRunner(8980);
        server.start();
        server.blockUntilShutdown();
    }
}
