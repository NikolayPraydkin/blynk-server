package cc.blynk.server.application.handlers.main.logic.graph;

import cc.blynk.server.Holder;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.profile.Profile;
import cc.blynk.server.core.model.widgets.Target;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.outputs.graph.GraphDataStream;
import cc.blynk.server.core.model.widgets.outputs.graph.Period;
import cc.blynk.server.core.model.widgets.outputs.graph.Superchart;
import cc.blynk.server.core.model.widgets.ui.DeviceSelector;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.protocol.exceptions.JsonException;
import cc.blynk.server.core.protocol.exceptions.NoDataException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.WebJsonMessage;
import cc.blynk.server.core.reporting.GraphPinRequest;
import cc.blynk.server.core.session.mobile.MobileStateHolder;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.protocol.enums.Command.GET_SUPERCHART_DATA;
import static cc.blynk.server.core.protocol.enums.Response.SERVER_ERROR;
import static cc.blynk.server.internal.CommonByteBufUtil.makeBinaryMessage;
import static cc.blynk.server.internal.WebByteBufUtil.noData;
import static cc.blynk.utils.ByteUtils.compress;
import static cc.blynk.utils.StringUtils.split2Device;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 2/1/2015.
 *
 */
public final class MobileGetEnhancedGraphDataLogic {

    private static final Logger log = LogManager.getLogger(MobileGetEnhancedGraphDataLogic.class);

    private MobileGetEnhancedGraphDataLogic() {
    }

    public static void messageReceived(Holder holder, ChannelHandlerContext ctx,
                                       MobileStateHolder state, StringMessage message) {
        String[] messageParts = message.body.split(StringUtils.BODY_SEPARATOR_STRING);

        if (messageParts.length < 3) {
            throw new JsonException("Wrong income message format.");
        }

        int targetId = -1;
        String[] dashIdAndTargetIdString = split2Device(messageParts[0]);
        if (dashIdAndTargetIdString.length == 2) {
            targetId = Integer.parseInt(dashIdAndTargetIdString[1]);
        }
        int dashId = Integer.parseInt(dashIdAndTargetIdString[0]);

        long widgetId = Long.parseLong(messageParts[1]);
        Period period = Period.valueOf(messageParts[2]);
        int page = 0;
        if (messageParts.length == 4) {
            page = Integer.parseInt(messageParts[3]);
        }
        int skipCount = period.numberOfPoints * page;

        Profile profile = state.user.profile;
        DashBoard dash = profile.getDashByIdOrThrow(dashId);
        Widget widget = dash.getWidgetById(widgetId);

        //special case for device tiles widget.
        if (widget == null) {
            DeviceTiles deviceTiles = dash.getWidgetByType(DeviceTiles.class);
            if (deviceTiles != null) {
                widget = deviceTiles.getWidgetById(widgetId);
            }
        }

        if (!(widget instanceof Superchart)) {
            throw new JsonException("Passed wrong widget id.");
        }

        Superchart enhancedHistoryGraph = (Superchart) widget;

        int numberOfStreams = enhancedHistoryGraph.dataStreams.length;
        if (numberOfStreams == 0) {
            log.debug("No data streams for enhanced graph with id {}.", widgetId);
            ctx.writeAndFlush(noData(message.id), ctx.voidPromise());
            return;
        }

        GraphPinRequest[] requestedPins = new GraphPinRequest[enhancedHistoryGraph.dataStreams.length];

        int i = 0;
        for (GraphDataStream graphDataStream : enhancedHistoryGraph.dataStreams) {
            //special case, for device tiles widget targetID may be overrided
            Target target;
            int targetIdUpdated = graphDataStream.getTargetId(targetId);
            if (targetIdUpdated < DeviceSelector.DEVICE_SELECTOR_STARTING_ID) {
                target = holder.deviceDao.getById(targetIdUpdated);
            } else {
                target = dash.getDeviceSelector(targetIdUpdated);
            }
            if (target == null) {
                requestedPins[i] = new GraphPinRequest(dashId, -1,
                        graphDataStream.dataStream, period, skipCount, graphDataStream.functionType);
            } else {
                if (target.isTag()) {
                    requestedPins[i] = new GraphPinRequest(dashId, target.getDeviceIds(),
                            graphDataStream.dataStream, period, skipCount, graphDataStream.functionType);
                } else {
                    requestedPins[i] = new GraphPinRequest(dashId, target.getDeviceId(),
                            graphDataStream.dataStream, period, skipCount, graphDataStream.functionType);
                }
            }
            i++;
        }

        readGraphData(holder, ctx.channel(), state.user, requestedPins, message.id);
    }

    private static void readGraphData(Holder holder, Channel channel, User user,
                                      GraphPinRequest[] requestedPins, int msgId) {
        holder.blockingIOProcessor.executeHistory(() -> {
            try {
                byte[][] data = holder.reportingDiskDao.getReportingData(requestedPins);
                byte[] compressed = compress(requestedPins[0].dashId, data);

                if (channel.isWritable()) {
                    channel.writeAndFlush(
                            makeBinaryMessage(GET_SUPERCHART_DATA, msgId, compressed),
                            channel.voidPromise()
                    );
                }
            } catch (NoDataException noDataException) {
                channel.writeAndFlush(noData(msgId), channel.voidPromise());
            } catch (Exception e) {
                log.error("Error reading reporting data. For user {}. Error: {}", user.email, e.getMessage());
                channel.writeAndFlush(new WebJsonMessage(msgId, "Error reading reporting data.", SERVER_ERROR),
                        channel.voidPromise());
            }
        });
    }

}
