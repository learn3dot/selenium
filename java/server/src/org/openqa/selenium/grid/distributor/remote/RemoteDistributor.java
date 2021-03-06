// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.grid.distributor.remote;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openqa.selenium.remote.http.HttpMethod.DELETE;
import static org.openqa.selenium.remote.http.HttpMethod.POST;

import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.grid.data.Session;
import org.openqa.selenium.grid.distributor.Distributor;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.web.Values;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.NewSessionPayload;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class RemoteDistributor extends Distributor {

  public static final Json JSON = new Json();
  private final Function<HttpRequest, HttpResponse> client;

  public RemoteDistributor(HttpClient client) {
    Objects.requireNonNull(client);
    this.client = req -> {
      try {
        return client.execute(req);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  @Override
  public Session newSession(NewSessionPayload payload) throws SessionNotCreatedException {
    HttpRequest request = new HttpRequest(POST, "/session");
    StringBuilder builder = new StringBuilder();
    try {
      payload.writeTo(builder);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    request.setContent(builder.toString().getBytes(UTF_8));

    HttpResponse response = client.apply(request);

    return Values.get(response, Session.class);
  }

  @Override
  public void add(Node node) {
    HttpRequest request = new HttpRequest(POST, "/se/grid/distributor/node");
    request.setContent(JSON.toJson(node).getBytes(UTF_8));

    HttpResponse response = client.apply(request);

    Values.get(response, Void.class);
  }

  @Override
  public void remove(UUID nodeId) {

    Objects.requireNonNull(nodeId, "Node ID must be set");
    HttpRequest request = new HttpRequest(DELETE, "/se/grid/distributor/node/" + nodeId);

    HttpResponse response = client.apply(request);

    Values.get(response, Void.class);
  }
}
