---
title: "deduplicate"
sidebar_label: "deduplicate"
id: "deduplicate"
description: "This macro returns the sql required to remove duplicate rows from a model, source, or CTE."
category: "SQL generators"
---

This macro returns the sql required to remove duplicate rows from a model, source, or CTE.

__Args__:

 * `relation` (required): a [Relation](https://docs.getdbt.com/reference/dbt-classes#relation) (a `ref` or `source`) or string which identifies the model to deduplicate.
 * `partition_by` (required): column names (or expressions) to use to identify a set/window of rows out of which to select one as the deduplicated row.
 * `order_by` (required): column names (or expressions) that determine the priority order of which row should be chosen if there are duplicates (comma-separated string). *NB.* if this order by clause results in ties then which row is returned may be nondeterministic across runs.

**Usage:**

```sql
{{ dbt_utils.deduplicate(
    relation=source('my_source', 'my_table'),
    partition_by='user_id, cast(timestamp as day)',
    order_by="timestamp desc",
   )
}}
```

```sql
{{ dbt_utils.deduplicate(
    relation=ref('my_model'),
    partition_by='user_id',
    order_by='effective_date desc, effective_sequence desc',
   )
}}
```

```sql
with my_cte as (
    select *
    from {{ source('my_source', 'my_table') }}
    where user_id = 1
),
deduplicated_cte as (
  {{ dbt_utils.deduplicate(
      relation='my_cte',
      partition_by='user_id, cast(timestamp as date)',
      order_by='timestamp desc',
     )
  }}
)
select * from deduplicated_cte
```
