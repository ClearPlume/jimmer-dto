package net.fallingangel.jimmerdto.enums

enum class SimplePropType(val family: Family, val fits: (kotlin.String) -> kotlin.Boolean = { true }) {
    Byte(Family.Integer, { it.toByteOrNull() != null }),
    Short(Family.Integer, { it.toShortOrNull() != null }),
    Int(Family.Integer, { it.toIntOrNull() != null }),
    Long(Family.Integer, { it.toLongOrNull() != null }),
    BigInteger(Family.Integer),
    Float(Family.Float, { it.toFloatOrNull()?.isFinite() == true }),
    Double(Family.Float, { it.toDoubleOrNull()?.isFinite() == true }),
    BigDecimal(Family.Float),
    Boolean(Family.Boolean),
    String(Family.String);

    enum class Family(val presentation: kotlin.String) {
        Boolean("boolean"), Integer("integer"), Float("float"), String("string"),
    }
}