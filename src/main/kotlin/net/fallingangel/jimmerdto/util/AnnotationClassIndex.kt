package net.fallingangel.jimmerdto.util

import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.idea.util.isKotlinFileType

val ANNOTATION_CLASS_INDEX = ID.create<String, Void>("net.fallingangel.jimmerdto.util.AnnotationClassIndex")

class AnnotationClassIndex : ScalarIndexExtension<String>() {
    override fun getName() = ANNOTATION_CLASS_INDEX

    override fun getIndexer() = DataIndexer<String, Void, FileContent> { file ->
        PsiTreeUtil.findChildrenOfType(file.psiFile, PsiClass::class.java)
                .filter { it.isAnnotationType }
                .associate { Pair(it.qualifiedName, null) }
    }

    override fun getKeyDescriptor() = EnumeratorStringDescriptor()

    override fun getInputFilter() = FileBasedIndex.InputFilter { it.isJavaFileType() || it.isKotlinFileType() }

    override fun dependsOnFileContent() = true

    override fun getVersion() = 0
}