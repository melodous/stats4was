package cosm0s.stats4was.sender.graphite;

import cosm0s.stats4was.domain.Statistic;
import cosm0s.stats4was.log.L4j;
import cosm0s.stats4was.sender.Sender;
import cosm0s.stats4was.utils.Constants;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

public class GraphiteSender implements Sender {

    private static boolean isShuttingDown;
    private Properties graphiteProperties;
    private static String DEFAULT_CONF = "Properties not found will be initialized by default";
    private static String GRAPHITE_INIT = "Graphite sender Background starting up";
    private static String SET_PORT = "The format of the port is incorrect, it sets the default 2003";
    private static String SET_RECONNECT_TIMEOUT = "The format of the timeout is incorrect, it sets the default 60";
    private static String SET_BUFFER_SIZE = "The format of the buffer size is incorrect, it sets the default 1048576";
    private static String DISABLE = "Statistics Retriever Background Service has been disabled. Reason: ";
    private static String SHUTDOWN = "Statistics Retriever Background Service shutting down";
    private static String CLOSE_CHANNEL = "Close channel";
    private static String RELEASE_FACTORY = "Release factory resources";
    private static String RELEASE_CLIENT = "Release client resources";
    private static String ERROR_CHANNEL = "Error on channel retrieval: ";
    private InetSocketAddress socketAddress;
    private ChannelFactory channelFactory;
    private ClientBootstrap clientBootstrap;
    private Pipeline pipeline;
    private ChannelFuture channelFuture;
    private Map<String,Integer> count;

    public GraphiteSender(){
        boolean haveProperties = true;
        this.graphiteProperties = new Properties();
        try {
            this.graphiteProperties.load(new FileInputStream(Constants.PropertiePath + "graphiteSender.properties"));
        } catch (IOException e) {
            haveProperties = false;
        }
        if(!haveProperties || this.graphiteProperties.isEmpty()) {
            this.setDefaultProperties();
        }
        count = new HashMap<String,Integer>();
    }

    private void setDefaultProperties(){
        L4j.getL4j().info(DEFAULT_CONF);
        this.graphiteProperties.setProperty("carbonHost", "localhost");
        this.graphiteProperties.setProperty("port", "2045");
        this.graphiteProperties.setProperty("reconnectTimeout","60");
        this.graphiteProperties.setProperty("sendBufferSize", "1048576");
        this.graphiteProperties.setProperty("hostPrefix", "pro.bbdd");
        this.graphiteProperties.setProperty("metricUseHost", "true");
        this.graphiteProperties.setProperty("hostSuffix", "wls");
    }

    @Override
    public void init(){
        L4j.getL4j().info(GRAPHITE_INIT);
        int port, reconnectTimeout, sendBufferSize;
        try {
            port = Integer.parseInt(this.graphiteProperties.getProperty("port"));
        } catch(NumberFormatException e){
            L4j.getL4j().warning(SET_PORT);
            port = 2003;
        }
        try {
            reconnectTimeout = Integer.parseInt(this.graphiteProperties.getProperty("reconnectTimeout"));
        } catch(NumberFormatException e){
            L4j.getL4j().warning(SET_RECONNECT_TIMEOUT);
            reconnectTimeout = 60;
        }
        try {
            sendBufferSize = Integer.parseInt(this.graphiteProperties.getProperty("sendBufferSize"));
        } catch(NumberFormatException e){
            L4j.getL4j().warning(SET_BUFFER_SIZE);
            sendBufferSize = 1048576;
        }
        try {
            this.socketAddress = new InetSocketAddress(graphiteProperties.getProperty("carbonHost"), port);
            this.channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
            this.clientBootstrap = new ClientBootstrap(channelFactory);
            this.pipeline = new Pipeline(this.clientBootstrap, new HashedWheelTimer(), reconnectTimeout);
            this.clientBootstrap.setPipelineFactory(this.pipeline);
            this.clientBootstrap.setOption("tcpNoDelay", true);
            this.clientBootstrap.setOption("keepAlive", true);
            this.clientBootstrap.setOption("remoteAddress", this.socketAddress);
            this.clientBootstrap.setOption("sendBufferSize", sendBufferSize);
            this.channelFuture = this.clientBootstrap.connect(this.socketAddress);
        } catch (Exception e) {
            L4j.getL4j().critical(new StringBuilder(DISABLE).append(e.toString()).toString());
        }

    }

    public void shutdown() {
        L4j.getL4j().info(SHUTDOWN);
        try {
            this.isShuttingDown = true;
            Channel channel = this.pipeline.getCurrentPipeline().getChannel();
            this.channelFuture.getChannel().write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            L4j.getL4j().info(CLOSE_CHANNEL);
            channelFactory.releaseExternalResources();
            L4j.getL4j().info(RELEASE_FACTORY);
            clientBootstrap.releaseExternalResources();
            L4j.getL4j().info(RELEASE_CLIENT);

        } catch (Exception e) {
            L4j.getL4j().error(new StringBuilder(ERROR_CHANNEL).append(e.toString()).toString(), e);
        }

    }

    @Override
    public boolean isConnected() {
        boolean isConnected = false;
        try {
            isConnected = this.pipeline.getCurrentPipeline().getChannel().isConnected();
        } catch (Exception e) {
            L4j.getL4j().error("isConnected", e);
        }
        return isConnected;
    }

    @Override
    public void send(Statistic statistic) {
        try {
            Channel channel = pipeline.getCurrentPipeline().getChannel();
            channel.write(statistic.standarMetric() + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isShuttingDown() {
        return isShuttingDown;
    }
}