package net.fallingangel.jimmerdto.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import net.fallingangel.jimmerdto.psi.DTOElement

abstract class DTOElementImpl(node: ASTNode) : DTOElement, ASTWrapperPsiElement(node)
