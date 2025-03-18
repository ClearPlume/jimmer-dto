package net.fallingangel.jimmerdto.psi.element

import com.intellij.psi.PsiElementVisitor

open class DTOVisitor : PsiElementVisitor() {
    open fun visitExportStatement(o: DTOExportStatement) {
        visitElement(o)
    }

    open fun visitImportStatement(o: DTOImportStatement) {
        visitElement(o)
    }

    open fun visitGroupedImport(o: DTOGroupedImport) {
        visitElement(o)
    }

    open fun visitImportedType(o: DTOImportedType) {
        visitElement(o)
    }

    open fun visitImported(o: DTOImported) {
        visitElement(o)
    }

    open fun visitAlias(o: DTOAlias) {
        visitElement(o)
    }

    open fun visitQualifiedName(o: DTOQualifiedName) {
        visitElement(o)
    }

    open fun visitQualifiedNamePart(o: DTOQualifiedNamePart) {
        visitElement(o)
    }

    open fun visitDto(o: DTODto) {
        visitElement(o)
    }

    open fun visitDtoName(o: DTODtoName) {
        visitElement(o)
    }

    open fun visitImplements(o: DTOImplements) {
        visitElement(o)
    }

    open fun visitDtoBody(o: DTODtoBody) {
        visitElement(o)
    }

    open fun visitAnnotation(o: DTOAnnotation) {
        visitElement(o)
    }

    open fun visitAnnotationValue(o: DTOAnnotationValue) {
        visitElement(o)
    }

    open fun visitAnnotationSingleValue(o: DTOAnnotationSingleValue) {
        visitElement(o)
    }

    open fun visitAnnotationArrayValue(o: DTOAnnotationArrayValue) {
        visitElement(o)
    }

    open fun visitAnnotationParameter(o: DTOAnnotationParameter) {
        visitElement(o)
    }

    open fun visitNestAnnotation(o: DTONestAnnotation) {
        visitElement(o)
    }

    open fun visitMacro(o: DTOMacro) {
        visitElement(o)
    }

    open fun visitMacroName(o: DTOMacroName) {
        visitElement(o)
    }

    open fun visitMacroArgs(o: DTOMacroArgs) {
        visitElement(o)
    }

    open fun visitNegativeProp(o: DTONegativeProp) {
        visitElement(o)
    }

    open fun visitAliasGroup(o: DTOAliasGroup) {
        visitElement(o)
    }

    open fun visitUserProp(o: DTOUserProp) {
        visitElement(o)
    }

    open fun visitTypeDef(o: DTOTypeDef) {
        visitElement(o)
    }

    open fun visitGenericArguments(o: DTOGenericArguments) {
        visitElement(o)
    }

    open fun visitGenericArgument(o: DTOGenericArgument) {
        visitElement(o)
    }

    open fun visitPositiveProp(o: DTOPositiveProp) {
        visitElement(o)
    }

    open fun visitPropConfig(o: DTOPropConfig) {
        visitElement(o)
    }

    open fun visitWhereArgs(o: DTOWhereArgs) {
        visitElement(o)
    }

    open fun visitPredicate(o: DTOPredicate) {
        visitElement(o)
    }

    open fun visitCompare(o: DTOCompare) {
        visitElement(o)
    }

    open fun visitCompareSymbol(o: DTOCompareSymbol) {
        visitElement(o)
    }

    open fun visitNullity(o: DTONullity) {
        visitElement(o)
    }

    open fun visitOrderByArgs(o: DTOOrderByArgs) {
        visitElement(o)
    }

    open fun visitOrderItem(o: DTOOrderItem) {
        visitElement(o)
    }

    open fun visitPropValue(o: DTOPropValue) {
        visitElement(o)
    }

    open fun visitIntPair(o: DTOIntPair) {
        visitElement(o)
    }

    open fun visitPropName(o: DTOPropName) {
        visitElement(o)
    }

    open fun visitPropFlag(o: DTOPropFlag) {
        visitElement(o)
    }

    open fun visitPropArg(o: DTOPropArg) {
        visitElement(o)
    }

    open fun visitValue(o: DTOValue) {
        visitElement(o)
    }

    open fun visitPropBody(o: DTOPropBody) {
        visitElement(o)
    }

    open fun visitEnumBody(o: DTOEnumBody) {
        visitElement(o)
    }

    open fun visitEnumMapping(o: DTOEnumMapping) {
        visitElement(o)
    }

    open fun visitEnumMappingConstant(o: DTOEnumMappingConstant) {
        visitElement(o)
    }
}