package net.fallingangel.jimmerdto.exception

class PropertyNotExistException(name: String) : RuntimeException("Property $name does not exist")