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
import static org.mockito.Mockito.when;

import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import java.util.Base64;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PBECodecTest {
  private static final String PLUGIN_NAME = "ai-review-agent-gemini";
  private static final String TEST_PASSWORD = "test-password";
  private static final String TEST_SALT = "thesalt1234";

  @Mock private PluginConfigFactory cfgFactoryMock;
  private PBECodec codec;

  @Before
  public void setUp() {
    Config cfg = new Config();
    cfg.setString("plugin", PLUGIN_NAME, "password", TEST_PASSWORD);
    cfg.setString("plugin", PLUGIN_NAME, "salt", TEST_SALT);

    PluginConfig pluginConfig = PluginConfig.createFromGerritConfig(PLUGIN_NAME, cfg);
    when(cfgFactoryMock.getFromGerritConfig(PLUGIN_NAME)).thenReturn(pluginConfig);

    codec = new PBECodec(cfgFactoryMock, PLUGIN_NAME);
  }

  @Test
  public void testEncodeAndDecode() {
    String original = "my-secret-token";
    String encoded = codec.encode(original);

    assertThat(encoded).isNotEqualTo(original);

    String decoded = codec.decode(encoded);
    assertThat(decoded).isEqualTo(original);
  }

  @Test
  public void testDecodeWithDifferentInstance() {
    String original = "another-secret-token";
    String encoded = codec.encode(original);

    PBECodec anotherCodec = new PBECodec(cfgFactoryMock, PLUGIN_NAME);
    String decoded = anotherCodec.decode(encoded);

    assertThat(decoded).isEqualTo(original);
  }
}
