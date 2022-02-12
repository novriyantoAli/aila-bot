package model

data class Configuration(
    val keyword: String,
    val channelName: String,
    val port: Int,
    val channelPlaylistTargetName: String,
    val channelPlaylistTargetCode: String,
    val configurationProfile: List<ConfigurationProfile>,
    val profileList: List<String>,
    val configurationAccount: List<ConfigurationAccount>,
    val proxyList: List<String>,
    val profile: Profile,
    val defaultDirectory: String,
    val runningDirectory: String
)