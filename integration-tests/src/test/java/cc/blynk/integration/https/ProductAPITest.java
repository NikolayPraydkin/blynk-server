package cc.blynk.integration.https;

import cc.blynk.server.core.model.device.ConnectionType;
import cc.blynk.server.core.model.web.Role;
import cc.blynk.server.core.model.web.product.DataStream;
import cc.blynk.server.core.model.web.product.Product;
import cc.blynk.server.core.model.web.product.metafields.*;
import cc.blynk.utils.JsonParser;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Currency;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 24.12.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProductAPITest extends APIBaseTest {

    @Test
    public void getProductsNotAuthorized() throws Exception {
        HttpGet getOwnProfile = new HttpGet(httpsAdminServerUrl + "/product");
        try (CloseableHttpResponse response = httpclient.execute(getOwnProfile)) {
            assertEquals(401, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getNonExistingProduct() throws Exception {
        login(admin.email, admin.pass);

        HttpGet product = new HttpGet(httpsAdminServerUrl + "/product/123");
        try (CloseableHttpResponse response = httpclient.execute(product)) {
            assertEquals(400, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void getProductById() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(product.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }

        HttpGet product1 = new HttpGet(httpsAdminServerUrl + "/product/1");
        try (CloseableHttpResponse response = httpclient.execute(product1)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }
    }


    @Test
    public void createProduct() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;
        product.logoUrl = "/static/logo.png";

        product.metaFields = new MetaField[] {
                new TextMetaField("My Farm", Role.ADMIN, "Farm of Smith"),
                new RangeMetaField("Farm of Smith", Role.ADMIN, 60, 120),
                new NumberMetaField("Farm of Smith", Role.ADMIN, 10.222),
                new MeasurementUnitMetaField("Farm of Smith", Role.ADMIN, MeasurementUnit.Celsius, "36"),
                new CostMetaField("Farm of Smith", Role.ADMIN, Currency.getInstance("USD"), 9.99, 1, MeasurementUnit.Gallon),
                new ContactMetaField("Farm of Smith", Role.ADMIN, "Tech Support",
                        "Dmitriy", "Dumanskiy", "dmitriy@blynk.cc", "+38063673333", "My street",
                        "Kyiv", "Ukraine", "03322"),
                new AddressMetaField("Farm of Smith", Role.ADMIN, "My street",
                        "San Diego", "CA", "03322", "US"),
                new CoordinatesMetaField("Farm Location", Role.ADMIN, 22.222, 23.333),
                new TimeMetaField("Some Time", Role.ADMIN, new Date())
        };

        product.dataStreams = new DataStream[] {
                new DataStream("Temperature", MeasurementUnit.Celsius, 0, 50, (byte) 0)
        };

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(product.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals(product.name, fromApi.name);
            assertEquals(product.description, fromApi.description);
            assertEquals(product.boardType, fromApi.boardType);
            assertEquals(product.connectionType, fromApi.connectionType);
            assertEquals(product.logoUrl, fromApi.logoUrl);
            assertEquals(0, fromApi.version);
            assertNotEquals(0, fromApi.updatedAt);
            assertNotNull(fromApi.dataStreams);
            assertNotNull(fromApi.metaFields);
            assertEquals(9, fromApi.metaFields.length);
        }
    }

    @Test
    public void updateProduct() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(product.toString(), ContentType.APPLICATION_JSON));

        Product fromApi;
        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals(product.name, fromApi.name);
            assertEquals(product.description, fromApi.description);
            assertEquals(product.boardType, fromApi.boardType);
            assertEquals(product.connectionType, fromApi.connectionType);
            assertEquals(0, fromApi.version);
        }

        product.id = 1;
        product.name = "Updated Name";

        HttpPost updateReq = new HttpPost(httpsAdminServerUrl + "/product");
        updateReq.setEntity(new StringEntity(product.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(updateReq)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
            assertEquals("Updated Name", fromApi.name);
            assertEquals(product.description, fromApi.description);
            assertEquals(product.boardType, fromApi.boardType);
            assertEquals(product.connectionType, fromApi.connectionType);
            assertEquals(1, fromApi.version);
        }
    }

    @Test
    public void getEmptyListOfProducts() throws Exception {
        login(admin.email, admin.pass);

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/product");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product[] fromApi = JsonParser.readAny(consumeText(response), Product[].class);
            assertNotNull(fromApi);
            assertEquals(0, fromApi.length);
        }
    }

    @Test
    public void getListOfProducts() throws Exception {
        createProduct();

        HttpGet req = new HttpGet(httpsAdminServerUrl + "/product");

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product[] fromApi = JsonParser.readAny(consumeText(response), Product[].class);
            assertNotNull(fromApi);
            assertEquals(1, fromApi.length);
        }
    }

    @Test
    public void getListOfProducts2() throws Exception {
        createProduct();

        Product product2 = new Product();
        product2.name = "My product2";
        product2.description = "Description2";
        product2.boardType = "ESP82662";
        product2.connectionType = ConnectionType.WI_FI;

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(product2.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(2, fromApi.id);
        }

        HttpGet getList = new HttpGet(httpsAdminServerUrl + "/product");

        try (CloseableHttpResponse response = httpclient.execute(getList)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product[] fromApi = JsonParser.readAny(consumeText(response), Product[].class);
            assertNotNull(fromApi);
            assertEquals(2, fromApi.length);
        }
    }

    @Test
    public void createProductAndDelete() throws Exception {
        login(admin.email, admin.pass);

        Product product = new Product();
        product.name = "My product";
        product.description = "Description";
        product.boardType = "ESP8266";
        product.connectionType = ConnectionType.WI_FI;

        HttpPut req = new HttpPut(httpsAdminServerUrl + "/product");
        req.setEntity(new StringEntity(product.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpclient.execute(req)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product fromApi = JsonParser.parseProduct(consumeText(response));
            assertNotNull(fromApi);
            assertEquals(1, fromApi.id);
        }

        HttpGet req2 = new HttpGet(httpsAdminServerUrl + "/product");

        try (CloseableHttpResponse response = httpclient.execute(req2)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product[] fromApi = JsonParser.readAny(consumeText(response), Product[].class);
            assertNotNull(fromApi);
            assertEquals(1, fromApi.length);
        }

        HttpDelete req3 = new HttpDelete(httpsAdminServerUrl + "/product/1");

        try (CloseableHttpResponse response = httpclient.execute(req3)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
        }

        HttpGet req4 = new HttpGet(httpsAdminServerUrl + "/product");

        try (CloseableHttpResponse response = httpclient.execute(req4)) {
            assertEquals(200, response.getStatusLine().getStatusCode());
            Product[] fromApi = JsonParser.readAny(consumeText(response), Product[].class);
            assertNotNull(fromApi);
            assertEquals(0, fromApi.length);
        }
    }

}
