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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.UserInitiated;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.account.AccountsUpdate;
import com.google.gerrit.server.account.externalids.ExternalId;
import com.google.gerrit.server.account.externalids.ExternalIdFactory;
import com.google.gerrit.server.account.externalids.ExternalIds;
import com.google.gerrit.server.permissions.GlobalPermission;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.secureconfig.Codec;

public class AddToken implements RestModifyView<AccountResource, AddToken.Input> {

  private final Provider<CurrentUser> currentUser;
  private final Provider<AccountsUpdate> accountsUpdateProvider;
  private final ExternalIds externalIds;
  private final ExternalIdFactory extIdFactory;
  private final Codec codec;
  private final PermissionBackend permissionBackend;

  public static class Input {
    public String token;
  }

  @Inject
  protected AddToken(
      Provider<CurrentUser> currentUser,
      @UserInitiated Provider<AccountsUpdate> accountsUpdateProvider,
      ExternalIds externalIds,
      ExternalIdFactory extIdFactory,
      Codec codec,
      PermissionBackend permissionBackend) {
    this.currentUser = currentUser;
    this.accountsUpdateProvider = accountsUpdateProvider;
    this.externalIds = externalIds;
    this.extIdFactory = extIdFactory;
    this.codec = codec;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<?> apply(AccountResource resource, AddToken.Input input)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {

    Account.Id accountId = resource.getUser().getAccountId();

    CurrentUser cu = currentUser.get();
    if (!cu.isIdentifiedUser()) {
      throw new AuthException("Authentication required");
    }

    Account.Id callerId = cu.asIdentifiedUser().getAccountId();

    if (!callerId.equals(accountId)
        && !permissionBackend.user(cu).test(GlobalPermission.ADMINISTRATE_SERVER)) {
      throw new AuthException("Cannot modify another user's token");
    }

    if (input == null || Strings.isNullOrEmpty(input.token)) {
      throw new BadRequestException("Missing 'token'");
    }
    String token = input.token;

    // external-id key format: external:gemini:<accountId>:<token>
    ExternalId extId =
        extIdFactory.create(
            SCHEME_EXTERNAL, getFormattedUserToken(token.trim(), accountId, codec), accountId);

    ImmutableSet<ExternalId> existingExtIds = externalIds.byAccount(accountId);
    accountsUpdateProvider
        .get()
        .update(
            "Updated Gemini token for account " + accountId,
            accountId,
            u -> {
              // Remove older Gemini token(s) for this account (if any)
              for (ExternalId e : existingExtIds) {
                if (SCHEME_EXTERNAL.equals(e.key().scheme())
                    && e.key().id() != null
                    && e.key().id().startsWith(getTokenPrefix(accountId) + ":")) {
                  // Delete the previous Gemini token for this account, regardless of its old value.
                  u.deleteExternalId(e);
                }
              }

              u.addExternalId(extId);
            });

    return Response.created();
  }

  static String getFormattedUserToken(String token, Account.Id accountId, Codec codec) {
    return String.join(":", getTokenPrefix(accountId), codec.encode(token));
  }
}
