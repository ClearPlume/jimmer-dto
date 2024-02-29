package net.fallingangel.jimmerdto.psi.mixin.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import net.fallingangel.jimmerdto.psi.mixin.DTOSingleProp

abstract class DTOSinglePropImpl(node: ASTNode) : DTOSingleProp, ASTWrapperPsiElement(node)