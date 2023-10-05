package net.fallingangel.jimmerdto.exception

class IllegalFileFormatException(type: String) : RuntimeException("Type <$type> of file is not a valid format")
