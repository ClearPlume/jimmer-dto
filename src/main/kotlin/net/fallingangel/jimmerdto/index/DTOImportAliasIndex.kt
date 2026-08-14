package net.fallingangel.jimmerdto.index

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import net.fallingangel.jimmerdto.core.DTOFileType
import net.fallingangel.jimmerdto.psi.DTOFile

val DTO_IMPORT_ALIAS_INDEX = ID.create<String, Set<String>>("net.fallingangel.jimmerdto.index.DTOImportAliasIndex")

class DTOImportAliasIndex : FileBasedIndexExtension<String, Set<String>>() {
    private val indexer = DataIndexer<String, Set<String>, FileContent> { content ->
        val file = content.psiFile as? DTOFile ?: return@DataIndexer emptyMap()
        file.importIndex
            .mapNotNull { (alias, qualified) -> qualified.singleOrNull()?.let { it.qualifiedName to alias } }
            .groupingBy { it.first }
            .fold({ _, _ -> linkedSetOf<String>() }) { _, accumulator, element ->
                accumulator.add(element.second)
                accumulator
            }
            .toMap()
    }

    override fun getName() = DTO_IMPORT_ALIAS_INDEX

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(DTOFileType.INSTANCE)

    override fun getKeyDescriptor(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<Set<String>> = StringSetExternalizer.INSTANCE

    override fun getIndexer() = indexer

    override fun dependsOnFileContent() = true

    override fun getVersion() = 0
}