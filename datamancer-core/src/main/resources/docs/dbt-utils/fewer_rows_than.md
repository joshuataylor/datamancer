---
title: "fewer_rows_than"
sidebar_label: "fewer_rows_than"
id: "fewer_rows_than"
description: "Asserts that the respective model has fewer rows than the model being compared."
category: "Generic Tests"
---

Asserts that the respective model has fewer rows than the model being compared.

Usage:

```yaml
version: 2

models:
  - name: model_name
    tests:
      - dbt_utils.fewer_rows_than:
          arguments:
            compare_model: ref('other_table_name')
```

This test supports the `group_by_columns` parameter; see [Grouping in tests](#grouping-in-tests) for details.
