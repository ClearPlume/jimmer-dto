package net.fallingangel.jimmerdto.tooling;

import java.io.Serializable;
import java.util.Map;

public interface JimmerBuildModel extends Serializable {
    Map<String, String> options();
}
