package net.fallingangel.jimmerdto.psi.element.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import net.fallingangel.jimmerdto.psi.element.DTOGroupedImport
import net.fallingangel.jimmerdto.psi.element.DTOImportedType
import net.fallingangel.jimmerdto.psi.element.DTOVisitor
import net.fallingangel.jimmerdto.util.findChildren
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class DTOGroupedImportImpl(node: ASTNode) : ANTLRPsiNode(node), DTOGroupedImport {
    override val types: List<DTOImportedType>
        get() = findChildren("/groupedImport/importedType")

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DTOVisitor) {
            visitor.visitGroupedImport(this)
        } else {
            super.accept(visitor)
        }
    }
}
