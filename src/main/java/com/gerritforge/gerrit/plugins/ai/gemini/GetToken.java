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

import static com.gerritforge.gerrit.plugins.ai.gemini.TokenUtils.getTokenPrefix;
import static com.google.gerrit.server.account.externalids.ExternalId.SCHEME_EXTERNAL;

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.secureconfig.Codec;

@Singleton
public class GetToken implements RestReadView<AccountResource> {

  private final Provider<CurrentUser> currentUser;
  private final ExternalIds externalIds;
  private final Codec codec;

  @Inject
  GetToken(Provider<CurrentUser> currentUser, ExternalIds externalIds, Codec codec) {
    this.currentUser = currentUser;
    this.externalIds = externalIds;
    this.codec = codec;
  }

  @Override
  public Response<String> apply(AccountResource resource)
      throws AuthException, BadRequestException, ResourceNotFoundException, Exception {

    Account.Id accountId = resource.getUser().getAccountId();

    CurrentUser cu = currentUser.get();
    if (!cu.isIdentifiedUser()) {
      throw new AuthException("Authentication required");
    }

    Account.Id callerId = cu.asIdentifiedUser().getAccountId();
    if (!callerId.equals(accountId)) {
      throw new AuthException("Cannot read another user's token");
    }

    ExternalId match = null;
    for (ExternalId e : externalIds.byAccount(accountId)) {
      if (SCHEME_EXTERNAL.equals(e.key().scheme())
          && e.key().id() != null
          && e.key().id().startsWith(getTokenPrefix(callerId) + ":")) {
        match = e;
        break;
      }
    }

    if (match == null) {
      throw new ResourceNotFoundException("Gemini token not set");
    }

    // Stored value format: gemini:<accountId>:<Encrypted(token)>
    String storedId = match.key().id();
    // The + 1 is because we want to include the ":" after the accountId
    int prefixLength = getTokenPrefix(callerId).length() + 1;
    return Response.ok(codec.decode(storedId.substring(prefixLength)));
  }
}
