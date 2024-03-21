package com.hsc.compiler.driver

import com.hsc.compiler.codegen.passes.*

fun passesForMode(mode: Mode): List<AstPass> {
    return when (mode) {
        Mode.Strict -> TODO()
        Mode.Normal -> listOf(
            RedeclarationCheckPass,
            OwnedRecursiveCallCheckPass,
            ReturnAssignPass,
            InlineFunctionPass,
            InlineFunctionParametersPass,
            RemoveParenPass,
            EvaluateConstantEquationsPass,
            FlattenComplexExpressionsPass,
            InlineFunctionCallAssignmentPass,
            FlattenTempReassignPass,
            EmptyBlockCheckPass,
            // CleanupTempVarsPass,
        )
        Mode.Optimized -> TODO()
    }
}