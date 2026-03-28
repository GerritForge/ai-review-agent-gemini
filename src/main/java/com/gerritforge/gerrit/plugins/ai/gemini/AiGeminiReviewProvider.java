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

import com.gerritforge.gerrit.plugins.ai.provider.AiReviewProviderModule;
import com.gerritforge.gerrit.plugins.ai.provider.api.AiReviewProvider;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class AiGeminiReviewProvider implements AiReviewProvider {
	private static final String GEMINI_PROVIDER = "gemini";

	@Override
	public String key() {
		return GEMINI_PROVIDER;
	}

	@Override
	public String review(String apiKey, String model, String prompt) {
		try (Client client = Client.builder()
			.apiKey(apiKey)
			.build()) {
			GenerateContentResponse response = client.models.generateContent(model, prompt, null);
			return response.text();
		}
	}
}
