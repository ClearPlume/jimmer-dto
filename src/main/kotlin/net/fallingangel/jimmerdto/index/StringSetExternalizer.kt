package net.fallingangel.jimmerdto.index

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.IOUtil
import java.io.DataInput
import java.io.DataOutput

class StringSetExternalizer : DataExternalizer<Set<String>> {
    override fun save(out: DataOutput, value: Set<String>) {
        DataInputOutputUtil.writeINT(out, value.size)
        for (string in value) {
            IOUtil.writeUTF(out, string)
        }
    }

    override fun read(`in`: DataInput): Set<String> {
        return IOUtil.readStringCollection(`in`, ::LinkedHashSet)
    }

    companion object {
        val INSTANCE = StringSetExternalizer()
    }
}