---
title: "pivot"
sidebar_label: "pivot"
id: "pivot"
description: "This macro pivots values from rows to columns."
category: "SQL generators"
---

This macro pivots values from rows to columns.

**Usage:**

```sql
{{ dbt_utils.pivot(<column>, <list of values>) }}
```

**Examples:**

    Input: orders

    | size | color |
    |------|-------|
    | S    | red   |
    | S    | blue  |
    | S    | red   |
    | M    | red   |

    select
      size,
      {{ dbt_utils.pivot(
          'color',
          dbt_utils.get_column_values(ref('orders'), 'color')
      ) }}
    from {{ ref('orders') }}
    group by size

    Output:

    | size | red | blue |
    |------|-----|------|
    | S    | 2   | 1    |
    | M    | 1   | 0    |

    Input: orders

    | size | color | quantity |
    |------|-------|----------|
    | S    | red   | 1        |
    | S    | blue  | 2        |
    | S    | red   | 4        |
    | M    | red   | 8        |

    select
      size,
      {{ dbt_utils.pivot(
          'color',
          dbt_utils.get_column_values(ref('orders'), 'color'),
          agg='sum',
          then_value='quantity',
          prefix='pre_',
          suffix='_post'
      ) }}
    from {{ ref('orders') }}
    group by size

    Output:

    | size | pre_red_post | pre_blue_post |
    |------|--------------|---------------|
    | S    | 5            | 2             |
    | M    | 8            | 0             |


__Args__:

 * `column`: Column name, required
 * `values`: List of row values to turn into columns, required
 * `alias`: Whether to create column aliases, default is True
 * `agg`: SQL aggregation function, default is sum
 * `cmp`: SQL value comparison, default is =
 * `prefix`: Column alias prefix, default is blank
 * `suffix`: Column alias postfix, default is blank
 * `then_value`: Value to use if comparison succeeds, default is 1
 * `else_value`: Value to use if comparison fails, default is 0
 * `quote_identifiers`: Whether to surround column aliases with double quotes, default is true
