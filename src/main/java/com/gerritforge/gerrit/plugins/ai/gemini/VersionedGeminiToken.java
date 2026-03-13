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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.AllUsersName;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.git.meta.VersionedMetaData;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;

/** Stores the encrypted Gemini API token in the user's gemini.config in All-Users. */
public class VersionedGeminiToken extends VersionedMetaData {
  private static final String FILE_NAME = "gemini.config";
  private static final String SECTION = "token";
  private static final String TOKEN_KEY = "hash";

  @Singleton
  public static class Accessor {
    private final GitRepositoryManager repoManager;
    private final AllUsersName allUsersName;
    private final Provider<MetaDataUpdate.User> metaDataUpdateFactory;
    private final IdentifiedUser.GenericFactory userFactory;

    @Inject
    Accessor(
        GitRepositoryManager repoManager,
        AllUsersName allUsersName,
        Provider<MetaDataUpdate.User> metaDataUpdateFactory,
        IdentifiedUser.GenericFactory userFactory) {
      this.repoManager = repoManager;
      this.allUsersName = allUsersName;
      this.metaDataUpdateFactory = metaDataUpdateFactory;
      this.userFactory = userFactory;
    }

    public Optional<String> getToken(Account.Id accountId)
        throws IOException, ConfigInvalidException {
      return read(accountId).getToken();
    }

    public synchronized void setToken(Account.Id accountId, String encryptedToken)
        throws IOException, ConfigInvalidException {
      VersionedGeminiToken geminiToken = read(accountId);
      geminiToken.setToken(encryptedToken);
      commit(accountId, geminiToken);
    }

    private VersionedGeminiToken read(Account.Id accountId)
        throws IOException, ConfigInvalidException {
      try (Repository git = repoManager.openRepository(allUsersName)) {
        VersionedGeminiToken geminiToken = new VersionedGeminiToken(accountId);
        geminiToken.load(allUsersName, git);
        return geminiToken;
      }
    }

    private void commit(Account.Id accountId, VersionedGeminiToken geminiToken) throws IOException {
      try (MetaDataUpdate md =
          metaDataUpdateFactory.get().create(allUsersName, userFactory.create(accountId))) {
        geminiToken.commit(md, true);
      }
    }
  }

  private final String ref;
  private Config cfg;
  private Optional<String> token;

  VersionedGeminiToken(Account.Id accountId) {
    this.ref = RefNames.refsUsers(accountId);
  }

  @Override
  protected String getRefName() {
    return ref;
  }

  @Override
  protected void onLoad() throws IOException, ConfigInvalidException {
    cfg = readConfig(FILE_NAME);
    token = Optional.ofNullable(Strings.emptyToNull(cfg.getString(SECTION, null, TOKEN_KEY)));
  }

  @Override
  protected boolean onSave(CommitBuilder commit) throws IOException {
    if (Strings.isNullOrEmpty(commit.getMessage())) {
      commit.setMessage("Update Gemini API token\n");
    }

    checkLoaded();
    if (token.isEmpty()) {
      cfg.unset(SECTION, null, TOKEN_KEY);
    } else {
      cfg.setString(SECTION, null, TOKEN_KEY, token.get());
    }

    saveConfig(FILE_NAME, cfg);
    return true;
  }

  Optional<String> getToken() {
    checkLoaded();
    return token;
  }

  void setToken(String encryptedToken) {
    token = Optional.of(encryptedToken);
  }

  private void checkLoaded() {
    checkState(token != null, "Gemini token not loaded yet");
  }
}
