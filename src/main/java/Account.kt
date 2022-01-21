data class Account(val username: String? = null, val password: String? = null, var profile: String? = null){
    override fun toString(): String { return "$username -> $profile" }
}