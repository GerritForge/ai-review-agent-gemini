package com.gerritforge.gerrit.plugins.ai.gemini;

import static com.google.gerrit.server.account.AccountResource.ACCOUNT_KIND;

import com.google.gerrit.extensions.restapi.RestApiModule;

public class AiReviewRestApiModule extends RestApiModule {

  @Override
  protected void configure() {
    put(ACCOUNT_KIND, "geminiToken").to(AddToken.class);
  }
}
