package com.hsc.mason

import kotlinx.serialization.Serializable

@Serializable
class Lock(
    val dependencies: MutableList<CapturedDependency>
)

@Serializable
class CapturedDependency(
    val name: String,
    val url: String,
    val commit: String,
)