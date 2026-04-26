---
title: "get_relations_by_prefix"
sidebar_label: "get_relations_by_prefix"
id: "get_relations_by_prefix"
description: "> This macro will soon be deprecated in favor of the more flexible `get_relations_by_pattern` macro (above)"
category: "Introspective macros"
---

> This macro will soon be deprecated in favor of the more flexible `get_relations_by_pattern` macro (above)

Returns a list of [Relations](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation)
that match a given prefix, with an optional exclusion pattern. It's particularly
handy paired with `union_relations`.

**Usage:**

```sql
-- Returns a list of relations that match schema.prefix%
{% set relations = dbt_utils.get_relations_by_prefix('my_schema', 'my_prefix') %}

-- Returns a list of relations as above, excluding any that end in `deprecated`
{% set relations = dbt_utils.get_relations_by_prefix('my_schema', 'my_prefix', '%deprecated') %}

-- Example using the union_relations macro
{% set event_relations = dbt_utils.get_relations_by_prefix('events', 'event_') %}
{{ dbt_utils.union_relations(relations = event_relations) }}
```

__Args__:

 * `schema` (required): The schema to inspect for relations.
 * `prefix` (required): The prefix of the table/view (case insensitive)
 * `exclude` (optional): Exclude any relations that match this pattern.
 * `database` (optional, default = `target.database`): The database to inspect
for relations.
