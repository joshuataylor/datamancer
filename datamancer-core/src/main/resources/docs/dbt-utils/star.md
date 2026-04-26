---
title: "star"
sidebar_label: "star"
id: "star"
description: "This macro generates a comma-separated list of all fields that exist in the `from` relation, excluding any fields listed in the `except` argument. The construction is identical to `select * from {{..."
category: "SQL generators"
---

This macro generates a comma-separated list of all fields that exist in the `from` relation, excluding any fields
listed in the `except` argument. The construction is identical to `select * from {{ref('my_model')}}`, replacing star (`*`) with
the star macro.
This macro also has an optional `relation_alias` argument that will prefix all generated fields with an alias (`relation_alias`.`field_name`).
The macro also has optional `prefix` and `suffix` arguments. When one or both are provided, they will be concatenated onto each field's alias
in the output (`prefix` ~ `field_name` ~ `suffix`). NB: This prevents the output from being used in any context other than a select statement.
This macro also has an optional `quote_identifiers` argument that will encase the selected columns and their aliases in double quotes.

__Args__:

 * `from` (required): a [Relation](https://docs.getdbt.com/reference/dbt-classes#relation) (a `ref` or `source`) that contains the list of columns you wish to select from
 * `except` (optional, default=`[]`): The name of the columns you wish to exclude. (case-insensitive)
 * `relation_alias` (optional, default=`''`): will prefix all generated fields with an alias (`relation_alias`.`field_name`).
 * `prefix` (optional, default=`''`): will prefix the output `field_name` (`field_name as prefix_field_name`).
 * `suffix` (optional, default=`''`): will suffix the output `field_name` (`field_name as field_name_suffix`).
 * `quote_identifiers` (optional, default=`True`): will encase selected columns and aliases in double quotes (`"field_name" as "field_name"`).

**Usage:**

```sql
select
  {{ dbt_utils.star(ref('my_model')) }}
from {{ ref('my_model') }}

```

```sql
select
  {{ dbt_utils.star(from=ref('my_model'), quote_identifiers=False) }}
from {{ ref('my_model') }}

```

```sql
select
{{ dbt_utils.star(from=ref('my_model'), except=["exclude_field_1", "exclude_field_2"]) }}
from {{ ref('my_model') }}

```

```sql
select
{{ dbt_utils.star(from=ref('my_model'), except=["exclude_field_1", "exclude_field_2"], prefix="max_") }}
from {{ ref('my_model') }}

```
