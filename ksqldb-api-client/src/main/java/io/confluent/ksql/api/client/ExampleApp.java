package io.confluent.ksql.api.client;

import java.util.concurrent.ExecutionException;

public class ExampleApp {

  // Running this example fails with:
  // something went wrong: java.util.concurrent.ExecutionException: java.lang.NoClassDefFoundError: org/apache/kafka/connect/json/JsonConverter
  public static void main(final String[] args) {
    final ClientOptions options = ClientOptions.create()
        .setHost("localhost")
        .setPort(8088);
    final Client client = Client.create(options);
    try {
      client.streamQuery("select * from test emit changes;").get();
    } catch (ExecutionException | InterruptedException e) {
      System.out.println("something went wrong: " + e);
    }
  }
}
