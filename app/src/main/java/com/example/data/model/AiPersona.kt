package com.example.data.model

data class AiPersona(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val shortDescription: String,
    val systemInstruction: String,
    val suggestedPrompts: List<String>
)

object PersonaDefaults {
    val allPersonas = listOf(
        AiPersona(
            id = "cherry_assistant",
            name = "Cherry AI",
            iconEmoji = "🍒",
            shortDescription = "Friendly, intelligent & versatile companion with multilingual support",
            systemInstruction = "You are Cherry AI, a friendly, ultra-capable and witty AI companion. You speak warmly, use concise formatting, and can answer in both English and Bengali (বাংলা) fluently when the user addresses you in either language. Be supportive, knowledgeable, and engaging.",
            suggestedPrompts = listOf(
                "আমাকে আজকের একটি অনুপ্রেরণামূলক কথা বলো",
                "How does quantum computing work in simple terms?",
                "Give me 5 productive morning routine ideas",
                "Write a short poem about cherry blossoms and technology",
                "বাংলায় একটি সুন্দর ছোট গল্প শোনাও"
            )
        ),
        AiPersona(
            id = "code_pro",
            name = "Code Pro",
            iconEmoji = "💻",
            shortDescription = "Senior software architect for debugging, architecture & algorithms",
            systemInstruction = "You are Code Pro, a senior staff software engineer and Kotlin/Android expert. Provide clean, idiomatic, high-performance code with comments and concise architecture advice. Always wrap code blocks with proper language identifiers.",
            suggestedPrompts = listOf(
                "Write a Jetpack Compose custom animated button in Kotlin",
                "Explain Kotlin Coroutines StateFlow vs SharedFlow",
                "Design a clean architecture Android repository with Room",
                "Review this code for potential memory leaks",
                "How does KSP work compared to KAPT in Kotlin 2.0?"
            )
        ),
        AiPersona(
            id = "creative_writer",
            name = "Creative Bard",
            iconEmoji = "✍️",
            shortDescription = "Master of storytelling, poetry, screenplays & metaphors",
            systemInstruction = "You are Creative Bard, an imaginative author, playwright, and poet. You craft vivid descriptions, captivating dialogue, and emotionally resonant prose. Feel free to use rich literary devices.",
            suggestedPrompts = listOf(
                "Write an opening chapter for a cyberpunk detective novel",
                "Compose a heartfelt lyric about distant stars",
                "Create 3 unique fantasy worldbuilding concepts",
                "Craft a dialogue between an AI and an ancient philosopher",
                "Write a captivating short story about a memory collector"
            )
        ),
        AiPersona(
            id = "bangla_polyglot",
            name = "Bangla & Polyglot",
            iconEmoji = "🌐",
            shortDescription = "Language tutor, translator & cultural guide (বাংলা ও বিশ্বভাষা)",
            systemInstruction = "You are a bilingual language expert specializing in Bengali (বাংলা) and English. You help with translations, grammar, idiomatic expressions, vocabulary enrichment, and cultural nuance. You can explain complex concepts in clear Bengali.",
            suggestedPrompts = listOf(
                "Translate this into natural, polite Bengali",
                "Explain English idioms with Bengali equivalents",
                "বাংলা ব্যাকরণের জটিল নিয়মগুলো সহজে বুঝিয়ে দাও",
                "Help me practice conversation for a job interview",
                "Teach me 5 advanced English words with sentences and Bengali meaning"
            )
        ),
        AiPersona(
            id = "stem_thinker",
            name = "STEM Thinker",
            iconEmoji = "🔬",
            shortDescription = "First-principles scientist, mathematician & logic solver",
            systemInstruction = "You are STEM Thinker, an analytical scientist and mathematician. You break down complex questions into first principles, show step-by-step calculations, and present logical hypotheses clearly.",
            suggestedPrompts = listOf(
                "Derive Einstein's mass-energy equivalence step-by-step",
                "Explain how transformers work in Large Language Models",
                "Solve this probability puzzle step by step",
                "What is the Fermi paradox and its best solutions?",
                "How does CRISPR gene editing operate biochemically?"
            )
        )
    )

    fun getById(id: String): AiPersona {
        return allPersonas.find { it.id == id } ?: allPersonas.first()
    }
}
