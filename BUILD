load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)

gerrit_plugin(
    name = "ai-review-agent-gemini",
    srcs = glob(["src/main/java/com/gerritforge/gerrit/plugins/ai/gemini/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: ai-review-agent-gemini",
        "Gerrit-Module: com.gerritforge.gerrit.plugins.ai.gemini.GeminiReviewProviderModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":ai-review-agent-provider-neverlink",
    ],
)

gerrit_plugin_tests(
    name = "ai-review-agent-gemini_tests",
    srcs = glob(["src/test/java/**/*IT.java"]),
    tags = ["ai-review-agent-gemini"],
    visibility = ["//visibility:public"],
    deps = [
        ":ai-review-agent-gemini__plugin",
        "//plugins/ai-review-agent-provider:ai-review-agent-provider-api",
    ],
)

java_library(
    name = "ai-review-agent-provider-neverlink",
    neverlink = True,
    exports = ["//plugins/ai-review-agent-provider:ai-review-agent-provider-api"],
)
