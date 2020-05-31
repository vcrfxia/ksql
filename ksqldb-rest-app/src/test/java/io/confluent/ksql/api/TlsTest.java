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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import io.confluent.ksql.rest.server.KsqlRestConfig;
import io.confluent.ksql.test.util.secure.ServerKeyStore;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClientOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlsTest extends ApiTest {

  protected static final Logger log = LoggerFactory.getLogger(TlsTest.class);

  private static final String KEY_STORE_PATH = ServerKeyStore.keyStoreProps()
      .get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG);
  private static final String KEY_STORE_PASSWORD = ServerKeyStore.keyStoreProps()
      .get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG);
  private static final String TRUST_STORE_PATH = ServerKeyStore.keyStoreProps()
      .get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
  private static final String TRUST_STORE_PASSWORD = ServerKeyStore.keyStoreProps()
      .get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG);

  @Override
  protected KsqlRestConfig createServerConfig() {

    Map<String, Object> config = new HashMap<>();
    config.put(KsqlRestConfig.LISTENERS_CONFIG, "https://localhost:0");
    config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, KEY_STORE_PATH);
    config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, KEY_STORE_PASSWORD);
    config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, TRUST_STORE_PATH);
    config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, TRUST_STORE_PASSWORD);
    config.put(KsqlRestConfig.VERTICLE_INSTANCES, 4);

    config.put(KsqlRestConfig.SSL_KEYSTORE_RELOAD_CONFIG, true);

    return new KsqlRestConfig(config);
  }

  @Override
  protected WebClientOptions createClientOptions() {
    return new WebClientOptions().setSsl(true).
        setUseAlpn(true).
        setProtocolVersion(HttpVersion.HTTP_2).
        setTrustStoreOptions(
            new JksOptions().setPath(TRUST_STORE_PATH).setPassword(TRUST_STORE_PASSWORD)).
        setVerifyHost(false).
        setDefaultHost("localhost").
        setDefaultPort(server.getListeners().get(0).getPort());
  }

  @Test
  public void shouldReloadCert() throws Exception {
    JsonObject requestBody = new JsonObject().put("sql", DEFAULT_PULL_QUERY);

    // Given: sanity check that a query succeeds
    HttpResponse<Buffer> response = sendRequest("/query-stream", requestBody.toBuffer());
    assertThat(response.statusCode(), is(200));
    assertThat(response.statusMessage(), is("OK"));

    // When: load expired key store
    ServerKeyStore.loadExpiredStore();
    assertThatEventually(
        "Should fail to execute query with expired key store",
        () -> {
          try {
            // this should fail
            sendRequest("/query-stream", requestBody.toBuffer());
            return false;
          } catch (Exception e) {
            assertThat(e, instanceOf(ExecutionException.class)); // thrown from CompletableFuture.get()
            assertThat(e.getMessage(),
                containsString("javax.net.ssl.SSLHandshakeException: Failed to create SSL connection"));
            return true;
          }
        },
        is(true),
        TimeUnit.SECONDS.toMillis(3),
        TimeUnit.SECONDS.toMillis(3)
    );

    // When: load valid store
    ServerKeyStore.loadValidStore();
    assertThatEventually(
        "Should successfully execute query with valid key store",
        () -> {
          try {
            return sendRequest("/query-stream", requestBody.toBuffer()).statusCode();
          } catch (Exception e) {
            return 0;
          }
        },
        is(200),
        TimeUnit.SECONDS.toMillis(3),
        TimeUnit.SECONDS.toMillis(3)
    );
  }
}
