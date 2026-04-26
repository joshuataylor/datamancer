package com.github.joshuataylor.datamancer.core.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for DbtConfigCompletionContributor.
 * Tests the config key definitions and helper methods.
 */
class DbtConfigCompletionContributorTest : BasePlatformTestCase() {

    // Config keys data tests
    fun testConfigKeysAreNotEmpty() {
        assertTrue(
            "Config keys should not be empty",
            DbtConfigCompletionContributor.CONFIG_KEYS.isNotEmpty()
        )
    }

    fun testConfigKeyNamesSetMatchesKeys() {
        val keyNames = DbtConfigCompletionContributor.CONFIG_KEYS.map { it.name }.toSet()
        assertEquals(
            "CONFIG_KEY_NAMES should match keys in CONFIG_KEYS",
            keyNames,
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES
        )
    }

    // Essential config keys tests
    fun testContainsMaterializedConfig() {
        assertTrue(
            "Should contain 'materialized' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("materialized")
        )
    }

    fun testContainsSchemaConfig() {
        assertTrue(
            "Should contain 'schema' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("schema")
        )
    }

    fun testContainsDatabaseConfig() {
        assertTrue(
            "Should contain 'database' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("database")
        )
    }

    fun testContainsTagsConfig() {
        assertTrue(
            "Should contain 'tags' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("tags")
        )
    }

    fun testContainsEnabledConfig() {
        assertTrue(
            "Should contain 'enabled' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("enabled")
        )
    }

    fun testContainsAliasConfig() {
        assertTrue(
            "Should contain 'alias' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("alias")
        )
    }

    // Incremental configs tests
    fun testContainsUniqueKeyConfig() {
        assertTrue(
            "Should contain 'unique_key' config for incremental models",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("unique_key")
        )
    }

    fun testContainsIncrementalStrategyConfig() {
        assertTrue(
            "Should contain 'incremental_strategy' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("incremental_strategy")
        )
    }

    fun testContainsOnSchemaChangeConfig() {
        assertTrue(
            "Should contain 'on_schema_change' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("on_schema_change")
        )
    }

    fun testContainsFullRefreshConfig() {
        assertTrue(
            "Should contain 'full_refresh' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("full_refresh")
        )
    }

    // Hook configs tests
    fun testContainsPreHookConfig() {
        assertTrue(
            "Should contain 'pre_hook' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("pre_hook")
        )
    }

    fun testContainsPostHookConfig() {
        assertTrue(
            "Should contain 'post_hook' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("post_hook")
        )
    }

    // Documentation configs tests
    fun testContainsDocsConfig() {
        assertTrue(
            "Should contain 'docs' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("docs")
        )
    }

    fun testContainsPersistDocsConfig() {
        assertTrue(
            "Should contain 'persist_docs' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("persist_docs")
        )
    }

    // Access control configs tests
    fun testContainsGrantsConfig() {
        assertTrue(
            "Should contain 'grants' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("grants")
        )
    }

    fun testContainsAccessConfig() {
        assertTrue(
            "Should contain 'access' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("access")
        )
    }

    fun testContainsGroupConfig() {
        assertTrue(
            "Should contain 'group' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("group")
        )
    }

    // Contract configs tests
    fun testContainsContractConfig() {
        assertTrue(
            "Should contain 'contract' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("contract")
        )
    }

    // Meta configs tests
    fun testContainsMetaConfig() {
        assertTrue(
            "Should contain 'meta' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("meta")
        )
    }

    // Partitioning configs tests
    fun testContainsPartitionByConfig() {
        assertTrue(
            "Should contain 'partition_by' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("partition_by")
        )
    }

    fun testContainsClusterByConfig() {
        assertTrue(
            "Should contain 'cluster_by' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("cluster_by")
        )
    }

    // BigQuery-specific configs tests
    fun testContainsHoursToExpirationConfig() {
        assertTrue(
            "Should contain 'hours_to_expiration' config for BigQuery",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("hours_to_expiration")
        )
    }

    fun testContainsLabelsConfig() {
        assertTrue(
            "Should contain 'labels' config for BigQuery",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("labels")
        )
    }

    // Snowflake-specific configs tests
    fun testContainsSnowflakeWarehouseConfig() {
        assertTrue(
            "Should contain 'snowflake_warehouse' config",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("snowflake_warehouse")
        )
    }

    fun testContainsCopyGrantsConfig() {
        assertTrue(
            "Should contain 'copy_grants' config for Snowflake",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("copy_grants")
        )
    }

    // Databricks/Spark-specific configs tests
    fun testContainsFileFormatConfig() {
        assertTrue(
            "Should contain 'file_format' config for Databricks/Spark",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("file_format")
        )
    }

    fun testContainsTblpropertiesConfig() {
        assertTrue(
            "Should contain 'tblproperties' config for Databricks/Spark",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("tblproperties")
        )
    }

    // Materialised view configs tests
    fun testContainsOnConfigurationChangeConfig() {
        assertTrue(
            "Should contain 'on_configuration_change' config for MVs",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("on_configuration_change")
        )
    }

    fun testContainsAutoRefreshConfig() {
        assertTrue(
            "Should contain 'auto_refresh' config for MVs",
            DbtConfigCompletionContributor.CONFIG_KEY_NAMES.contains("auto_refresh")
        )
    }

    // ConfigKey data class tests
    fun testConfigKeyHasRequiredFields() {
        val configKey = DbtConfigCompletionContributor.CONFIG_KEYS.first()
        assertNotNull("ConfigKey should have name", configKey.name)
        assertNotNull("ConfigKey should have description", configKey.description)
        assertNotNull("ConfigKey should have valueType", configKey.valueType)
        assertNotNull("ConfigKey should have exampleValue", configKey.exampleValue)
    }

    fun testMaterializedConfigHasAllowedValues() {
        val materializedConfig = DbtConfigCompletionContributor.CONFIG_KEYS
            .find { it.name == "materialized" }

        assertNotNull("materialized config should exist", materializedConfig)
        assertNotNull("materialized should have allowedValues", materializedConfig?.allowedValues)
        assertTrue(
            "materialized allowedValues should contain 'table'",
            materializedConfig?.allowedValues?.contains("table") == true
        )
        assertTrue(
            "materialized allowedValues should contain 'view'",
            materializedConfig?.allowedValues?.contains("view") == true
        )
        assertTrue(
            "materialized allowedValues should contain 'incremental'",
            materializedConfig?.allowedValues?.contains("incremental") == true
        )
    }

    fun testOnSchemaChangeConfigHasAllowedValues() {
        val config = DbtConfigCompletionContributor.CONFIG_KEYS
            .find { it.name == "on_schema_change" }

        assertNotNull("on_schema_change config should exist", config)
        assertNotNull("on_schema_change should have allowedValues", config?.allowedValues)
        assertTrue(
            "on_schema_change allowedValues should contain 'fail'",
            config?.allowedValues?.contains("fail") == true
        )
        assertTrue(
            "on_schema_change allowedValues should contain 'sync_all_columns'",
            config?.allowedValues?.contains("sync_all_columns") == true
        )
    }

    // Key uniqueness tests
    fun testAllConfigKeyNamesAreUnique() {
        val keyNames = DbtConfigCompletionContributor.CONFIG_KEYS.map { it.name }
        assertEquals(
            "All config key names should be unique",
            keyNames.size,
            keyNames.distinct().size
        )
    }

    // Key format tests
    fun testAllConfigKeyNamesAreValidIdentifiers() {
        DbtConfigCompletionContributor.CONFIG_KEYS.forEach { key ->
            assertTrue(
                "Config key '${key.name}' should be a valid identifier",
                key.name.matches(Regex("[a-z_][a-z0-9_]*"))
            )
        }
    }

    fun testAllConfigKeyNamesAreLowercase() {
        DbtConfigCompletionContributor.CONFIG_KEYS.forEach { key ->
            assertEquals(
                "Config key '${key.name}' should be lowercase",
                key.name.lowercase(),
                key.name
            )
        }
    }

    // Description tests
    fun testAllConfigKeysHaveNonEmptyDescriptions() {
        DbtConfigCompletionContributor.CONFIG_KEYS.forEach { key ->
            assertTrue(
                "Config key '${key.name}' should have a non-empty description",
                key.description.isNotBlank()
            )
        }
    }

    // Example value tests
    fun testAllConfigKeysHaveNonEmptyExampleValues() {
        DbtConfigCompletionContributor.CONFIG_KEYS.forEach { key ->
            assertTrue(
                "Config key '${key.name}' should have a non-empty example value",
                key.exampleValue.isNotBlank()
            )
        }
    }

    // Count test
    fun testConfigKeyCount() {
        // We expect at least 40 config keys based on our definition
        assertTrue(
            "Should have at least 40 config keys defined",
            DbtConfigCompletionContributor.CONFIG_KEYS.size >= 40
        )
    }

    // Contributor instantiation test
    fun testContributorCanBeInstantiated() {
        val contributor = DbtConfigCompletionContributor()
        assertNotNull("Contributor should be instantiated", contributor)
    }
}
