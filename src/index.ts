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

import type {PluginApi} from '@gerritcodereview/typescript-api/plugin';
import type {
  AiCodeReviewProvider,
  Actions,
  Action,
  ChatRequest,
  ChatResponse,
  ChatResponseListener,
  Models,
} from '@gerritcodereview/typescript-api/ai-code-review';

const DEFAULT_MODEL = 'gemini-2.5-flash';
const LS_API_KEY = 'GERRIT_GEMINI_API_KEY';

function buildChatResponse(text: string): ChatResponse {
  return {
    response_parts: [{id: 0, text}],
    references: [], // TODO: populate references
    citations: [], // TODO: populate citations
    timestamp_millis: Date.now(),
  };
}

function getApiKey(): string | null {
  return window.localStorage.getItem(LS_API_KEY);
}

function requireApiKey(listener: ChatResponseListener): string | null {
  const key = getApiKey();
  if (!key) {
    listener.emitError(
      `Missing Gemini API key. Set it in DevTools console:\n` +
        `localStorage.setItem('${LS_API_KEY}', 'YOUR_KEY_HERE')`
    );
    listener.done();
    return null;
  }
  return key;
}

async function callGeminiGenerateContent(args: {
  apiKey: string;
  model: string;
  prompt: string;
}): Promise<string> {
  const {apiKey, model, prompt} = args;

  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/` +
    `${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(
      apiKey
    )}`;

  const body = {
    contents: [
      {
        role: 'user',
        parts: [{text: prompt}],
      },
    ],
  };

  const res = await fetch(url, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const errText = await res.text().catch(() => '');
    throw new Error(
      `Gemini error ${res.status}: ${errText || res.statusText}`
    );
  }

  const json: any = await res.json();

  // The response from the API looks like:
  //  {
  //   "candidates": [
  //     {
  //       "content": {
  //         "parts": [
  //           { "text": "Hello " },
  //           { "text": "world" }
  //         ]
  //       }
  //     }
  //   ]
  // }
  //
  // The following code take the first candidate, extract all text parts, ignore anything weird or missing,
  // and glue the text together into a single string.
  const parts: any[] =
     json?.candidates?.[0]?.content?.parts ?? [];

  const text = parts
    .map(p => (typeof p?.text === 'string' ? p.text : ''))
    .filter(Boolean)
    .join('');

  return text || '(No text returned by Gemini)';
}

class GeminiAiProvider implements AiCodeReviewProvider {
  // The provider does not support the "add extra context" feature (e.g., attaching additional files or notes beyond what Gerrit already sends)
  supports_add_context = false;
  // The provider does not maintain or accept chat history. Each request is treated as a fresh, stateless call
  supports_history = false;
  // The provider doesn’t expose extra "more" actions beyond the ones defined in getActions()
  supports_more_menu = false;
  //  The provider can operate on the current change (e.g., review/explain the active Gerrit change)
  supports_this_change = true;

  async getModels(): Promise<Models> {
    return {
      models: [
        {
          model_id: DEFAULT_MODEL,
          short_text: 'Gemini',
          full_display_text: `Gemini (${DEFAULT_MODEL})`,
        },
      ],
      default_model_id: DEFAULT_MODEL,
      documentation_url: 'https://ai.google.dev/api/generate-content',
    };
  }

  getActions(): Promise<Actions> {
    console.log('Gemini Plugin: getActions called');
    return Promise.resolve({
      actions: [
        {
          id: 'review',
          display_text: 'Review change',
          enable_send_without_input: true,
          initial_user_prompt: 'Please review this code change.',
        },
        {
          id: 'explain',
          display_text: 'Explain change',
          enable_send_without_input: true,
          initial_user_prompt: 'Explain this code change.',
        },
      ],
      default_action_id: 'review',
    });
  }

  chat(req: ChatRequest, listener: ChatResponseListener): void {
    void this.chatAsync(req, listener);
  }

  private async chatAsync(
    req: ChatRequest,
    listener: ChatResponseListener,
    plugin: PluginApi
  ): Promise<void> {
    const apiKey = requireApiKey(listener);
    if (!apiKey) return;

    listener.emitResponse(buildChatResponse('_Gathering file contents and calling Gemini...'));

    try {
      const changeId = req.change?._number;
      // We'll take the first 10 files to avoid hitting token limits or browser timeouts
      const filesToReview = (req.files || []).slice(0, 10);

      let diffContext = '';

      for (const file of filesToReview) {
        if (file.path === '/COMMIT_MSG') continue;

        // Fetch the diff from Gerrit REST API
        // Endpoint: /changes/{change-id}/revisions/current/files/{file-id}/diff
        const diff = await plugin.restApi().get(
          `/changes/${changeId}/revisions/current/files/${encodeURIComponent(file.path)}/diff?context=ALL`
        );

        // Extract the 'after' lines (the new code)
        const content = diff.content
          .map((c: any) => c.ab || c.b) // 'ab' is unchanged, 'b' is new content
          .flat()
          .join('\n');

        diffContext += `\n--- File: ${file.path} ---\n${content}\n`;
      }

      const prompt =
        `${req.prompt}\n\n` +
        `Context: This is a code review for change ${changeId}.\n` +
        `Code Content:\n${diffContext}`;

      const model = req.model_name || DEFAULT_MODEL;
      const text = await callGeminiGenerateContent({apiKey, model, prompt});

      listener.emitResponse(buildChatResponse(text));
      listener.done();
    } catch (e) {
      listener.emitError(e instanceof Error ? e.message : 'Error fetching patch content');
      listener.done();
    }
  }
}

function install(plugin: PluginApi) {
  const provider = new GeminiAiProvider();

  // Override the chat method to pass the plugin instance
  provider.chat = (req, listener) => {
    // @ts-ignore - TODO: reaching into private, there mught might be better way of doing it
    provider.chatAsync(req, listener, plugin);
  };

  plugin.aiCodeReview().register(provider);
}

window.Gerrit.install(install);
