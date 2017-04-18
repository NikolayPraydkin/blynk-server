package cc.blynk.server.core.dao;

import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.model.web.product.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.04.17.
 */
public class OrganizationDao {

    public static final int DEFAULT_ORGANIZATION_ID = 1;
    private static final Logger log = LogManager.getLogger(OrganizationDao.class);
    public final ConcurrentMap<Integer, Organization> organizations;
    private final AtomicInteger orgSequence;
    private final AtomicInteger productSequence;
    private final FileManager fileManager;

    public OrganizationDao(FileManager fileManager) {
        this.fileManager = fileManager;
        this.organizations = fileManager.deserializeOrganizations();

        int largestOrgSequenceNumber = 0;
        int largestProductSequenceNumber = 0;
        for (Organization organization : organizations.values()) {
            largestOrgSequenceNumber = Math.max(largestOrgSequenceNumber, organization.id);
            for (Product product : organization.products) {
                largestProductSequenceNumber = Math.max(largestProductSequenceNumber, product.id);
            }
        }
        this.orgSequence = new AtomicInteger(largestOrgSequenceNumber);
        this.productSequence = new AtomicInteger(largestProductSequenceNumber);
        log.info("Organization sequence number is {}", largestOrgSequenceNumber);
    }

    public Organization add(Organization organization) {
        organization.id = orgSequence.incrementAndGet();
        organizations.putIfAbsent(organization.id, organization);
        return organization;
    }

    public Product getProduct(int orgId, int productId) {
        Organization org = getOrgById(orgId);
        if (org == null) {
            return null;
        }
        for (Product product : org.products) {
            if (product.id == productId) {
                return product;
            }
        }
        return null;
    }

    public Organization getOrgById(int id) {
        return organizations.get(id);
    }

    public boolean deleteProduct(int orgId, Product product) {
        Organization org = getOrgById(orgId);
        if (org == null) {
            return false;
        }
        if (org.products.remove(product)) {
            org.lastModifiedTs = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public boolean delete(int id) {
        Organization org = organizations.remove(id);
        if (org != null) {
            fileManager.deleteOrg(id);
            return true;
        }
        return false;
    }

    public Product addProduct(int orgId, Product product) {
        Organization organization = getOrgById(orgId);
        if (organization == null) {
            return null;
        }
        product.id = productSequence.incrementAndGet();
        organization.products.add(product);
        organization.lastModifiedTs = System.currentTimeMillis();
        return product;
    }

}
