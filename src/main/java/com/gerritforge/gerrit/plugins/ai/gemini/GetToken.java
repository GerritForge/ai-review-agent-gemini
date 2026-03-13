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

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.secureconfig.Codec;

import java.util.Optional;

@Singleton
public class GetToken implements RestReadView<AccountResource> {

  private final Provider<CurrentUser> currentUser;
  private final VersionedGeminiToken.Accessor geminiTokenAccessor;
  private final Codec codec;

  public static class Output {
    public String token;
  }

  @Inject
  GetToken(
      Provider<CurrentUser> currentUser,
      VersionedGeminiToken.Accessor geminiTokenAccessor,
      Codec codec) {
    this.currentUser = currentUser;
    this.geminiTokenAccessor = geminiTokenAccessor;
    this.codec = codec;
  }

  @Override
  public Response<Output> apply(AccountResource resource)
      throws AuthException, BadRequestException, ResourceNotFoundException, Exception {

    IdentifiedUser iu = resource.getUser();
    Account.Id accountId = iu.getAccountId();

    if (!iu.hasSameAccountId(currentUser.get())) {
      throw new AuthException("Cannot read another user's token");
    }

    Optional<String> storedToken = geminiTokenAccessor.getToken(accountId);
    if (storedToken.isPresent()) {
      Output out = new Output();
      out.token = codec.decode(storedToken.get());
      return Response.ok(out);
    }

    throw new ResourceNotFoundException("Gemini token not set");
  }
}
