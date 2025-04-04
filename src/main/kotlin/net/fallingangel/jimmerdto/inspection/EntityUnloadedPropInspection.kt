package net.fallingangel.jimmerdto.inspection

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import net.fallingangel.jimmerdto.util.extract
import net.fallingangel.jimmerdto.util.hasAnnotation
import net.fallingangel.jimmerdto.util.inspection
import org.babyfish.jimmer.sql.Entity

class EntityUnloadedPropInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                if (expression.firstChild !is PsiReferenceExpression) {
                    return
                }

                if (expression.parent !is PsiMethodCallExpression) {
                    return
                }

                // user
                val parameter = (expression.firstChild as PsiReferenceExpression).resolve() as? PsiParameter ?: return
                val type = parameter.type.extract as? PsiClassType ?: return
                val clazz = type.resolve() ?: return
                if (!clazz.hasAnnotation(Entity::class)) {
                    return
                }
                // 抓取的属性列表
                val fetchedProps = mutableListOf<String>()

                val foreach = parameter.declarationScope as? PsiForeachStatement ?: return
                /*
                 * List<User> users = client.createQuery(userTable)
                 *     .select(userTable.fetch(userFetcher.firstName().lastName()))
                 *     .execute();
                 */
                val declaration = foreach.iteratedValue?.reference?.resolve() as? PsiLocalVariable ?: return
                /*
                 * client.createQuery(userTable)
                 *     .select(userTable.fetch(userFetcher.firstName().lastName()))
                 *     .execute();
                 */
                val executeCall = declaration.initializer as? PsiMethodCallExpression ?: return
                val execute = executeCall.resolveMethod() ?: return

                if (execute.name == "execute" && execute.containingClass?.qualifiedName == "org.babyfish.jimmer.sql.ast.Executable") {
                    /*
                     * client.createQuery(userTable)
                     *     .select(userTable.fetch(userFetcher.firstName().lastName()))
                     */
                    val selectCall = executeCall.methodExpression.qualifierExpression as? PsiMethodCallExpression ?: return
                    val select = selectCall.resolveMethod() ?: return

                    if (select.name == "select" && select.containingClass?.qualifiedName == "org.babyfish.jimmer.sql.ast.query.selectable.RootSelectable") {
                        // userTable.fetch(userFetcher.firstName().lastName())
                        val selectParameter = selectCall.argumentList.expressions[0] as? PsiMethodCallExpression ?: return
                        val fetchMethod = selectParameter.resolveMethod() ?: return
                        if (fetchMethod.name == "fetch" && fetchMethod.containingClass?.qualifiedName == "org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable") {
                            // userFetcher.firstName().lastName()
                            val fetchExpression = selectParameter.argumentList.expressions[0] as? PsiMethodCallExpression ?: return
                            fetchExpression.resolve(fetchedProps)
                            println(fetchedProps)
                        }
                    }
                }

                if (expression.referenceName !in fetchedProps) {
                    holder.registerProblem(
                        expression,
                        inspection("inspection.entity.unloaded.usage"),
                    )
                }

                super.visitReferenceExpression(expression)
            }
        }
    }

    private fun PsiMethodCallExpression.resolve(path: MutableList<String>) {
        val propName = methodExpression.referenceName!!
        path.add(propName)
        val chainedExpression = methodExpression.qualifierExpression
        if (chainedExpression !is PsiMethodCallExpression) {
            return
        }
        chainedExpression.resolve(path)
    }
}