<<<<<<< PATCH SET (4731415d836a0e4e99370e6488421dc3e565eb44 Call Gemini API from the backend)
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
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class AiReviewProviderModule extends AbstractModule {

  @Override
  protected void configure() {
    DynamicItem.bind(binder(), AiReviewProvider.class)
        .to(AiGeminiReviewProvider.class)
        .in(Scopes.SINGLETON);
  }
}
=======
>>>>>>> BASE      (fb72521809781b5d23dff785c24ae4787ed73c67 Move the Gerrit ai-review-agent UI out of the plugin)
