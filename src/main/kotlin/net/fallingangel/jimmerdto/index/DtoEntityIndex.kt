package net.fallingangel.jimmerdto.index

import com.intellij.util.indexing.*
import com.intellij.util.io.BooleanDataDescriptor
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.psi.DTOFile
import java.util.*

val DTO_ENTITY_INDEX = ID.create<String, Boolean>("net.fallingangel.jimmerdto.index.DtoEntityIndex")

class DtoEntityIndex : FileBasedIndexExtension<String, Boolean>() {
    private val indexer = DataIndexer<String, Boolean, FileContent> { content ->
        val file = content.psiFile as? DTOFile ?: return@DataIndexer emptyMap()
        Collections.singletonMap(file.qualifiedEntity, file.hasExport)
    }

    override fun getName() = DTO_ENTITY_INDEX

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(DTOFileType.INSTANCE)

    override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<Boolean>  = BooleanDataDescriptor.INSTANCE

    override fun getIndexer() = indexer

    override fun dependsOnFileContent() = true

    override fun getVersion() = 1
}