package_group(
    name = "visibility",
    packages = ["//plugins/ai-review-agent-gemini/..."],
)

package(default_visibility = [":visibility"])

load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:junit.bzl", "junit_tests")

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
        "//lib/errorprone:annotations",
        "@google-api-common//jar",
        "@google-gemini//jar",
        "@jackson-annotations//jar",
        "@jackson-core//jar",
        "@jackson-databind//jar",
        "@jackson-datatype-jdk8//jar",
        "@jackson-datatype-jsr310//jar",
        "@kotlin-stdlib-jdk8//jar",
        "@kotlin-stdlib//jar",
        "@okhttp//jar",
        "@okio-jvm//jar",
        "@okio//jar",
    ],
)

junit_tests(
    name = "ai-review-agent-gemini_tests",
    srcs = glob(["src/test/java/**/*IT.java"]),
    tags = ["ai-review-agent-gemini"],
    visibility = ["//visibility:public"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":ai-review-agent-gemini__plugin",
        "//plugins/ai-review-agent-provider",
    ],
)

java_library(
    name = "ai-review-agent-provider-neverlink",
    neverlink = True,
    exports = ["//plugins/ai-review-agent-provider"],
)
