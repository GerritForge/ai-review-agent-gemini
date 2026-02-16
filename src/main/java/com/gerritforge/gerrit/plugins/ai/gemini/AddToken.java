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
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.UserInitiated;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AccountsUpdate;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIdFactory;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class AddToken implements RestModifyView<AccountResource, AddToken.Input> {
  private static final String GEMINI_PREFIX = "gemini";
  private final Provider<CurrentUser> currentUser;
  private final Provider<AccountsUpdate> accountsUpdateProvider;
  private final ExternalIds externalIds;
  private final ExternalIdFactory extIdFactory;
  private final PBECodec codec;

  public static class Input {
    public String token;
  }

  @Inject
  AddToken(
      Provider<CurrentUser> currentUser,
      @UserInitiated Provider<AccountsUpdate> accountsUpdateProvider,
      ExternalIds externalIds,
      ExternalIdFactory extIdFactory,
      PBECodec codec) {
    this.currentUser = currentUser;
    this.accountsUpdateProvider = accountsUpdateProvider;
    this.externalIds = externalIds;
    this.extIdFactory = extIdFactory;
    this.codec = codec;
  }

  @Override
  public Response<?> apply(AccountResource resource, AddToken.Input input)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {

    Account.Id accountId = resource.getUser().getAccountId();

    CurrentUser cu = currentUser.get();
    if (!(cu instanceof IdentifiedUser)) {
      throw new AuthException("Authentication required");
    }

    Account.Id callerId = ((IdentifiedUser) cu).getAccountId();

    if (!callerId.equals(accountId)) {
      throw new AuthException("Cannot modify another user's token");
    }

    if (input == null || input.token == null) {
      throw new BadRequestException("Missing 'token'");
    }

    String token = input.token.trim();
    if (token.isEmpty()) {
      throw new BadRequestException("Empty 'token'");
    }

    String userTokenPrefix = String.format("%s_%s_", GEMINI_PREFIX, callerId.get());
    String userToken = String.join("", userTokenPrefix, codec.encode(token));

    // external-id key format: external:gemini_<accountId>_<token>
    ExternalId extId = extIdFactory.create(SCHEME_EXTERNAL, userToken, accountId);

    accountsUpdateProvider
        .get()
        .update(
            "Updated Gemini token for account " + accountId,
            accountId,
            u -> {
              // Remove older Gemini token(s) for this account (if any)
              for (ExternalId e : externalIds.byAccount(accountId)) {
                if (SCHEME_EXTERNAL.equals(e.key().scheme())
                    && e.key().id() != null
                    && e.key().id().startsWith(userTokenPrefix)) {
                  // Delete the previous Gemini token for this account, regardless of its old value.
                  u.deleteExternalId(e);
                }
              }

              u.deleteExternalId(extId);
              u.addExternalId(extId);
            });

    return Response.ok();
  }
}
