---
title: "union_relations"
sidebar_label: "union_relations"
id: "union_relations"
description: "This macro combines via `union all` an array of [Relations](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation), even when columns have differing orders in each Relation, an..."
category: "SQL generators"
---

This macro combines via `union all` an array of [Relations](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation),
even when columns have differing orders in each Relation, and/or some columns are
missing from some relations. Any columns exclusive to a subset of these
relations will be filled with `null` where not present. A new column
(`_dbt_source_relation`) is also added to indicate the source for each record.

**Usage:**

```sql
{{ dbt_utils.union_relations(
    relations=[ref('my_model'), source('my_source', 'my_table')],
    exclude=["_loaded_at"]
) }}
```

__Args__:

 * `relations` (required): An array of [Relations](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation).
 * `exclude` (optional): A list of column names that should be excluded from
the final query.
 * `include` (optional): A list of column names that should be included in the
final query. Note the `include` and `exclude` arguments are mutually exclusive.
 * `column_override` (optional): A dictionary of explicit column type overrides,
e.g. `{"some_field": "varchar(100)"}`.``
 * `source_column_name` (optional, `default="_dbt_source_relation"`): The name of
the column that records the source of this row. Pass `None` to omit this column from the results.
 * `where` (optional): Filter conditions to include in the `where` clause.
