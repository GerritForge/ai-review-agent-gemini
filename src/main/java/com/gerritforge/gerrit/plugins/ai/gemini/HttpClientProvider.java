// Copyright (C) 2026 GerritForge, Inc.
//
// Licensed under the BSL 1.1 (the "License");
// you may not use this file except in compliance with the License.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.plugins.ai.gemini;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

@Singleton
class HttpClientProvider implements Provider<CloseableHttpClient>, LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private CloseableHttpClient httpClient;

  @Inject
  HttpClientProvider() {
    this.httpClient = HttpClients.createDefault();
  }

  @Override
  public CloseableHttpClient get() {
    return httpClient;
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    try {
      httpClient.close();
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed to close HTTP client");
    }
  }
}
