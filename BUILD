package_group(
    name = "visibility",
    packages = ["//plugins/ai-review-agent-gemini/..."],
)

package(default_visibility = [":visibility"])

load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "ai-review-agent-gemini",
    srcs = glob(["src/main/java/com/gerritforge/gerrit/plugins/ai/gemini/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: ai-review-agent-gemini",
        "Gerrit-HttpModule: com.gerritforge.gerrit.plugins.ai.gemini.HttpModule",
    ],
    resource_jars = ["//plugins/ai-review-agent-gemini/web:ai-review-agent-gemini"],
    resource_strip_prefix = "plugins/ai-review-agent-gemini/resources",
    resources = glob(["resources/**/*"]),
    deps = [
        "//lib/errorprone:annotations",
    ],
)
