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

import com.google.common.base.Strings;
import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.permissions.GlobalPermission;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.secureconfig.Codec;

public class AddToken implements RestModifyView<AccountResource, AddToken.Input> {
  private final Provider<CurrentUser> currentUser;
  private final VersionedGeminiToken.Accessor geminiTokenAccessor;
  private final Codec codec;
  private final PermissionBackend permissionBackend;

  public static class Input {
    public String token;
  }

  @Inject
  protected AddToken(
      Provider<CurrentUser> currentUser,
      VersionedGeminiToken.Accessor geminiTokenAccessor,
      Codec codec,
      PermissionBackend permissionBackend) {
    this.currentUser = currentUser;
    this.geminiTokenAccessor = geminiTokenAccessor;
    this.codec = codec;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<?> apply(AccountResource resource, AddToken.Input input)
      throws AuthException, BadRequestException, ResourceConflictException, Exception {

    IdentifiedUser iu = resource.getUser();
    Account.Id accountId = iu.getAccountId();

    if (!iu.hasSameAccountId(currentUser.get())) {
      permissionBackend.currentUser().check(GlobalPermission.ADMINISTRATE_SERVER);
    }

    if (Strings.isNullOrEmpty(input.token)) {
      throw new BadRequestException("Missing 'token'");
    }
    geminiTokenAccessor.setToken(accountId, codec.encode(input.token.trim()));

    return Response.created();
  }
}
