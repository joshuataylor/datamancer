package com.github.joshuataylor.datamancer.dbtcore.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DbtManifest(
    val metadata: DbtManifestMetadata = DbtManifestMetadata(),
    val nodes: Map<String, DbtManifestNode> = emptyMap(),
    val sources: Map<String, DbtManifestSource> = emptyMap(),
    @SerialName("parent_map") val parentMap: Map<String, List<String>> = emptyMap(),
    @SerialName("child_map") val childMap: Map<String, List<String>> = emptyMap()
)

@Serializable
data class DbtManifestMetadata(
    @SerialName("dbt_version") val dbtVersion: String? = null,
    @SerialName("adapter_type") val adapterType: String? = null,
    @SerialName("project_name") val projectName: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null
)

@Serializable
data class DbtManifestNode(
    @SerialName("unique_id") val uniqueId: String = "",
    val name: String = "",
    @SerialName("resource_type") val resourceType: String = "",
    // "schema" conflicts with Kotlin keyword, aliased
    @SerialName("schema") val schema: String? = null,
    val database: String? = null,
    val alias: String? = null,
    @SerialName("relation_name") val relationName: String? = null,
    val description: String? = null,
    // v10+ field names; older manifests used raw_sql/compiled_sql (silently absent)
    @SerialName("raw_code") val rawCode: String? = null,
    @SerialName("compiled_code") val compiledCode: String? = null,
    val compiled: Boolean? = null,
    @SerialName("depends_on") val dependsOn: DbtDependsOn? = null,
    val columns: Map<String, DbtManifestColumn> = emptyMap(),
    val config: DbtNodeConfig? = null,
    val tags: List<String> = emptyList(),
    val meta: Map<String, JsonElement> = emptyMap(),
    @SerialName("patch_path") val patchPath: String? = null,
    @SerialName("original_file_path") val originalFilePath: String? = null,
    val fqn: List<String> = emptyList()
)

@Serializable
data class DbtManifestColumn(
    val name: String = "",
    val description: String? = null,
    @SerialName("data_type") val dataType: String? = null,
    val meta: Map<String, JsonElement> = emptyMap(),
    val tags: List<String> = emptyList(),
    val constraints: List<DbtColumnConstraint> = emptyList()
)

@Serializable
data class DbtManifestSource(
    @SerialName("unique_id") val uniqueId: String = "",
    val name: String = "",
    @SerialName("source_name") val sourceName: String = "",
    @SerialName("schema") val schema: String? = null,
    val database: String? = null,
    val description: String? = null,
    @SerialName("source_description") val sourceDescription: String? = null,
    val columns: Map<String, DbtManifestColumn> = emptyMap(),
    val meta: Map<String, JsonElement> = emptyMap(),
    val tags: List<String> = emptyList(),
    @SerialName("original_file_path") val originalFilePath: String? = null
)

@Serializable
data class DbtDependsOn(
    val nodes: List<String> = emptyList(),
    val macros: List<String> = emptyList()
)

@Serializable
data class DbtNodeConfig(
    val materialized: String? = null,
    val tags: List<String> = emptyList(),
    val meta: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class DbtColumnConstraint(
    val type: String = "",
    val name: String? = null,
    val columns: List<String> = emptyList()
)
