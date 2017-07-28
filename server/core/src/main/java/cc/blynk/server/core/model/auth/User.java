package cc.blynk.server.core.model.auth;

import cc.blynk.server.core.dao.OrganizationDao;
import cc.blynk.server.core.model.AppName;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.Views;
import cc.blynk.server.core.model.web.Role;
import cc.blynk.server.core.processors.NotificationBase;
import cc.blynk.server.core.protocol.exceptions.EnergyLimitException;
import cc.blynk.utils.JsonParser;
import cc.blynk.utils.ParseUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * User: ddumanskiy
 * Date: 8/11/13
 * Time: 4:03 PM
 */
public class User {

    private static final int INITIAL_ENERGY_AMOUNT = ParseUtil.parseInt(System.getProperty("initial.energy", "2000"));

    @JsonView(Views.WebUser.class)
    public volatile String name;

    public volatile String pass;

    @JsonView(Views.WebUser.class)
    public Role role;

    //key fields
    @JsonView(Views.WebUser.class)
	public String email;

    @JsonView(Views.WebUser.class)
    public String appName;

    public String region;

    @JsonView(Views.WebUser.class)
    public int orgId;

    @JsonView(Views.WebUser.class)
    public UserStatus status;

    //used mostly to understand if user profile was changed, all other fields update ignored as it is not so important
    public volatile long lastModifiedTs;

    public String lastLoggedIP;
    public long lastLoggedAt;

    public Profile profile;

    public boolean isFacebookUser;

    public volatile int energy;

    public transient int emailMessages;
    private transient long emailSentTs;

    public User() {
        this.lastModifiedTs = System.currentTimeMillis();
        this.profile = new Profile();
        this.energy = INITIAL_ENERGY_AMOUNT;
        this.isFacebookUser = false;
        this.appName = AppName.BLYNK;
        this.orgId = OrganizationDao.DEFAULT_ORGANIZATION_ID;
    }

    public User(String email, String pass, String appName, String region, boolean isFacebookUser, Role role) {
        this();
        this.email = email;
        this.name = email;
        this.pass = pass;
        this.appName = appName;
        this.region = region;
        this.isFacebookUser = isFacebookUser;
        this.role = role;
    }

    @JsonProperty("id")
    private String id() {
        return email + "-" + appName;
    }

    public boolean hasAccess(int orgId) {
        return isSuperAdmin() || this.orgId == orgId;
    }

    public void subtractEnergy(int price) {
        if (AppName.BLYNK.equals(appName) && price > energy) {
            throw new EnergyLimitException("Not enough energy.");
        }
        //non-atomic. we are fine with that
        this.energy -= price;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public void recycleEnergy(int price) {
        purchaseEnergy(price);
    }

    public void purchaseEnergy(int price) {
        //non-atomic. we are fine with that
        this.energy += price;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    private static final int EMAIL_DAY_LIMIT = 100;
    private static final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000;

    public void checkDailyEmailLimit() {
        long now = System.currentTimeMillis();
        if (now - emailSentTs < MILLIS_IN_DAY) {
            if (emailMessages > EMAIL_DAY_LIMIT) {
                throw NotificationBase.EXCEPTION_CACHE;
            }
        } else {
            this.emailMessages = 0;
            this.emailSentTs = now;
        }
    }

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }

    public void setName(String name) {
        this.name = name;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public void setRole(Role role) {
        this.role = role;
        this.lastModifiedTs = System.currentTimeMillis();
    }

    public boolean isUpdated(long lastStart) {
        return (lastStart <= lastModifiedTs) || isDashUpdated(lastStart);
    }

    private boolean isDashUpdated(long lastStart) {
        for (DashBoard dashBoard : profile.dashBoards) {
            if (lastStart <= dashBoard.updatedAt) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;

        User user = (User) o;

        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        return !(appName != null ? !appName.equals(user.appName) : user.appName != null);

    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }

}
