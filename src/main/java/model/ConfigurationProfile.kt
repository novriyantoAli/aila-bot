package model

//"os": "windows",
//"path_prefix": "C:\\Users\\",
//"path_postfix": "\\AppData\\Local\\Google\\Chrome\\User Data\\",
//"profile_default": "profile_windows_default"
class ConfigurationProfile(
    val os: String,
    val pathPrefix: String,
    val pathPostfix: String,
    val profileDefault: String,
    val command: Command
)