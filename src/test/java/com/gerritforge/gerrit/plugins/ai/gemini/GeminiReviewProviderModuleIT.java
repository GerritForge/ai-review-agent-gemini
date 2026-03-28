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

import static com.google.common.truth.Truth.assertThat;

import com.gerritforge.gerrit.plugins.ai.provider.api.AiReviewProvider;
import com.gerritforge.gerrit.plugins.ai.provider.api.AiReviewProviderApiModule;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.Test;

@TestPlugin(
    name = "ai-review-agent-provider",
    sysModule = "com.gerritforge.gerrit.plugins.ai.gemini.GeminiReviewProviderModuleIT$TestModule")
public class GeminiReviewProviderModuleIT extends LightweightPluginDaemonTest {

  public static class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new AiReviewProviderApiModule());
      install(new GeminiReviewProviderModule());
    }
  }

  @Test
  public void shouldRegisterProviderInDynamicSet() {
    DynamicSet<AiReviewProvider> aiReviewProviders =
        plugin.getSysInjector().getInstance(new Key<>() {});
    assertThat(aiReviewProviders).isNotNull();

    Optional<AiReviewProvider> geminiProvider =
        StreamSupport.stream(aiReviewProviders.spliterator(), false)
            .filter(AiGeminiReviewProvider.class::isInstance)
            .findFirst();
    assertThat(geminiProvider).isPresent();
    assertThat(geminiProvider.get().getDisplayName()).isEqualTo("Gemini");
  }
}
