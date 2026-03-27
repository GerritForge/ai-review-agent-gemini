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

import com.gerritforge.gerrit.plugins.ai.provider.api.AiReviewProvider;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.AbstractModule;

import java.util.Set;

public class GeminiReviewProviderModule extends AbstractModule {

  public static final AiReviewProvider INSTANCE = new AiReviewProvider() {

    @Override
    public String getDisplayName() {
      return "Gemini";
    }

    @Override
    public Set<String> getModels() {
      return Set.of("gemini-2.5-flash", "gemini-2.5-pro");
    }

    @Override
    public String review(String apiToken, String model, String prompt) {
      return "**Gemini integration not implemented yet**";
    }
  };

  @Override
  protected void configure() {
    DynamicSet.bind(binder(), AiReviewProvider.class).toInstance(INSTANCE);
  }
}
