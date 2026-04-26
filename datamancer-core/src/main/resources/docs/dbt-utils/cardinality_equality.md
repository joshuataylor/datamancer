---
title: "cardinality_equality"
sidebar_label: "cardinality_equality"
id: "cardinality_equality"
description: "Asserts that values in a given column have exactly the same cardinality as values from a different column in a different model."
category: "Generic Tests"
---

Asserts that values in a given column have exactly the same cardinality as values from a different column in a different model.

**Usage:**

```yaml
version: 2

models:
  - name: model_name
    columns:
      - name: from_column
        tests:
          - dbt_utils.cardinality_equality:
              arguments:
                field: other_column_name
                to: ref('other_model_name')
```
