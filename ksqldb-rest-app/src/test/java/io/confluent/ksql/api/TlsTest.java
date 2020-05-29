/*
 * Copyright 2020 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.ksql.api;

import static io.confluent.ksql.test.util.AssertEventually.assertThatEventually;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.confluent.ksql.rest.server.KsqlRestConfig;
import io.confluent.ksql.test.util.secure.ServerKeyStore;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlsTest extends ApiTest {

  protected static final Logger log = LoggerFactory.getLogger(TlsTest.class);

  @Override
  protected KsqlRestConfig createServerConfig() {

    String keyStorePath = ServerKeyStore.keyStoreProps()
        .get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG);
    String keyStorePassword = ServerKeyStore.keyStoreProps()
        .get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG);
    String trustStorePath = ServerKeyStore.keyStoreProps()
        .get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
    String trustStorePassword = ServerKeyStore.keyStoreProps()
        .get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG);

    Map<String, Object> config = new HashMap<>();
    config.put(KsqlRestConfig.LISTENERS_CONFIG, "https://localhost:0");
    config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStorePath);
    config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keyStorePassword);
    config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
    config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);
    config.put(KsqlRestConfig.VERTICLE_INSTANCES, 4);

    config.put(KsqlRestConfig.SSL_KEYSTORE_RELOAD_CONFIG, true);

    return new KsqlRestConfig(config);
  }

  @Override
  protected WebClientOptions createClientOptions() {
    return new WebClientOptions().setSsl(true).
        setUseAlpn(true).
        setProtocolVersion(HttpVersion.HTTP_2).
        setTrustAll(true).
        setVerifyHost(false).
        setDefaultHost("localhost").
        setDefaultPort(server.getListeners().get(0).getPort());
  }

  @Test
  public void shouldReloadCert() throws Exception {
    JsonObject requestBody = new JsonObject().put("sql", DEFAULT_PULL_QUERY);
    JsonObject properties = new JsonObject().put("prop1", "val1").put("prop2", 23);
    requestBody.put("properties", properties);

    // execute pull query
    HttpResponse<Buffer> response = sendRequest("/query-stream", requestBody.toBuffer());
    assertThat(response.statusCode(), is(200));
    assertThat(response.statusMessage(), is("OK"));

    // load invalid store
    ServerKeyStore.loadInvalidStore();
    Thread.sleep(10000); // TODO: switch to assert eventually

    // should not execute pull query
    assertThatEventually(() -> {
      try {
        // this should fail
        final HttpResponse<Buffer> response3 = sendRequest("/query-stream", requestBody.toBuffer());
        System.out.println("response code: " + response3.statusCode());
        return false;
      } catch (Exception e) {
        System.out.println("error: " + e);
        return true;
      }
    }, is(true));
  }
}
