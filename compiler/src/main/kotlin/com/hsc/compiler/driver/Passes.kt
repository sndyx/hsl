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
            RemoveParenPass,
            EvaluateConstantEquationsPass,
            FlattenComplexExpressionsPass,
            MapCallActionsPass,
            InlineFunctionParametersPass,
            InlineFunctionCallAssignmentPass,
            FlattenTempReassignPass,
            EmptyBlockCheckPass,
            // CleanupTempVarsPass,
            LimitCheckPass,
            )
        Mode.Optimized -> TODO()
    }
}