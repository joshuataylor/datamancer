---
title: "not_empty_string"
sidebar_label: "not_empty_string"
id: "not_empty_string"
description: "Asserts that a column does not have any values equal to `''`."
category: "Generic Tests"
---

Asserts that a column does not have any values equal to `''`. 

**Usage:**
```yaml
version: 2

models:
  - name: model_name
    columns:
      - name: column_name
        tests:
          - dbt_utils.not_empty_string
```

The macro accepts an optional argument `trim_whitespace` that controls whether whitespace should be trimmed from the column when evaluating. The default is `true`. 

**Usage:**
```yaml
version: 2

models:
  - name: model_name
    columns:
      - name: column_name
        tests:
          - dbt_utils.not_empty_string:
              arguments:
                trim_whitespace: false
              
```
