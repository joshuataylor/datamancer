---
title: "equality"
sidebar_label: "equality"
id: "equality"
description: "Asserts the equality of two relations. Optionally specify a subset of columns to compare or exclude, and a precision to compare numeric columns on."
category: "Generic Tests"
---

Asserts the equality of two relations. Optionally specify a subset of columns to compare or exclude, and a precision to compare numeric columns on.

**Usage:**

```yaml
version: 2

models:
  # compare the entire table 
  - name: model_name
    tests:
      - dbt_utils.equality:
          arguments:
            compare_model: ref('other_table_name')

  # only compare some of the columns
  - name: model_name_compare_columns
    tests:
      - dbt_utils.equality:
          arguments:
            compare_model: ref('other_table_name')
            compare_columns:
              - first_column
              - second_column
            precision: 4

  # compare all columns except the ones on the ignore list
  - name: model_name_exclude_columns
    tests:
      - dbt_utils.equality:
          arguments:
            compare_model: ref('other_table_name')
            exclude_columns:
              - third_column
```
