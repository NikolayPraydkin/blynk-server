package cc.blynk.server.web.handlers.logic.organization;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.OrganizationDao;
import cc.blynk.server.core.model.dto.OrganizationDTO;
import cc.blynk.server.core.model.permissions.Role;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.web.WebAppStateHolder;
import cc.blynk.server.web.handlers.PermissionBasedLogic;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import static cc.blynk.server.core.model.permissions.PermissionsTable.ORG_VIEW;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 13.04.18.
 */
public class WebGetOrganizationsLogic implements PermissionBasedLogic {

    private final OrganizationDao organizationDao;

    public WebGetOrganizationsLogic(Holder holder) {
        this.organizationDao = holder.organizationDao;
    }

    @Override
    public boolean hasPermission(Role role) {
        return role.canViewOrg();
    }

    @Override
    public int getPermission() {
        return ORG_VIEW;
    }

    @Override
    public void messageReceived0(ChannelHandlerContext ctx, WebAppStateHolder state, StringMessage message) {
        int orgId = Integer.parseInt(message.body);
        List<OrganizationDTO> orgs = organizationDao.getFirstLevelChilds(orgId);

        if (ctx.channel().isWritable()) {
            String orgString = JsonParser.toJson(orgs);
            ctx.writeAndFlush(
                    makeUTF8StringMessage(message.command, message.id, orgString), ctx.voidPromise());
        }
    }

}
