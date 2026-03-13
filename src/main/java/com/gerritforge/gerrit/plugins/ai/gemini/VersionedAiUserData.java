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

import static com.google.common.base.Preconditions.checkArgument;

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
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;

/** Stores the encrypted Gemini API token in the user's ai-user-data.config in All-Users. */
public class VersionedAiUserData extends VersionedMetaData {
  public interface Factory {
    VersionedAiUserData create(Account.Id accountId);
  }

  static final String FILE_NAME = "ai-user-data.config";
  private static final String SECTION = "token";
  private static final String SUBSECTION = "gemini";
  private static final String TOKEN_KEY = "apiKey";

  private final GitRepositoryManager repoManager;
  private final AllUsersName allUsersName;
  private final Provider<MetaDataUpdate.User> metaDataUpdateFactory;
  private final IdentifiedUser.GenericFactory userFactory;
  private final Account.Id accountId;
  private final String refName;
  private Config cfg;

  @Inject
  VersionedAiUserData(
      GitRepositoryManager repoManager,
      AllUsersName allUsersName,
      Provider<MetaDataUpdate.User> metaDataUpdateFactory,
      IdentifiedUser.GenericFactory userFactory,
      @Assisted Account.Id accountId) {
    this.repoManager = repoManager;
    this.allUsersName = allUsersName;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.userFactory = userFactory;
    this.accountId = accountId;
    this.refName = RefNames.refsUsers(accountId);
  }

  @Override
  protected String getRefName() {
    return refName;
  }

  public VersionedAiUserData load() throws IOException, ConfigInvalidException {
    try (Repository git = repoManager.openRepository(allUsersName)) {
      load(allUsersName, git);
    }
    return this;
  }

  @Override
  protected void onLoad() throws IOException, ConfigInvalidException {
    cfg = readConfig(FILE_NAME);
  }

  @Override
  protected boolean onSave(CommitBuilder commit) throws IOException {
    if (Strings.isNullOrEmpty(commit.getMessage())) {
      commit.setMessage("Update Gemini API token\n");
    }

    saveConfig(FILE_NAME, cfg);
    return true;
  }

  public Optional<String> getToken() {
    return Optional.ofNullable(Strings.emptyToNull(cfg.getString(SECTION, SUBSECTION, TOKEN_KEY)));
  }

  public void setToken(String encryptedToken) throws IOException {
    checkArgument(
        !Strings.isNullOrEmpty(encryptedToken), "Gemini token must be present and non-empty");

    cfg.setString(SECTION, SUBSECTION, TOKEN_KEY, encryptedToken);
    try (MetaDataUpdate md =
        metaDataUpdateFactory.get().create(allUsersName, userFactory.create(accountId))) {
      commit(md, true);
    }
  }
}
