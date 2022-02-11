package model

data class Command(
    val browser: String,
    val browserArguments: List<String>,
    val killer: String,
    val killerArguments: List<String>
)