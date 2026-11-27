package com.zion.mobile.commands

class CommandParser(private val installedApps: Map<String, String>) {

    fun parseCommand(text: String): Command {

        val input = text.lowercase().trim()

        return when {
            input.contains("hello") -> Command(CommandIntent.GREETING)

            input.contains("your name") -> Command(CommandIntent.GET_NAME)

            input.contains("refresh apps") -> Command(CommandIntent.REFRESH_APPS)

            input.contains("open ") ||
            input.contains("start ") ||
            input.contains("launch ") -> {
                var target: String? = null

                for ((appName, _) in installedApps) {
                    if (input.contains(appName)) {
                        target = appName
                        break
                    }
                }
                Command(CommandIntent.OPEN_APP, target)
            }
            
            input.contains("call ") ||
            input.contains("dial ") -> {
                var recent = false

                var target = input
                    .replaceFirst("call ", "")
                    .replaceFirst("dial ", "")
                    .trim()

                if (target.contains("recent")) {
                    target = target
                        .replace("recently", "")
                        .replace("recent", "")
                        .trim()

                    recent = true
                }

                Command(CommandIntent.CALL, target, recent)
            }

            else ->
                Command(CommandIntent.UNKNOWN)
        }
    }
}