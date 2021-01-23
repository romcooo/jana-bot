package org.koppakurhiev.janabot


private class Avengers {
    fun assemble() {
        BotContext.launch()
    }
}

// Use this to test refactoring
fun main() {
    // BotContext.launch()
    Avengers().assemble()
}