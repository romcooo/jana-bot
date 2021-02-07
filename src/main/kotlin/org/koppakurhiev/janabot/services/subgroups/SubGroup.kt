package org.koppakurhiev.janabot.services.subgroups

class SubGroup(
    val name: String,
    val chatId: Long,
    val admins: MutableList<Int> = mutableListOf(),
    val members: MutableList<String> = mutableListOf(),
) {

    override fun toString(): String {
        return "SubGroup(name='$name', chatId=$chatId, members=$members, admins=$admins)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SubGroup
        if (name != other.name) return false
        if (chatId != other.chatId) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + chatId.hashCode()
        return result
    }
}
