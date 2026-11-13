package fr.nihongo.app

internal data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String?
) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int {
        compareValuesBy(this, other, ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)
            .takeIf { it != 0 }
            ?.let { return it }

        if (prerelease == null) return if (other.prerelease == null) 0 else 1
        if (other.prerelease == null) return -1

        val leftParts = prerelease.split('.')
        val rightParts = other.prerelease.split('.')
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val left = leftParts.getOrNull(index) ?: return -1
            val right = rightParts.getOrNull(index) ?: return 1
            val leftNumber = left.toIntOrNull()
            val rightNumber = right.toIntOrNull()
            val comparison = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1
                rightNumber != null -> 1
                else -> left.compareTo(right)
            }
            if (comparison != 0) return comparison
        }
        return 0
    }

    companion object {
        private val pattern =
            Regex("""^v?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

        fun parse(value: String): ReleaseVersion? {
            val match = pattern.matchEntire(value.trim()) ?: return null
            return ReleaseVersion(
                major = match.groupValues[1].toIntOrNull() ?: return null,
                minor = match.groupValues[2].toIntOrNull() ?: return null,
                patch = match.groupValues[3].toIntOrNull() ?: return null,
                prerelease = match.groupValues[4].ifEmpty { null }
            )
        }
    }
}
