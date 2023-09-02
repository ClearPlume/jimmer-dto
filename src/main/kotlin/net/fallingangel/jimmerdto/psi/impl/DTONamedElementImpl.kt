package net.fallingangel.jimmerdto.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import net.fallingangel.jimmerdto.psi.DTONamedElement

abstract class DTONamedElementImpl(node: ASTNode) : DTONamedElement, ASTWrapperPsiElement(node)
