package net.fallingangel.jimmerdto.util

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.idea.util.isKotlinFileType
import org.jetbrains.kotlin.psi.KtClass

val ANNOTATION_CLASS_INDEX = ID.create<String, Void>("net.fallingangel.jimmerdto.util.AnnotationClassIndex")

class AnnotationClassIndex : ScalarIndexExtension<String>() {
    override fun getName() = ANNOTATION_CLASS_INDEX

    override fun getIndexer() = DataIndexer<String, Void, FileContent> { file ->
        if (file.fileType == JavaFileType.INSTANCE) {
            PsiTreeUtil.findChildrenOfType(file.psiFile, PsiClass::class.java)
                    .filter { it.isAnnotationType }
                    .associate { Pair(it.qualifiedName, null) }
        } else {
            PsiTreeUtil.findChildrenOfType(file.psiFile, KtClass::class.java)
                    .filter { it.isAnnotation() }
                    .associate { Pair(it.fqName!!.asString(), null) }
        }
    }

    override fun getKeyDescriptor() = EnumeratorStringDescriptor()

    override fun getInputFilter() = FileBasedIndex.InputFilter { it.isJavaFileType() || it.isKotlinFileType() }

    override fun dependsOnFileContent() = true

    override fun getVersion() = 0
}