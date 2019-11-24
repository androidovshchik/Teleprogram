package defpackage.teleprogram.model

@Suppress("unused")
enum class Script(
    val id: Int
) {
    BASE(0),
    MAIN(1),
    EVENT_MESSAGE(100),
    WORK_ATTEMPT(200),
    WORK_SINGLE(210),
    WORK_REPEAT(220);

    companion object {

        private val map = values().associateBy(Script::id)
    }
}