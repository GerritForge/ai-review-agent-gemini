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

import static com.google.gerrit.server.account.externalids.ExternalId.SCHEME_EXTERNAL;

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class GetToken implements RestReadView<AccountResource> {
  private static final String GEMINI_PREFIX = "gemini";

  public static class Output {
    public String token;

    private Output(String token) {
      this.token = token;
    }
  }

  private final Provider<CurrentUser> currentUser;
  private final ExternalIds externalIds;
  private final PBECodec codec;

  @Inject
  GetToken(Provider<CurrentUser> currentUser, ExternalIds externalIds, PBECodec codec) {
    this.currentUser = currentUser;
    this.externalIds = externalIds;
    this.codec = codec;
  }

  @Override
  public Response<?> apply(AccountResource resource)
      throws AuthException, BadRequestException, ResourceNotFoundException, Exception {

    Account.Id accountId = resource.getUser().getAccountId();

    // Enforce self-service only: callers may only read their own token.
    CurrentUser cu = currentUser.get();
    if (!(cu instanceof IdentifiedUser)) {
      throw new AuthException("Authentication required");
    }
    Account.Id callerId = ((IdentifiedUser) cu).getAccountId();
    if (!callerId.equals(accountId)) {
      throw new AuthException("Cannot read another user's token");
    }

    String userTokenPrefix = String.format("%s_%s_", GEMINI_PREFIX, callerId.get());

    ExternalId match = null;
    for (ExternalId e : externalIds.byAccount(accountId)) {
      if (SCHEME_EXTERNAL.equals(e.key().scheme())
          && e.key().id() != null
          && e.key().id().startsWith(userTokenPrefix)) {
        match = e;
        break;
      }
    }

    if (match == null) {
      throw new ResourceNotFoundException("Gemini token not set");
    }

    // Stored value format: gemini_<accountId>_<base64(PBE(token))>
    String storedId = match.key().id();
    int lastUnderscore = storedId.lastIndexOf('_');
    if (lastUnderscore < 0 || lastUnderscore == storedId.length() - 1) {
      throw new BadRequestException("Stored token has invalid format");
    }

    String encoded = storedId.substring(lastUnderscore + 1);
    String token = codec.decode(encoded);

    return Response.ok(new Output(token));
  }
}
