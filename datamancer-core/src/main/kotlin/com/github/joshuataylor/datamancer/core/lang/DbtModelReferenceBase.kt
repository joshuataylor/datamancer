package com.github.joshuataylor.datamancer.core.lang

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.util.concurrency.annotations.RequiresReadLock

/**
 * Abstract base class for dbt model references.
 *
 * Provides caching support via ResolveCache to avoid redundant resolution operations.
 * Subclasses must implement [resolveInner] to provide actual resolution logic.
 *
 * @param T The type of PSI element this reference is attached to
 */
abstract class DbtModelReferenceBase<T : PsiElement>(
    element: T,
    textRange: TextRange,
    protected val modelName: String
) : PsiReferenceBase<T>(element, textRange) {

    /**
     * Resolves the reference using cached resolution.
     *
     * This method delegates to [resolveInner] and caches the result using ResolveCache
     * for improved performance.
     *
     * @return The resolved PSI element, or null if resolution fails
     */
    override fun resolve(): PsiElement? {
        val resolver = Resolver()
        val results = ResolveCache.getInstance(element.project)
            .resolveWithCaching(this, resolver, false, false)

        return if (results != null && results.isNotEmpty()) results[0].element else null
    }

    /**
     * Indicates that this is a soft reference.
     *
     * Soft references don't show errors when they cannot be resolved, which is appropriate
     * for dbt models that might be in other projects or not yet created.
     *
     * @return true to indicate this is a soft reference
     */
    override fun isSoft(): Boolean = true

    /**
     * Abstract method to be implemented by subclasses for actual reference resolution.
     *
     * This method is called by the resolver and should return an array of resolve results.
     * For simple cases, return a single-element array. For multi-resolve scenarios,
     * return multiple results.
     *
     * @param incompleteCode Whether to consider incomplete code during resolution
     * @return Array of resolve results (may be empty if not resolved)
     */
    @RequiresReadLock
    protected abstract fun resolveInner(incompleteCode: Boolean): Array<ResolveResult>

    /**
     * Equality check based on element and model name.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DbtModelReferenceBase<*>) return false

        return element == other.element && modelName == other.modelName
    }

    /**
     * Hash code based on element and model name.
     */
    override fun hashCode(): Int {
        var result = element.hashCode()
        result = 31 * result + modelName.hashCode()
        return result
    }

    /**
     * Resolver implementation for ResolveCache.
     *
     * Delegates to [resolveInner] for actual resolution logic.
     */
    private inner class Resolver :
        ResolveCache.AbstractResolver<DbtModelReferenceBase<T>, Array<ResolveResult>> {
        override fun resolve(
            ref: DbtModelReferenceBase<T>,
            incompleteCode: Boolean
        ): Array<ResolveResult> {
            return ref.resolveInner(incompleteCode)
        }
    }
}
