---
title: "unpivot"
sidebar_label: "unpivot"
id: "unpivot"
description: "This macro \"un-pivots\" a table from wide format to long format. Functionality is similar to pandas [melt](http://pandas.pydata.org/pandas-docs/stable/generated/pandas.melt.html) function. Boolean v..."
category: "SQL generators"
---

This macro "un-pivots" a table from wide format to long format. Functionality is similar to pandas [melt](http://pandas.pydata.org/pandas-docs/stable/generated/pandas.melt.html) function.
Boolean values are replaced with the strings 'true'|'false'

**Usage:**

```sql
{{ dbt_utils.unpivot(
  relation=ref('table_name'),
  cast_to='datatype',
  exclude=[<list of columns to exclude from unpivot>],
  remove=[<list of columns to remove>],
  field_name=<column name for field>,
  value_name=<column name for value>
) }}
```

**Usage:**

    Input: orders

    | date       | size | color | status     |
    |------------|------|-------|------------|
    | 2017-01-01 | S    | red   | complete   |
    | 2017-03-01 | S    | red   | processing |

    {{ dbt_utils.unpivot(ref('orders'), cast_to='varchar', exclude=['date','status']) }}

    Output:

    | date       | status     | field_name | value |
    |------------|------------|------------|-------|
    | 2017-01-01 | complete   | size       | S     |
    | 2017-01-01 | complete   | color      | red   |
    | 2017-03-01 | processing | size       | S     |
    | 2017-03-01 | processing | color      | red   |

__Args__:

 * `relation`: The [Relation](https://docs.getdbt.com/docs/writing-code-in-dbt/class-reference/#relation) to unpivot.
 * `cast_to`: The data type to cast the unpivoted values to, default is varchar
 * `exclude`: A list of columns to exclude from the unpivot operation but keep in the resulting table.
 * `remove`: A list of columns to remove from the resulting table.
 * `field_name`: column name in the resulting table for field
 * `value_name`: column name in the resulting table for value
 * `quote_identifiers` (optional, default=`False`): will encase selected columns and aliases in quotes according to your adapter's implementation of `adapter.quote` (e.g. `"field_name" as "field_name"`).
