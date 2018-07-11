package cc.blynk.server.servers.application;

import cc.blynk.core.http.handlers.NoMatchHandler;
import cc.blynk.core.http.handlers.StaticFile;
import cc.blynk.core.http.handlers.StaticFileEdsWith;
import cc.blynk.core.http.handlers.StaticFileHandler;
import cc.blynk.core.http.handlers.UploadHandler;
import cc.blynk.core.http.handlers.url.UrlMapper;
import cc.blynk.core.http.handlers.url.UrlReWriterHandler;
import cc.blynk.core.http.utils.UrlStartWithMapper;
import cc.blynk.server.Holder;
import cc.blynk.server.api.http.dashboard.AccountHandler;
import cc.blynk.server.api.http.dashboard.AuthCookieHandler;
import cc.blynk.server.api.http.dashboard.DataHandler;
import cc.blynk.server.api.http.dashboard.DevicesHandler;
import cc.blynk.server.api.http.dashboard.ExternalAPIHandler;
import cc.blynk.server.api.http.dashboard.OTAHandler;
import cc.blynk.server.api.http.dashboard.OrganizationHandler;
import cc.blynk.server.api.http.dashboard.ProductHandler;
import cc.blynk.server.api.http.dashboard.WebLoginHandler;
import cc.blynk.server.api.http.handlers.BaseHttpAndBlynkUnificationHandler;
import cc.blynk.server.api.http.handlers.BaseWebSocketUnificator;
import cc.blynk.server.api.websockets.handlers.WebSocketHandler;
import cc.blynk.server.api.websockets.handlers.WebSocketWrapperEncoder;
import cc.blynk.server.application.handlers.main.AppChannelStateHandler;
import cc.blynk.server.application.handlers.main.ResetPasswordHandler;
import cc.blynk.server.application.handlers.main.auth.AppLoginHandler;
import cc.blynk.server.application.handlers.main.auth.GetServerHandler;
import cc.blynk.server.application.handlers.main.auth.RegisterHandler;
import cc.blynk.server.application.handlers.sharing.auth.AppShareLoginHandler;
import cc.blynk.server.common.handlers.AlreadyLoggedHandler;
import cc.blynk.server.common.handlers.UserNotLoggedHandler;
import cc.blynk.server.core.protocol.handlers.decoders.AppMessageDecoder;
import cc.blynk.server.core.protocol.handlers.decoders.MessageDecoder;
import cc.blynk.server.core.protocol.handlers.decoders.WebAppMessageDecoder;
import cc.blynk.server.core.protocol.handlers.encoders.AppMessageEncoder;
import cc.blynk.server.core.protocol.handlers.encoders.MessageEncoder;
import cc.blynk.server.core.protocol.handlers.encoders.WebAppMessageEncoder;
import cc.blynk.server.hardware.handlers.hardware.HardwareChannelStateHandler;
import cc.blynk.server.hardware.handlers.hardware.auth.HardwareLoginHandler;
import cc.blynk.server.servers.BaseServer;
import cc.blynk.server.web.handlers.auth.WebAppLoginHandler;
import cc.blynk.utils.FileUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import static cc.blynk.utils.StringUtils.WEBSOCKET_PATH;
import static cc.blynk.utils.StringUtils.WEBSOCKET_WEB_PATH;
import static cc.blynk.utils.properties.ServerProperties.STATIC_FILES_FOLDER;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 1/12/2015.
 */
public class AppAndHttpsServer extends BaseServer {

    private final ChannelInitializer<SocketChannel> channelInitializer;

    public AppAndHttpsServer(Holder holder) {
        super(holder.props.getProperty("listen.address"),
                holder.props.getIntProperty("https.port"), holder.transportTypeHolder);

        var appChannelStateHandler = new AppChannelStateHandler(holder.sessionDao);
        var registerHandler = new RegisterHandler(holder);
        var appLoginHandler = new AppLoginHandler(holder);
        var appShareLoginHandler = new AppShareLoginHandler(holder);
        var userNotLoggedHandler = new UserNotLoggedHandler();
        var getServerHandler = new GetServerHandler(holder);
        var resetPasswordHandler = new ResetPasswordHandler(holder);

        var hardwareIdleTimeout = holder.limits.hardwareIdleTimeout;
        var appIdleTimeout = holder.limits.appIdleTimeout;

        var hardwareChannelStateHandler = new HardwareChannelStateHandler(holder);
        var hardwareLoginHandler = new HardwareLoginHandler(holder, port);

        var apiPath = holder.props.getApiPath();

        var stats = holder.stats;

        //http API handlers
        var noMatchHandler = new NoMatchHandler();
        var webSocketHandler = new WebSocketHandler(stats);
        var webSocketWrapperEncoder = new WebSocketWrapperEncoder();

        var webAppMessageEncoder = new WebAppMessageEncoder();
        var webAppLoginHandler = new WebAppLoginHandler(holder);

        var externalAPIHandler = new ExternalAPIHandler(holder, "/external/api");
        var urlReWriterHandler = new UrlReWriterHandler(
                new UrlMapper("/favicon.ico", "/static/favicon.ico"),
                new UrlMapper("/", "/static/index.html"),
                new UrlStartWithMapper("/dashboard", "/static/index.html")
        );

        var jarPath = holder.props.jarPath;

        var webLoginHandler = new WebLoginHandler(holder, apiPath);
        var authCookieHandler = new AuthCookieHandler(holder.sessionDao);
        var accountHandler = new AccountHandler(holder, apiPath);
        var devicesHandler = new DevicesHandler(holder, apiPath);
        var dataHandler = new DataHandler(holder, apiPath);
        var productHandler = new ProductHandler(holder, apiPath);
        var organizationHandler = new OrganizationHandler(holder, apiPath);

        var alreadyLoggedHandler = new AlreadyLoggedHandler();
        var otaHandler = new OTAHandler(holder, apiPath);

        var baseWebSocketUnificator = new BaseWebSocketUnificator() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                var req = (FullHttpRequest) msg;
                var uri = req.uri();

                log.trace("In https and websocket unificator handler.");
                if (uri.startsWith(WEBSOCKET_PATH)) {
                    initWebSocketPipeline(ctx, WEBSOCKET_PATH);
                } else if (uri.equals(WEBSOCKET_WEB_PATH)) {
                    initWebDashboardSocket(ctx);
                } else {
                    initHttpPipeline(ctx);
                }

                ctx.fireChannelRead(msg);
            }

            private void initHttpPipeline(ChannelHandlerContext ctx) {
                ctx.pipeline()
                        .addLast(webLoginHandler)
                        .addLast(authCookieHandler)
                        .addLast(new UploadHandler(jarPath, "/api/upload", "/" + STATIC_FILES_FOLDER))
                        .addLast(accountHandler)
                        .addLast(devicesHandler)
                        .addLast(dataHandler)
                        .addLast(productHandler)
                        .addLast(organizationHandler)
                        .addLast(otaHandler)
                        .addLast(noMatchHandler)
                        .remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized https pipeline. {}", ctx.pipeline().names());
                }
            }

            private void initWebDashboardSocket(ChannelHandlerContext ctx) {
                var pipeline = ctx.pipeline();

                //websockets specific handlers
                pipeline.addFirst("WebChannelState", appChannelStateHandler)
                        .addFirst("WebReadTimeout", new IdleStateHandler(appIdleTimeout, 0, 0))
                        .addLast("WebWebSocketServerProtocolHandler",
                        new WebSocketServerProtocolHandler(WEBSOCKET_WEB_PATH))
                        .addLast("WebMessageDecoder", new WebAppMessageDecoder(stats, holder.limits))
                        .addLast("WebMessageEncoder", webAppMessageEncoder)
                        .addLast("WebGetServer", getServerHandler)
                        .addLast("WebRegister", registerHandler)
                        .addLast("WebLogin", webAppLoginHandler)
                        .addLast("WebNotLogged", userNotLoggedHandler);
                pipeline.remove(ChunkedWriteHandler.class);
                pipeline.remove(UrlReWriterHandler.class);
                pipeline.remove(StaticFileHandler.class);
                pipeline.remove(HttpObjectAggregator.class);
                pipeline.remove(HttpServerKeepAliveHandler.class);
                pipeline.remove(ExternalAPIHandler.class);
                pipeline.remove(HttpContentCompressor.class);
                pipeline.remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized web dashboard pipeline. {}", ctx.pipeline().names());
                }
            }

            private void initWebSocketPipeline(ChannelHandlerContext ctx, String websocketPath) {
                var pipeline = ctx.pipeline();

                //websockets specific handlers
                pipeline.addFirst("WSIdleStateHandler", new IdleStateHandler(hardwareIdleTimeout, 0, 0))
                        .addLast("WSChannelState", hardwareChannelStateHandler)
                        .addLast("WSWebSocketServerProtocolHandler",
                        new WebSocketServerProtocolHandler(websocketPath, true))
                        .addLast("WSWebSocket", webSocketHandler)
                        .addLast("WSMessageDecoder", new MessageDecoder(stats, holder.limits))
                        .addLast("WSSocketWrapper", webSocketWrapperEncoder)
                        .addLast("WSMessageEncoder", new MessageEncoder(stats))
                        .addLast("WSLogin", hardwareLoginHandler)
                        .addLast("WSNotLogged", alreadyLoggedHandler);
                pipeline.remove(ChunkedWriteHandler.class);
                pipeline.remove(UrlReWriterHandler.class);
                pipeline.remove(StaticFileHandler.class);
                pipeline.remove(HttpObjectAggregator.class);
                pipeline.remove(HttpServerKeepAliveHandler.class);
                pipeline.remove(ExternalAPIHandler.class);
                pipeline.remove(HttpContentCompressor.class);
                pipeline.remove(this);
                if (log.isTraceEnabled()) {
                    log.trace("Initialized secured hardware websocket pipeline. {}", ctx.pipeline().names());
                }
            }
        };

        channelInitializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline()
                .addLast(holder.sslContextHolder.sslCtx.newHandler(ch.alloc()))
                .addLast(new BaseHttpAndBlynkUnificationHandler() {
                    @Override
                    public ChannelPipeline buildHttpPipeline(ChannelPipeline pipeline) {
                        log.trace("HTTPS connection detected.", pipeline.channel());
                        return pipeline
                                .addLast("HttpsServerCodec", new HttpServerCodec())
                                .addLast("HttpsServerKeepAlive", new HttpServerKeepAliveHandler())
                                .addLast("HttpCompressor", new HttpContentCompressor())
                                .addLast("HttpsObjectAggregator",
                                        new HttpObjectAggregator(holder.limits.webRequestMaxSize, true))
                                .addLast("HttpChunkedWrite", new ChunkedWriteHandler())
                                .addLast("HttpUrlMapper", urlReWriterHandler)
                                .addLast("HttpStaticFile",
                                        new StaticFileHandler(holder, new StaticFile("/static"),
                                                new StaticFileEdsWith(FileUtils.CSV_DIR, ".gz"),
                                                new StaticFileEdsWith(FileUtils.CSV_DIR, ".zip")))
                                .addLast(externalAPIHandler)
                                .addLast("HttpsWebSocketUnificator", baseWebSocketUnificator);
                    }

                    @Override
                    public ChannelPipeline buildBlynkPipeline(ChannelPipeline pipeline) {
                        log.trace("Blynk protocol connection detected.", pipeline.channel());
                        return pipeline
                                .addFirst("AChannelState", appChannelStateHandler)
                                .addFirst("AReadTimeout", new IdleStateHandler(appIdleTimeout, 0, 0))
                                .addLast("AMessageDecoder", new AppMessageDecoder(holder.stats, holder.limits))
                                .addLast("AMessageEncoder", new AppMessageEncoder(holder.stats))
                                .addLast("AGetServer", getServerHandler)
                                .addLast("ARegister", registerHandler)
                                .addLast("ALogin", appLoginHandler)
                                .addLast("AResetPass", resetPasswordHandler)
                                .addLast("AShareLogin", appShareLoginHandler)
                                .addLast("ANotLogged", userNotLoggedHandler);
                    }
                });
            }
        };
    }

    @Override
    public ChannelInitializer<SocketChannel> getChannelInitializer() {
        return channelInitializer;
    }

    @Override
    protected String getServerName() {
        return "HTTPS API, WebSockets and Admin page";
    }

    @Override
    public ChannelFuture close() {
        System.out.println("Shutting down HTTPS API, WebSockets and Admin server...");
        return super.close();
    }

}
